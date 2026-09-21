package com.example.mobile_embedded_system.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Сущность мультисети (networks) по ТЗ (§1.2–1.3, DataBase.txt).
 */
@Entity(tableName = "networks")
public class NetworkEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "mqtt_root")
    public String mqttRoot;

    @ColumnInfo(name = "created_at_ms")
    public long createdAtMs;

    public NetworkEntity(@NonNull String id, String name, String mqttRoot, long createdAtMs) {
        this.id = id;
        this.name = name;
        this.mqttRoot = mqttRoot;
        this.createdAtMs = createdAtMs;
    }
}
