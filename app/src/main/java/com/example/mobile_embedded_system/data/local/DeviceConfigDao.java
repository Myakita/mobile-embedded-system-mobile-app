package com.example.mobile_embedded_system.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface DeviceConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(DeviceConfigEntity config);

    @Query("SELECT * FROM device_configs WHERE device_serial = :deviceSerial LIMIT 1")
    LiveData<DeviceConfigEntity> getConfigForDevice(long deviceSerial);

    @Query("SELECT * FROM device_configs WHERE device_serial = :deviceSerial LIMIT 1")
    DeviceConfigEntity getConfigForDeviceSync(long deviceSerial);
}
