package com.example.mobile_embedded_system.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Сущность отправленных приказов (commands) по ТЗ (DataBase.txt).
 */
@Entity(tableName = "commands")
public class CommandEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @ColumnInfo(name = "network_id")
    public String networkId;

    @ColumnInfo(name = "source_subject_id")
    public long sourceSubjectId;

    @ColumnInfo(name = "destination_subject_id")
    public long destinationSubjectId;

    @ColumnInfo(name = "command_type")
    public int commandType;

    @ColumnInfo(name = "sequence")
    public long sequence;

    @ColumnInfo(name = "timestamp_ms")
    public long timestampMs;

    public CommandEntity(@NonNull String id, String networkId, long sourceSubjectId, long destinationSubjectId, int commandType, long sequence, long timestampMs) {
        this.id = id;
        this.networkId = networkId;
        this.sourceSubjectId = sourceSubjectId;
        this.destinationSubjectId = destinationSubjectId;
        this.commandType = commandType;
        this.sequence = sequence;
        this.timestampMs = timestampMs;
    }
}
