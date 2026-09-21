package com.example.mobile_embedded_system.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TelemetryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(TelemetryEntity record);

    @Query("SELECT * FROM telemetry_records WHERE user_id = :userId ORDER BY timestamp DESC, sequence DESC LIMIT 1")
    LiveData<TelemetryEntity> getLatestTelemetryForUser(long userId);

    @Query("SELECT * FROM telemetry_records WHERE user_id = :userId AND timestamp >= :fromTimestamp ORDER BY timestamp ASC, sequence ASC")
    LiveData<List<TelemetryEntity>> getHistoryForUser(long userId, long fromTimestamp);

    @Query("SELECT * FROM telemetry_records WHERE user_id = :userId AND timestamp >= :fromTimestamp ORDER BY timestamp ASC, sequence ASC")
    List<TelemetryEntity> getHistoryForUserSync(long userId, long fromTimestamp);

    @Query("SELECT * FROM telemetry_records WHERE user_id = :userId AND network_id = :networkId ORDER BY timestamp DESC, sequence DESC LIMIT 1")
    LiveData<TelemetryEntity> getLatestTelemetryForUserInNetwork(long userId, String networkId);

    @Query("SELECT * FROM telemetry_records WHERE user_id = :userId AND network_id = :networkId AND timestamp >= :fromTimestamp ORDER BY timestamp ASC, sequence ASC")
    LiveData<List<TelemetryEntity>> getHistoryForUserInNetwork(long userId, String networkId, long fromTimestamp);

    @Query("SELECT * FROM telemetry_records WHERE user_id = :userId AND network_id = :networkId AND timestamp >= :fromTimestamp ORDER BY timestamp ASC, sequence ASC")
    List<TelemetryEntity> getHistoryForUserInNetworkSync(long userId, String networkId, long fromTimestamp);

    /**
     * Удаление устаревших записей телеметрии по времени формирования (в секундах Unix Epoch).
     */
    @Query("DELETE FROM telemetry_records WHERE timestamp < :cutoffTimestampSec")
    int deleteOlderThan(long cutoffTimestampSec);
}