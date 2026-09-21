package com.example.mobile_embedded_system.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SubjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SubjectEntity subject);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SubjectEntity> subjects);

    @Query("SELECT * FROM subjects")
    List<SubjectEntity> getAllSubjectsSync();

    @Query("DELETE FROM subjects WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM subjects")
    void deleteAll();

    @Query("SELECT * FROM subjects WHERE network_id = :networkId")
    LiveData<List<SubjectEntity>> getSubjectsForNetwork(String networkId);
}
