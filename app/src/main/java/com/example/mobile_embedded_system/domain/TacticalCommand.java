package com.example.mobile_embedded_system.domain;

import com.example.mobile_embedded_system.data.model.LMashPayload;

/**
 * Пакет тактического приказа целеуказания (C2 Uplink) (ТЗ §4.1, §14).
 */
public class TacticalCommand {

    public static final String CMD_ASSIGN_TARGET = "ASSIGN_TARGET";

    private final String command;
    private final long targetUserId;
    private final String waypointCallsign;
    private final double latitude;
    private final double longitude;
    private final long timestampMs;

    public TacticalCommand(long targetUserId, String waypointCallsign, double latitude, double longitude) {
        this.command = CMD_ASSIGN_TARGET;
        this.targetUserId = targetUserId;
        this.waypointCallsign = waypointCallsign;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestampMs = System.currentTimeMillis();
    }

    public String getCommand() {
        return command;
    }

    public long getTargetUserId() {
        return targetUserId;
    }

    public String getWaypointCallsign() {
        return waypointCallsign;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    /**
     * Сериализация приказа в бинарный пакет LMashPayload (54 байта Little-Endian по ТЗ §3.3).
     */
    public LMashPayload toLMashPayload(long sequence, long sourceId) {
        long timestampSec = timestampMs / 1000L;
        long deviceSerial = (sourceId != 0L) ? sourceId : 99881100L;
        LMashPayload payload =
                LMashPayload.createCommand(
                        sequence,
                        timestampSec,
                        deviceSerial,
                        sourceId,
                        targetUserId,
                        LMashPayload.CMD_HOLD
                );
        payload.setLatitudeE7((int) (latitude * 1e7));
        payload.setLongitudeE7((int) (longitude * 1e7));
        return payload;
    }

    public byte[] toBytes(long sequence, long sourceId) {
        return toLMashPayload(sequence, sourceId).toBytes();
    }
}