package com.example.mobile_embedded_system.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
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

    public SubjectEntity(@NonNull String id, String networkId, long userId, String name, String parentId) {
        this.id = id;
        this.networkId = networkId;
        this.userId = userId;
        this.name = name;
        this.parentId = parentId;
    }
}
