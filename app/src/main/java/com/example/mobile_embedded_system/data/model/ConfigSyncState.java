package com.example.mobile_embedded_system.data.model;

/**
 * Статусы синхронизации конфигурации устройства по ТЗ (§11.58, §11.59).
 */
public enum ConfigSyncState {
    APPLIED("Применено"),
    PENDING("Ожидает применения"),
    CONFIGURATION_REQUIRED("Требует обновления конфигурации"),
    ERROR("Ошибка применения");

    private final String description;

    ConfigSyncState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
