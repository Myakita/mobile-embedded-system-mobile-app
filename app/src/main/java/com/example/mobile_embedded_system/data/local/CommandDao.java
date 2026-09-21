package com.example.mobile_embedded_system.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CommandDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CommandEntity command);

    @Query("SELECT * FROM commands WHERE network_id = :networkId ORDER BY timestamp_ms DESC")
    LiveData<List<CommandEntity>> getCommandsForNetwork(String networkId);
}
