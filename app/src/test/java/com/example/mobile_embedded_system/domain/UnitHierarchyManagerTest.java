package com.example.mobile_embedded_system.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.lifecycle.LiveData;

import com.example.mobile_embedded_system.data.local.SubjectDao;
import com.example.mobile_embedded_system.data.local.SubjectEntity;
import com.example.mobile_embedded_system.data.local.TelemetryEntity;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class UnitHierarchyManagerTest {

    @Test
    public void testDefaultHierarchyInitialization() {
        UnitHierarchyManager manager = new UnitHierarchyManager();

        List<HierarchyNode> roots = manager.getRootNodes();
        assertNotNull(roots);
        assertEquals(1, roots.size());

        HierarchyNode battalion = roots.get(0);
        assertEquals("1-й Батальон", battalion.getName());
        assertEquals(HierarchyNode.NodeType.BATTALION, battalion.getType());

        List<Long> unitIds = manager.getAllUnitUserIds();
        assertEquals(3, unitIds.size());
        assertTrue(unitIds.contains(1001L));
        assertTrue(unitIds.contains(1002L));
        assertTrue(unitIds.contains(1003L));
    }

    @Test
    public void testFindNodeByUserId() {
        UnitHierarchyManager manager = new UnitHierarchyManager();
        HierarchyNode commander = manager.findNodeByUserId(1001L);
        assertNotNull(commander);
        assertEquals("Боец [1001] • Командир", commander.getName());
        assertEquals(1001L, (long) commander.getUserId());

        assertNull(manager.findNodeByUserId(9999L));
    }

    @Test
    public void testAddAndRemoveNode() {
        UnitHierarchyManager manager = new UnitHierarchyManager();

        HierarchyNode squad4 = manager.findNodeById(4L);
        assertNotNull(squad4);

        HierarchyNode newSoldier = manager.addNode("Боец [1004] • Пулемётчик", HierarchyNode.NodeType.SOLDIER, 4L, 1004L);
        assertNotNull(newSoldier);
        assertEquals(4L, (long) newSoldier.getParentId());
        assertEquals(1004L, (long) newSoldier.getUserId());

        List<Long> unitIds = manager.getAllUnitUserIds();
        assertEquals(4, unitIds.size());
        assertTrue(unitIds.contains(1004L));

        boolean removed = manager.removeNode(newSoldier.getId());
        assertTrue(removed);
        assertNull(manager.findNodeById(newSoldier.getId()));

        List<Long> unitIdsAfterRemove = manager.getAllUnitUserIds();
        assertEquals(3, unitIdsAfterRemove.size());
        assertFalse(unitIdsAfterRemove.contains(1004L));
    }

    @Test
    public void testEvaluateNodeSummary() {
        UnitHierarchyManager manager = new UnitHierarchyManager();
        HierarchyNode squad4 = manager.findNodeById(4L);
        assertNotNull(squad4);

        Map<Long, TelemetryEntity> telemetryMap = new HashMap<>();

        TelemetryEntity u1001 = new TelemetryEntity();
        u1001.userId = 1001L;
        u1001.receivedAtMs = System.currentTimeMillis();
        u1001.pulseBpm = 80;
        u1001.temperatureCelsius = 36.6;
        telemetryMap.put(1001L, u1001);

        TelemetryEntity u1002 = new TelemetryEntity();
        u1002.userId = 1002L;
        u1002.receivedAtMs = System.currentTimeMillis();
        u1002.pulseBpm = 145; // Critical
        u1002.temperatureCelsius = 37.0;
        telemetryMap.put(1002L, u1002);

        UnitHierarchyManager.NodeSummary summary = manager.evaluateNodeSummary(squad4, telemetryMap);
        assertEquals(3, summary.totalUnits);
        assertEquals(2, summary.onlineUnits);
        assertEquals(TacticalStatusEvaluator.Status.CRITICAL, summary.worstStatus);
    }

    @Test
    public void testSubjectDaoPersistence() {
        FakeSubjectDao fakeDao = new FakeSubjectDao();
        Executor directExecutor = Runnable::run;

        UnitHierarchyManager manager = new UnitHierarchyManager(fakeDao, directExecutor);

        // При первом запуске дефолтная иерархия сохраняется в DAO
        assertTrue(fakeDao.subjects.size() >= 7);

        // Добавляем узел
        HierarchyNode added = manager.addNode("Новый взвод", HierarchyNode.NodeType.PLATOON, 2L, null);
        assertNotNull(fakeDao.findById(String.valueOf(added.getId())));

        // Удаляем узел
        manager.removeNode(added.getId());
        assertNull(fakeDao.findById(String.valueOf(added.getId())));
    }

    @Test
    public void testMoveNodeCycleDetectionAndSuccess() {
        UnitHierarchyManager manager = new UnitHierarchyManager();
        // Node 2 (Взвод 1) -> Node 4 (Отделение А) -> Node 5 (Командир)
        // 1. Попытка переместить узел в самого себя
        try {
            manager.moveNode(2L, 2L);
            fail("Ожидалось исключение при перемещении узла в самого себя");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Циклическая"));
        }

        // 2. Попытка переместить родительский узел (2) внутрь своего потомка (4)
        try {
            manager.moveNode(2L, 4L);
            fail("Ожидалось исключение при создании цикла иерархии (AC-02.1)");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Циклическая"));
        }

        // 3. Корректное перемещение узла 4 в корень (null)
        boolean moved = manager.moveNode(4L, null);
        assertTrue(moved);
        HierarchyNode squad4 = manager.findNodeById(4L);
        assertNotNull(squad4);
        assertNull(squad4.getParentId());
    }

    @Test
    public void testIsSubordinateAcl() {
        UnitHierarchyManager manager = new UnitHierarchyManager();
        // В дефолтной иерархии 1001 - Командир отделения А, 1002 - Снайпер отделения А
        // Командир отделения может командовать подчиненным стрелком
        assertTrue(manager.isSubordinate(1001L, 1002L));

        // Сам себе всегда подчинен / имеет доступ
        assertTrue(manager.isSubordinate(1001L, 1001L));

        // Рядовой боец (1002) не может отдавать приказы командиру (1001)
        assertFalse(manager.isSubordinate(1002L, 1001L));

        // Несуществующий боец
        assertFalse(manager.isSubordinate(9999L, 1001L));
    }

    private static class FakeSubjectDao implements SubjectDao {
        final List<SubjectEntity> subjects = new ArrayList<>();

        @Override
        public void insert(SubjectEntity subject) {
            deleteById(subject.id);
            subjects.add(subject);
        }

        @Override
        public void insertAll(List<SubjectEntity> list) {
            for (SubjectEntity item : list) {
                insert(item);
            }
        }

        @Override
        public List<SubjectEntity> getAllSubjectsSync() {
            return new ArrayList<>(subjects);
        }

        @Override
        public void deleteById(String id) {
            subjects.removeIf(s -> s.id.equals(id));
        }

        @Override
        public void deleteAll() {
            subjects.clear();
        }

        @Override
        public LiveData<List<SubjectEntity>> getSubjectsForNetwork(String networkId) {
            return null;
        }

        @Override
        public List<SubjectEntity> getSubjectsForNetworkSync(String networkId) {
            List<SubjectEntity> res = new ArrayList<>();
            for (SubjectEntity s : subjects) {
                if (networkId.equals(s.networkId)) {
                    res.add(s);
                }
            }
            return res;
        }

        @Override
        public void deleteForNetwork(String networkId) {
            subjects.removeIf(s -> networkId.equals(s.networkId));
        }

        @Override
        public SubjectEntity getSubjectByIdSync(String id) {
            return findById(id);
        }

        @Override
        public SubjectEntity getSubjectByUserIdSync(String networkId, long userId) {
            for (SubjectEntity s : subjects) {
                if (networkId.equals(s.networkId) && s.userId != null && s.userId == userId) {
                    return s;
                }
            }
            return null;
        }

        SubjectEntity findById(String id) {
            for (SubjectEntity s : subjects) {
                if (s.id.equals(id)) return s;
            }
            return null;
        }
    }

    private static TelemetryEntity tele(long user, int pulse, double temp, long receivedAtMs) {
        TelemetryEntity e = new TelemetryEntity();
        e.userId = user;
        e.pulseBpm = pulse;
        e.temperatureCelsius = temp;
        e.receivedAtMs = receivedAtMs;
        return e;
    }

    @Test
    public void defaultTreeHasFiveLevelsWithExpectedPaths() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        assertEquals("BN1", m.findNodeById(1L).getHierarchyPath());
        assertEquals("BN1/CO2", m.findNodeById(2L).getHierarchyPath());
        assertEquals("BN1/CO2/PL3", m.findNodeById(3L).getHierarchyPath());
        assertEquals("BN1/CO2/PL3/SQ1", m.findNodeById(4L).getHierarchyPath());
        assertEquals("BN1/CO2/PL3/SQ1/1001", m.findNodeById(5L).getHierarchyPath());
        assertEquals(HierarchyNode.NodeType.SQUAD, m.findNodeById(4L).getType());
        assertEquals("mesh-a", m.getCurrentNetworkId());
    }

    @Test
    public void findNodeByUnknownIdIsNull() {
        assertNull(new UnitHierarchyManager().findNodeById(12345L));
    }

    @Test
    public void findByUserIdIgnoresNonSoldierNodes() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        m.addNode("Взвод с userId", HierarchyNode.NodeType.PLATOON, 2L, 777L);
        assertNull(m.findNodeByUserId(777L));
    }

    @Test
    public void addNodeToUnknownParentIsRejected() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        try {
            m.addNode("X", HierarchyNode.NodeType.SOLDIER, 999L, 1L);
            fail("ожидался IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("999"));
        }
    }

    @Test
    public void addedSoldierGetsPathWithUserId() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        HierarchyNode n = m.addNode("Боец [1004]", HierarchyNode.NodeType.SOLDIER, 4L, 1004L);
        assertEquals("BN1/CO2/PL3/SQ1/1004", n.getHierarchyPath());
        assertEquals(100L, n.getId());
        assertTrue(m.findNodeById(4L).getChildren().contains(n));
    }

    @Test
    public void addedNodeWithoutUserIdGetsPathWithNodeId() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        HierarchyNode n = m.addNode("Взвод 4", HierarchyNode.NodeType.PLATOON, 2L, null);
        assertEquals("BN1/CO2/" + n.getId(), n.getHierarchyPath());
    }

    @Test
    public void addedRootNodeUsesNodePathAndGrowsRoots() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        HierarchyNode root = m.addNode("Второй батальон", HierarchyNode.NodeType.BATTALION, null, null);
        assertEquals("NODE_100", root.getHierarchyPath());
        assertEquals(2, m.getRootNodes().size());
        assertNull(root.getParentId());
    }

    @Test
    public void generatedIdsAreSequential() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        long a = m.addNode("a", HierarchyNode.NodeType.SOLDIER, 4L, 2001L).getId();
        long b = m.addNode("b", HierarchyNode.NodeType.SOLDIER, 4L, 2002L).getId();
        assertEquals(a + 1, b);
    }

    @Test
    public void moveToDeepDescendantIsRejectedAndTreeIsUnchanged() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        try {
            m.moveNode(1L, 5L); // батальон в бойца своего же поддерева
            fail("циклическая иерархия должна быть отклонена");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("AC-02.1"));
        }
        assertNull(m.findNodeById(1L).getParentId());
        assertEquals(1, m.getRootNodes().size());
        assertEquals(Long.valueOf(1L), m.findNodeById(2L).getParentId());
        assertTrue(m.isDescendant(1L, 5L));
    }

    @Test
    public void moveToDirectChildIsRejected() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        try {
            m.moveNode(2L, 3L);
            fail("ожидался IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void moveToUnknownParentIsRejected() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        try {
            m.moveNode(4L, 999L);
            fail("ожидался IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("999"));
        }
        assertEquals(Long.valueOf(3L), m.findNodeById(4L).getParentId());
    }

    @Test
    public void moveUnknownNodeReturnsFalse() {
        assertFalse(new UnitHierarchyManager().moveNode(999L, 1L));
    }

    @Test
    public void moveToSiblingBranchReparentsNodeAndPath() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        assertTrue(m.moveNode(3L, 1L)); // взвод непосредственно под батальон
        HierarchyNode platoon = m.findNodeById(3L);
        assertEquals(Long.valueOf(1L), platoon.getParentId());
        assertEquals("BN1/3", platoon.getHierarchyPath());
        assertTrue(m.findNodeById(2L).getChildren().isEmpty());
        assertTrue(m.findNodeById(1L).getChildren().contains(platoon));
        assertTrue(m.getAllUnitUserIds().contains(1001L)); // бойцы не потеряны
    }

    @Test
    public void moveToRootMakesNodeRootWithNodePath() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        assertTrue(m.moveNode(4L, null));
        assertEquals(2, m.getRootNodes().size());
        assertEquals("NODE_4", m.findNodeById(4L).getHierarchyPath());
    }

    @Test
    public void isDescendantRelations() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        assertTrue(m.isDescendant(1L, 7L));
        assertTrue(m.isDescendant(3L, 5L));
        assertFalse(m.isDescendant(7L, 1L));
        assertFalse(m.isDescendant(4L, 4L));
        assertFalse(m.isDescendant(1L, 999L));
        assertFalse(m.isDescendant(999L, 1L));
    }

    @Test
    public void removeUnknownNodeReturnsFalse() {
        assertFalse(new UnitHierarchyManager().removeNode(999L));
    }

    @Test
    public void removeSubtreeDropsAllSoldiersBelowIt() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        m.addNode("Боец [1004]", HierarchyNode.NodeType.SOLDIER, 3L, 1004L);
        assertTrue(m.removeNode(4L)); // отделение с тремя бойцами
        assertNull(m.findNodeById(5L));
        assertEquals(java.util.Collections.singletonList(1004L), m.getAllUnitUserIds());
    }

    @Test
    public void duplicateUserIdIsListedOnce() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        m.addNode("Дубль", HierarchyNode.NodeType.SOLDIER, 4L, 1001L);
        assertEquals(3, m.getAllUnitUserIds().size());
    }

    @Test
    public void networkIdDefaultsToMeshAForNull() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        m.setCurrentNetworkId("mesh-b");
        assertEquals("mesh-b", m.getCurrentNetworkId());
        m.setCurrentNetworkId(null);
        assertEquals("mesh-a", m.getCurrentNetworkId());
    }

    @Test
    public void switchNetworkWithoutSubjectsRestoresDefaultTree() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        m.addNode("Лишний", HierarchyNode.NodeType.SOLDIER, 4L, 1500L);
        m.switchNetwork("mesh-b", null);
        assertEquals("mesh-b", m.getCurrentNetworkId());
        assertEquals(1, m.getRootNodes().size());
        assertEquals(3, m.getAllUnitUserIds().size());
    }

    @Test
    public void switchNetworkRebuildsTreeFromStoredSubjects() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        java.util.List<SubjectEntity> subjects = new ArrayList<>();
        subjects.add(new SubjectEntity("200", "mesh-b", null, "Рота Б", null, "COMPANY", "CB"));
        subjects.add(new SubjectEntity("201", "mesh-b", 2001L, "Боец Б", "200", "SOLDIER", "CB/2001"));
        subjects.add(new SubjectEntity("202", "mesh-b", 0L, "Взвод без пользователя", "200", "PLATOON", "CB/202"));
        m.switchNetwork("mesh-b", subjects);

        assertEquals(1, m.getRootNodes().size());
        assertEquals(200L, m.getRootNodes().get(0).getId());
        assertEquals(2, m.findNodeById(200L).getChildren().size());
        assertEquals(java.util.Collections.singletonList(2001L), m.getAllUnitUserIds());
        assertNull(m.findNodeById(202L).getUserId()); // userId = 0 трактуется как «нет пользователя»
        assertNull(m.findNodeByUserId(1001L));
        assertEquals(203L, m.addNode("Новый", HierarchyNode.NodeType.SOLDIER, 200L, 2003L).getId());
    }

    @Test
    public void orphanSubjectBecomesRoot() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        java.util.List<SubjectEntity> subjects = new ArrayList<>();
        subjects.add(new SubjectEntity("300", "mesh-b", 3001L, "Сирота", "299", "SOLDIER", "X/3001"));
        m.switchNetwork("mesh-b", subjects);
        assertEquals(1, m.getRootNodes().size());
        assertEquals(300L, m.getRootNodes().get(0).getId());
    }

    @Test
    public void unknownNodeTypeInStorageFallsBackToSoldier() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        java.util.List<SubjectEntity> subjects = new ArrayList<>();
        subjects.add(new SubjectEntity("400", "mesh-b", 4001L, "Странный", null, "GENERAL", "G"));
        m.switchNetwork("mesh-b", subjects);
        assertEquals(HierarchyNode.NodeType.SOLDIER, m.findNodeById(400L).getType());
    }

    @Test
    public void telemetryTopicForSquadSubscribesToWholeSubtree() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        assertEquals("mesh-a/BN1/CO2/PL3/SQ1/#", m.generateMqttTelemetryTopic(m.findNodeById(4L), "mesh-a"));
        assertEquals("mesh-b/BN1/#", m.generateMqttTelemetryTopic(m.findNodeById(1L), "/mesh-b/"));
    }

    @Test
    public void commanderMayCommandEveryoneInSquad() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        assertTrue(m.isSubordinate(1001L, 1002L));
        assertTrue(m.isSubordinate(1001L, 1003L));
    }

    @Test
    public void ordinarySoldiersCannotCommandPeers() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        assertFalse(m.isSubordinate(1002L, 1003L));
        assertFalse(m.isSubordinate(1003L, 1002L));
        assertFalse(m.isSubordinate(1003L, 1001L));
    }

    @Test
    public void unknownTargetIsNotSubordinate() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        assertFalse(m.isSubordinate(1001L, 9999L));
    }

    @Test
    public void nodeAboveInTreeMayCommandItsDescendant() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        m.addNode("Боец [1005]", HierarchyNode.NodeType.SOLDIER, 5L, 1005L); // под бойцом 1001
        assertTrue(m.isSubordinate(1001L, 1005L));
        assertFalse(m.isSubordinate(1005L, 1001L));
        assertFalse(m.isSubordinate(1002L, 1005L));
    }

    @Test
    public void soldierNamedCommanderMayCommandSiblings() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        m.addNode("Заместитель Командир отделения", HierarchyNode.NodeType.SOLDIER, 4L, 2001L);
        assertTrue(m.isSubordinate(2001L, 1002L));
        assertFalse(m.isSubordinate(1002L, 2001L));
    }

    @Test
    public void commandRightsFollowSoldierWhenMovedToAnotherSquad() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        HierarchyNode squad2 = m.addNode("2-е Отделение", HierarchyNode.NodeType.SQUAD, 3L, null);
        m.moveNode(6L, squad2.getId()); // 1002 уходит в чужое отделение
        // Командир 1001 больше не соседний узел и не предок для 1002 — но жёстко прошитое правило 1001 тоже требует общего родителя.
        assertFalse(m.isSubordinate(1001L, 1002L));
    }

    @Test
    public void summaryOfEmptyTelemetryCountsUnitsButNoneOnline() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        UnitHierarchyManager.NodeSummary s = m.evaluateNodeSummary(m.findNodeById(4L), new HashMap<Long, TelemetryEntity>());
        assertEquals(3, s.totalUnits);
        assertEquals(0, s.onlineUnits);
        assertEquals(TacticalStatusEvaluator.Status.OK, s.worstStatus);
    }

    @Test
    public void summaryOfBattalionAggregatesWholeSubtree() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        Map<Long, TelemetryEntity> data = new HashMap<>();
        long now = System.currentTimeMillis();
        data.put(1001L, tele(1001L, 70, 36.6, now));
        data.put(1002L, tele(1002L, 70, 36.6, now));
        data.put(1003L, tele(1003L, 70, 36.6, now));
        UnitHierarchyManager.NodeSummary s = m.evaluateNodeSummary(m.findNodeById(1L), data);
        assertEquals(3, s.totalUnits);
        assertEquals(3, s.onlineUnits);
        assertEquals(TacticalStatusEvaluator.Status.OK, s.worstStatus);
    }

    @Test
    public void staleReceptionIsOfflineButStillEvaluated() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        Map<Long, TelemetryEntity> data = new HashMap<>();
        long now = System.currentTimeMillis();
        data.put(1001L, tele(1001L, 70, 36.6, now - 121_000L));
        data.put(1002L, tele(1002L, 70, 36.6, now - 119_000L));
        data.put(1003L, tele(1003L, 100, 36.6, now - 500_000L));
        UnitHierarchyManager.NodeSummary s = m.evaluateNodeSummary(m.findNodeById(4L), data);
        assertEquals(1, s.onlineUnits);
        assertEquals(TacticalStatusEvaluator.Status.WARNING, s.worstStatus);
    }

    @Test
    public void criticalWinsOverWarningRegardlessOfOrder() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        Map<Long, TelemetryEntity> data = new HashMap<>();
        long now = System.currentTimeMillis();
        data.put(1001L, tele(1001L, 140, 36.6, now));
        data.put(1002L, tele(1002L, 100, 36.6, now));
        data.put(1003L, tele(1003L, 70, 36.6, now));
        assertEquals(TacticalStatusEvaluator.Status.CRITICAL,
                m.evaluateNodeSummary(m.findNodeById(4L), data).worstStatus);
    }

    @Test
    public void summaryOfSingleSoldierNodeIsItself() {
        UnitHierarchyManager m = new UnitHierarchyManager();
        Map<Long, TelemetryEntity> data = new HashMap<>();
        data.put(1002L, tele(1002L, 100, 36.6, System.currentTimeMillis()));
        UnitHierarchyManager.NodeSummary s = m.evaluateNodeSummary(m.findNodeById(6L), data);
        assertEquals(1, s.totalUnits);
        assertEquals(1, s.onlineUnits);
        assertEquals(TacticalStatusEvaluator.Status.WARNING, s.worstStatus);
    }

    @Test
    public void persistedHierarchyIsRestoredOnNextStart() {
        FakeSubjectDao dao = new FakeSubjectDao();
        Executor direct = Runnable::run;
        UnitHierarchyManager first = new UnitHierarchyManager(dao, direct);
        first.addNode("Боец [1004]", HierarchyNode.NodeType.SOLDIER, 4L, 1004L);

        UnitHierarchyManager second = new UnitHierarchyManager(dao, direct); // «перезапуск» (AC-03.4)
        assertTrue(second.getAllUnitUserIds().contains(1004L));
        assertEquals(4, second.getAllUnitUserIds().size());
        assertNotNull(second.findNodeByUserId(1004L));
    }

    @Test
    public void moveIsPersistedToDao() {
        FakeSubjectDao dao = new FakeSubjectDao();
        UnitHierarchyManager m = new UnitHierarchyManager(dao, Runnable::run);
        m.moveNode(3L, 1L);
        assertEquals("1", dao.findById("3").parentId);
    }

    @Test
    public void rejectedMoveDoesNotTouchDao() {
        FakeSubjectDao dao = new FakeSubjectDao();
        UnitHierarchyManager m = new UnitHierarchyManager(dao, Runnable::run);
        try {
            m.moveNode(1L, 5L);
        } catch (IllegalArgumentException expected) {
            // ok
        }
        assertEquals("2", dao.findById("3").parentId);
        assertNull(dao.findById("1").parentId);
    }

    @Test
    public void daoFailureOnStartupKeepsDefaultTree() {
        FakeSubjectDao broken = new FakeSubjectDao() {
            @Override
            public List<SubjectEntity> getAllSubjectsSync() {
                throw new IllegalStateException("db closed");
            }
        };
        UnitHierarchyManager m = new UnitHierarchyManager(broken, Runnable::run);
        assertEquals(3, m.getAllUnitUserIds().size());
        assertEquals(1, m.getRootNodes().size());
    }

    @Test
    public void switchNetworkOnlyPersistsForActiveNetwork() {
        FakeSubjectDao dao = new FakeSubjectDao();
        UnitHierarchyManager m = new UnitHierarchyManager(dao, Runnable::run);
        m.switchNetwork("mesh-b", null);
        boolean hasB = false;
        for (SubjectEntity s : dao.subjects) {
            if ("mesh-b".equals(s.networkId)) hasB = true;
        }
        assertTrue(hasB);
    }
}
