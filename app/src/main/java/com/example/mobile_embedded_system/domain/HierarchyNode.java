package com.example.mobile_embedded_system.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Узел иерархии подразделения (ТЗ §1.8–1.12, §5.8–5.12).
 */
public class HierarchyNode {

    public enum NodeType {
        BATTALION("БА ТАЛЬОН"),
        COMPANY("РОТА"),
        PLATOON("ВЗВОД"),
        SQUAD("ОТДЕЛЕНИЕ"),
        SOLDIER("БОЕЦ");

        private final String label;

        NodeType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private final long id;
    private String name;
    private NodeType type;
    private Long parentId;
    private Long userId; // Только если тип SOLDIER
    private String hierarchyPath;
    private boolean isExpanded = true;
    private final List<HierarchyNode> children = new ArrayList<>();

    public HierarchyNode(long id, String name, NodeType type, Long parentId, Long userId, String hierarchyPath) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.parentId = parentId;
        this.userId = userId;
        this.hierarchyPath = hierarchyPath;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public NodeType getType() { return type; }
    public void setType(NodeType type) { this.type = type; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getHierarchyPath() { return hierarchyPath; }
    public void setHierarchyPath(String hierarchyPath) { this.hierarchyPath = hierarchyPath; }

    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }

    public List<HierarchyNode> getChildren() { return children; }
    public void addChild(HierarchyNode child) { children.add(child); }
}
