package com.example.mobile_embedded_system.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Модульные тесты узла иерархии подразделения (ТЗ §1.8–1.12).
 */
public class HierarchyNodeTest {

    @Test
    public void constructorKeepsAllFields() {
        HierarchyNode n = new HierarchyNode(5L, "Боец", HierarchyNode.NodeType.SOLDIER, 4L, 1001L, "BN1/1001");
        assertEquals(5L, n.getId());
        assertEquals("Боец", n.getName());
        assertEquals(HierarchyNode.NodeType.SOLDIER, n.getType());
        assertEquals(Long.valueOf(4L), n.getParentId());
        assertEquals(Long.valueOf(1001L), n.getUserId());
        assertEquals("BN1/1001", n.getHierarchyPath());
        assertTrue(n.getChildren().isEmpty());
    }

    @Test
    public void rootNodeHasNullParentAndUser() {
        HierarchyNode n = new HierarchyNode(1L, "Батальон", HierarchyNode.NodeType.BATTALION, null, null, "BN1");
        assertNull(n.getParentId());
        assertNull(n.getUserId());
    }

    @Test
    public void newNodeIsExpandedByDefault() {
        HierarchyNode n = new HierarchyNode(1L, "x", HierarchyNode.NodeType.SQUAD, null, null, "p");
        assertTrue(n.isExpanded());
        n.setExpanded(false);
        assertFalse(n.isExpanded());
    }

    @Test
    public void settersChangeMutableFields() {
        HierarchyNode n = new HierarchyNode(1L, "x", HierarchyNode.NodeType.SQUAD, null, null, "p");
        n.setName("y");
        n.setType(HierarchyNode.NodeType.PLATOON);
        n.setParentId(9L);
        n.setUserId(77L);
        n.setHierarchyPath("q/r");
        assertEquals("y", n.getName());
        assertEquals(HierarchyNode.NodeType.PLATOON, n.getType());
        assertEquals(Long.valueOf(9L), n.getParentId());
        assertEquals(Long.valueOf(77L), n.getUserId());
        assertEquals("q/r", n.getHierarchyPath());
        assertEquals(1L, n.getId());
    }

    @Test
    public void childrenKeepInsertionOrder() {
        HierarchyNode parent = new HierarchyNode(1L, "p", HierarchyNode.NodeType.SQUAD, null, null, "p");
        HierarchyNode a = new HierarchyNode(2L, "a", HierarchyNode.NodeType.SOLDIER, 1L, 1L, "p/1");
        HierarchyNode b = new HierarchyNode(3L, "b", HierarchyNode.NodeType.SOLDIER, 1L, 2L, "p/2");
        parent.addChild(a);
        parent.addChild(b);
        assertEquals(2, parent.getChildren().size());
        assertSame(a, parent.getChildren().get(0));
        assertSame(b, parent.getChildren().get(1));
    }

    @Test
    public void nodeTypeLabels() {
        assertEquals("РОТА", HierarchyNode.NodeType.COMPANY.getLabel());
        assertEquals("ВЗВОД", HierarchyNode.NodeType.PLATOON.getLabel());
        assertEquals("ОТДЕЛЕНИЕ", HierarchyNode.NodeType.SQUAD.getLabel());
        assertEquals("БОЕЦ", HierarchyNode.NodeType.SOLDIER.getLabel());
    }

    @Test
    public void allFiveNodeTypesExist() {
        assertEquals(5, HierarchyNode.NodeType.values().length);
        assertEquals(HierarchyNode.NodeType.BATTALION, HierarchyNode.NodeType.valueOf("BATTALION"));
    }
}
