package com.example.mobile_embedded_system.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                TelemetryEntity.class,
                DeviceConfigEntity.class,
                NetworkEntity.class,
                SubjectEntity.class,
                DeviceEntity.class,
                CommandEntity.class
        },
        version = 5,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract TelemetryDao telemetryDao();
    public abstract DeviceConfigDao deviceConfigDao();
    public abstract NetworkDao networkDao();
    public abstract SubjectDao subjectDao();
    public abstract DeviceDao deviceDao();
    public abstract CommandDao commandDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "unit_monitor_db"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}