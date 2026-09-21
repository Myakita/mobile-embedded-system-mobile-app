package com.example.mobile_embedded_system.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Сущность привязанного устройства (devices) по ТЗ (DataBase.txt).
 */
@Entity(tableName = "devices")
public class DeviceEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @ColumnInfo(name = "network_id")
    public String networkId;

    @ColumnInfo(name = "serial")
    public long serial;

    @ColumnInfo(name = "subject_id")
    public String subjectId;

    @ColumnInfo(name = "last_seen_ms")
    public long lastSeenMs;

    public DeviceEntity(@NonNull String id, String networkId, long serial, String subjectId, long lastSeenMs) {
        this.id = id;
        this.networkId = networkId;
        this.serial = serial;
        this.subjectId = subjectId;
        this.lastSeenMs = lastSeenMs;
    }
}
