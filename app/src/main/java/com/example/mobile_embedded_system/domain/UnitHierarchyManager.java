package com.example.mobile_embedded_system.domain;

import com.example.mobile_embedded_system.data.local.TelemetryEntity;
import com.example.mobile_embedded_system.domain.network.MqttTopicBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Менеджер дерева иерархии подразделений и перерасчёта MQTT ACL (ТЗ §1.8–1.12, §5.8–5.12).
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

    private final List<HierarchyNode> rootNodes = new ArrayList<>();
    private long nextNodeId = 100L;

    public UnitHierarchyManager() {
        initDefaultHierarchy();
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

    public synchronized List<HierarchyNode> getRootNodes() {
        return rootNodes;
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

    public synchronized HierarchyNode addNode(String name, HierarchyNode.NodeType type, Long parentId, Long userId) {
        long id = nextNodeId++;
        HierarchyNode parent = parentId != null ? findNodeById(parentId) : null;
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
        return node;
    }

    public synchronized boolean removeNode(long id) {
        return removeNodeRecursive(rootNodes, id);
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
