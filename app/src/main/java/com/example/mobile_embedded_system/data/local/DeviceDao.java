package com.example.mobile_embedded_system.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DeviceDao {

    @Deprecated
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DeviceEntity device);

    @Update
    void update(DeviceEntity device);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertWithAbort(DeviceEntity device);

    @Query("SELECT * FROM devices WHERE network_id = :networkId")
    LiveData<List<DeviceEntity>> getDevicesForNetwork(String networkId);

    @Query("SELECT * FROM devices WHERE network_id = :networkId")
    List<DeviceEntity> getDevicesForNetworkSync(String networkId);

    @Query("SELECT * FROM devices WHERE network_id = :networkId AND serial = :serial LIMIT 1")
    DeviceEntity getDeviceBySerialSync(String networkId, long serial);

    @Query("SELECT * FROM devices WHERE network_id = :networkId AND subject_id = :subjectId LIMIT 1")
    DeviceEntity getDeviceBySubjectSync(String networkId, String subjectId);

    @Query("DELETE FROM devices WHERE id = :id")
    void deleteById(String id);
}
