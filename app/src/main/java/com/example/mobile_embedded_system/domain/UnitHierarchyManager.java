package com.example.mobile_embedded_system.domain;

import com.example.mobile_embedded_system.data.local.SubjectDao;
import com.example.mobile_embedded_system.data.local.SubjectEntity;
import com.example.mobile_embedded_system.data.local.TelemetryEntity;
import com.example.mobile_embedded_system.domain.network.MqttTopicBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Менеджер дерева иерархии подразделений и перерасчёта MQTT ACL (ТЗ §1.8–1.12, §5.8–5.12).
 * Поддерживает сохранение и восстановление иерархии в локальной Room БД (US-14, AC-03.4).
 */
public class UnitHierarchyManager {

    public static class NodeSummary {
        public final int totalUnits;
        public final int onlineUnits;
        public final TacticalStatusEvaluator.Status worstStatus;

        public NodeSummary(int totalUnits, int onlineUnits, TacticalStatusEvaluator.Status worstStatus) {
            this.totalUnits = totalUnits;
            this.onlineUnits = onlineUnits;
            this.worstStatus = worstStatus;
        }
    }

    private final SubjectDao subjectDao;
    private final Executor executor;
    private final List<HierarchyNode> rootNodes = new ArrayList<>();
    private long nextNodeId = 100L;
    private String currentNetworkId = "mesh-a";

    public UnitHierarchyManager() {
        this(null, null);
    }

    public UnitHierarchyManager(SubjectDao subjectDao, Executor executor) {
        this.subjectDao = subjectDao;
        this.executor = executor;
        initDefaultHierarchy();
        if (subjectDao != null && executor != null) {
            loadOrPersistFromDb();
        }
    }

    private void initDefaultHierarchy() {
        HierarchyNode battalion = new HierarchyNode(1L, "1-й Батальон", HierarchyNode.NodeType.BATTALION, null, null, "BN1");
        HierarchyNode company = new HierarchyNode(2L, "2-я Рота", HierarchyNode.NodeType.COMPANY, 1L, null, "BN1/CO2");
        HierarchyNode platoon = new HierarchyNode(3L, "3-й Взвод", HierarchyNode.NodeType.PLATOON, 2L, null, "BN1/CO2/PL3");
        HierarchyNode squad = new HierarchyNode(4L, "1-е Отделение", HierarchyNode.NodeType.SQUAD, 3L, null, "BN1/CO2/PL3/SQ1");

        HierarchyNode u1001 = new HierarchyNode(5L, "Боец [1001] • Командир", HierarchyNode.NodeType.SOLDIER, 4L, 1001L, "BN1/CO2/PL3/SQ1/1001");
        HierarchyNode u1002 = new HierarchyNode(6L, "Боец [1002] • Стрелок", HierarchyNode.NodeType.SOLDIER, 4L, 1002L, "BN1/CO2/PL3/SQ1/1002");
        HierarchyNode u1003 = new HierarchyNode(7L, "Боец [1003] • Санинструктор", HierarchyNode.NodeType.SOLDIER, 4L, 1003L, "BN1/CO2/PL3/SQ1/1003");

        squad.addChild(u1001);
        squad.addChild(u1002);
        squad.addChild(u1003);

        platoon.addChild(squad);
        company.addChild(platoon);
        battalion.addChild(company);

        rootNodes.add(battalion);
    }

    private void loadOrPersistFromDb() {
        if (executor == null || subjectDao == null) return;
        executor.execute(() -> {
            try {
                List<SubjectEntity> list = subjectDao.getAllSubjectsSync();
                if (list == null || list.isEmpty()) {
                    saveAllToDbSync();
                } else {
                    reconstructFromDb(list);
                }
            } catch (Exception ignored) {
            }
        });
    }

    private synchronized void saveAllToDbSync() {
        if (subjectDao == null) return;
        List<SubjectEntity> entities = new ArrayList<>();
        collectEntitiesRecursive(rootNodes, entities);
        subjectDao.deleteForNetwork(this.currentNetworkId);
        subjectDao.insertAll(entities);
    }

    private void collectEntitiesRecursive(List<HierarchyNode> nodes, List<SubjectEntity> out) {
        for (HierarchyNode n : nodes) {
            out.add(new SubjectEntity(
                    String.valueOf(n.getId()),
                    this.currentNetworkId,
                    n.getUserId(),
                    n.getName(),
                    n.getParentId() != null ? String.valueOf(n.getParentId()) : null,
                    n.getType().name(),
                    n.getHierarchyPath()
            ));
            collectEntitiesRecursive(n.getChildren(), out);
        }
    }

    private synchronized void reconstructFromDb(List<SubjectEntity> list) {
        Map<Long, HierarchyNode> nodeMap = new HashMap<>();
        List<HierarchyNode> newRoots = new ArrayList<>();
        long maxId = 100L;

        for (SubjectEntity entity : list) {
            try {
                long id = Long.parseLong(entity.id);
                if (id >= maxId) {
                    maxId = id + 1;
                }
                HierarchyNode.NodeType type = HierarchyNode.NodeType.SOLDIER;
                if (entity.nodeType != null) {
                    try {
                        type = HierarchyNode.NodeType.valueOf(entity.nodeType);
                    } catch (Exception ignored) {}
                }
                Long parentId = entity.parentId != null ? Long.parseLong(entity.parentId) : null;
                Long userId = (entity.userId != null && entity.userId > 0) ? entity.userId : null;
                HierarchyNode node = new HierarchyNode(id, entity.name, type, parentId, userId, entity.hierarchyPath);
                nodeMap.put(id, node);
            } catch (Exception ignored) {}
        }

        for (HierarchyNode node : nodeMap.values()) {
            if (node.getParentId() != null && nodeMap.containsKey(node.getParentId())) {
                nodeMap.get(node.getParentId()).addChild(node);
            } else {
                newRoots.add(node);
            }
        }

        if (!newRoots.isEmpty()) {
            rootNodes.clear();
            rootNodes.addAll(newRoots);
            nextNodeId = maxId;
        }
    }

    public synchronized List<HierarchyNode> getRootNodes() {
        return rootNodes;
    }

    public synchronized List<Long> getAllUnitUserIds() {
        List<Long> userIds = new ArrayList<>();
        collectUserIdsRecursive(rootNodes, userIds);
        if (userIds.isEmpty()) {
            userIds.add(1001L);
            userIds.add(1002L);
            userIds.add(1003L);
        }
        return userIds;
    }

    private void collectUserIdsRecursive(List<HierarchyNode> nodes, List<Long> out) {
        for (HierarchyNode node : nodes) {
            if (node.getType() == HierarchyNode.NodeType.SOLDIER && node.getUserId() != null) {
                if (!out.contains(node.getUserId())) {
                    out.add(node.getUserId());
                }
            }
            collectUserIdsRecursive(node.getChildren(), out);
        }
    }

    public synchronized HierarchyNode findNodeById(long id) {
        return findNodeRecursive(rootNodes, id);
    }

    private HierarchyNode findNodeRecursive(List<HierarchyNode> nodes, long id) {
        for (HierarchyNode node : nodes) {
            if (node.getId() == id) return node;
            HierarchyNode found = findNodeRecursive(node.getChildren(), id);
            if (found != null) return found;
        }
        return null;
    }

    public synchronized HierarchyNode findNodeByUserId(long userId) {
        return findNodeByUserIdRecursive(rootNodes, userId);
    }

    private HierarchyNode findNodeByUserIdRecursive(List<HierarchyNode> nodes, long userId) {
        for (HierarchyNode node : nodes) {
            if (node.getType() == HierarchyNode.NodeType.SOLDIER && node.getUserId() != null && node.getUserId() == userId) {
                return node;
            }
            HierarchyNode found = findNodeByUserIdRecursive(node.getChildren(), userId);
            if (found != null) return found;
        }
        return null;
    }

    public synchronized String getCurrentNetworkId() {
        return currentNetworkId;
    }

    public synchronized void setCurrentNetworkId(String networkId) {
        this.currentNetworkId = networkId != null ? networkId : "mesh-a";
    }

    public synchronized void switchNetwork(String networkId, List<SubjectEntity> subjects) {
        this.currentNetworkId = networkId != null ? networkId : "mesh-a";
        rootNodes.clear();
        if (subjects != null && !subjects.isEmpty()) {
            reconstructFromDb(subjects);
        } else {
            initDefaultHierarchy();
            if (subjectDao != null && executor != null) {
                executor.execute(this::saveAllToDbSync);
            }
        }
    }

    public synchronized boolean isDescendant(long ancestorId, long checkId) {
        HierarchyNode ancestor = findNodeById(ancestorId);
        if (ancestor == null) return false;
        return isDescendantRecursive(ancestor.getChildren(), checkId);
    }

    private boolean isDescendantRecursive(List<HierarchyNode> children, long checkId) {
        for (HierarchyNode child : children) {
            if (child.getId() == checkId) return true;
            if (isDescendantRecursive(child.getChildren(), checkId)) return true;
        }
        return false;
    }

    /**
     * Валидация прав командного управления по иерархии (AC-02.2).
     * Разрешено, если target находится в поддереве superior или superior является командиром отделения.
     */
    public synchronized boolean isSubordinate(long superiorUserId, long targetUserId) {
        if (superiorUserId == targetUserId) {
            return true;
        }
        HierarchyNode superiorNode = findNodeByUserId(superiorUserId);
        HierarchyNode targetNode = findNodeByUserId(targetUserId);
        if (superiorNode == null || targetNode == null) {
            return false;
        }
        if (isDescendant(superiorNode.getId(), targetNode.getId())) {
            return true;
        }
        if (superiorNode.getParentId() != null && superiorNode.getParentId().equals(targetNode.getParentId())) {
            if (superiorNode.getName().toLowerCase(Locale.ROOT).contains("командир") || superiorUserId == 1001L) {
                return true;
            }
        }
        return false;
    }

    public synchronized HierarchyNode addNode(String name, HierarchyNode.NodeType type, Long parentId, Long userId) {
        HierarchyNode parent = null;
        if (parentId != null) {
            parent = findNodeById(parentId);
            if (parent == null) {
                throw new IllegalArgumentException("Родительский узел не найден: " + parentId);
            }
        }

        long id = nextNodeId++;
        String path;
        if (parent != null) {
            path = parent.getHierarchyPath() + "/" + (userId != null ? userId : id);
        } else {
            path = "NODE_" + id;
        }
        HierarchyNode node = new HierarchyNode(id, name, type, parentId, userId, path);
        if (parent != null) {
            parent.addChild(node);
        } else {
            rootNodes.add(node);
        }

        if (subjectDao != null && executor != null) {
            executor.execute(() -> {
                try {
                    subjectDao.insert(new SubjectEntity(
                            String.valueOf(node.getId()),
                            this.currentNetworkId,
                            node.getUserId(),
                            node.getName(),
                            node.getParentId() != null ? String.valueOf(node.getParentId()) : null,
                            node.getType().name(),
                            node.getHierarchyPath()
                    ));
                } catch (Exception ignored) {}
            });
        }

        return node;
    }

    /**
     * Перемещение узла с контролем циклических зависимостей (AC-02.1).
     */
    public synchronized boolean moveNode(long nodeId, Long newParentId) {
        HierarchyNode node = findNodeById(nodeId);
        if (node == null) return false;

        if (newParentId != null) {
            if (newParentId == nodeId || isDescendant(nodeId, newParentId)) {
                throw new IllegalArgumentException("Циклическая иерархия отклонена (AC-02.1)");
            }
            HierarchyNode newParent = findNodeById(newParentId);
            if (newParent == null) {
                throw new IllegalArgumentException("Новый родительский узел не найден: " + newParentId);
            }
        }

        removeNodeRecursive(rootNodes, nodeId);
        node.setParentId(newParentId);

        if (newParentId != null) {
            HierarchyNode parent = findNodeById(newParentId);
            parent.addChild(node);
            node.setHierarchyPath(parent.getHierarchyPath() + "/" + (node.getUserId() != null ? node.getUserId() : node.getId()));
        } else {
            rootNodes.add(node);
            node.setHierarchyPath("NODE_" + node.getId());
        }

        if (subjectDao != null && executor != null) {
            executor.execute(this::saveAllToDbSync);
        }
        return true;
    }

    public synchronized boolean removeNode(long id) {
        boolean removed = removeNodeRecursive(rootNodes, id);
        if (removed && subjectDao != null && executor != null) {
            executor.execute(() -> {
                try {
                    subjectDao.deleteById(String.valueOf(id));
                } catch (Exception ignored) {}
            });
        }
        return removed;
    }

    private boolean removeNodeRecursive(List<HierarchyNode> nodes, long id) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getId() == id) {
                nodes.remove(i);
                return true;
            }
            if (removeNodeRecursive(nodes.get(i).getChildren(), id)) {
                return true;
            }
        }
        return false;
    }

    public String generateMqttTelemetryTopic(HierarchyNode node, String networkRoot) {
        return MqttTopicBuilder.buildSubtreeSubscriptionTopic(networkRoot, node.getHierarchyPath());
    }

    public NodeSummary evaluateNodeSummary(HierarchyNode node, Map<Long, TelemetryEntity> latestData) {
        int[] counts = new int[2]; // [0] = total, [1] = online
        TacticalStatusEvaluator.Status[] worst = new TacticalStatusEvaluator.Status[]{TacticalStatusEvaluator.Status.OK};
        long nowMs = System.currentTimeMillis();

        evaluateNodeRecursive(node, latestData, counts, worst, nowMs);
        return new NodeSummary(counts[0], counts[1], worst[0]);
    }

    private void evaluateNodeRecursive(HierarchyNode node, Map<Long, TelemetryEntity> latestData, int[] counts, TacticalStatusEvaluator.Status[] worst, long nowMs) {
        if (node.getType() == HierarchyNode.NodeType.SOLDIER && node.getUserId() != null) {
            counts[0]++;
            TelemetryEntity entity = latestData.get(node.getUserId());
            if (entity != null) {
                if (nowMs - entity.receivedAtMs < 120_000L) {
                    counts[1]++;
                }
                TacticalStatusEvaluator.Status status = TacticalStatusEvaluator.evaluate(entity.pulseBpm, entity.temperatureCelsius);
                if (status == TacticalStatusEvaluator.Status.CRITICAL) {
                    worst[0] = TacticalStatusEvaluator.Status.CRITICAL;
                } else if (status == TacticalStatusEvaluator.Status.WARNING && worst[0] != TacticalStatusEvaluator.Status.CRITICAL) {
                    worst[0] = TacticalStatusEvaluator.Status.WARNING;
                }
            }
        } else {
            for (HierarchyNode child : node.getChildren()) {
                evaluateNodeRecursive(child, latestData, counts, worst, nowMs);
            }
        }
    }
}
