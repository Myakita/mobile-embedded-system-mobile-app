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

        SubjectEntity findById(String id) {
            for (SubjectEntity s : subjects) {
                if (s.id.equals(id)) return s;
            }
            return null;
        }
    }
}
