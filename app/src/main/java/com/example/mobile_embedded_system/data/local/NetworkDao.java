package com.example.mobile_embedded_system.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface NetworkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(NetworkEntity network);

    @Query("SELECT * FROM networks ORDER BY created_at_ms ASC")
    LiveData<List<NetworkEntity>> getAllNetworks();

    @Query("SELECT * FROM networks WHERE id = :networkId LIMIT 1")
    NetworkEntity getNetworkSync(String networkId);
}
