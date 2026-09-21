package com.example.mobile_embedded_system.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Сущность участника группы (subjects) по ТЗ (DataBase.txt).
 */
@Entity(tableName = "subjects")
public class SubjectEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @ColumnInfo(name = "network_id")
    public String networkId;

    @ColumnInfo(name = "user_id")
    public long userId;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "parent_id")
    public String parentId;

    @ColumnInfo(name = "node_type")
    public String nodeType;

    @ColumnInfo(name = "hierarchy_path")
    public String hierarchyPath;

    public SubjectEntity() {
    }

    @Ignore
    public SubjectEntity(@NonNull String id, String networkId, long userId, String name, String parentId) {
        this(id, networkId, userId, name, parentId, null, null);
    }

    @Ignore
    public SubjectEntity(@NonNull String id, String networkId, long userId, String name, String parentId, String nodeType, String hierarchyPath) {
        this.id = id;
        this.networkId = networkId;
        this.userId = userId;
        this.name = name;
        this.parentId = parentId;
        this.nodeType = nodeType;
        this.hierarchyPath = hierarchyPath;
    }
}
