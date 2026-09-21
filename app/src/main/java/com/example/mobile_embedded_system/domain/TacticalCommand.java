package com.example.mobile_embedded_system.domain;

import com.example.mobile_embedded_system.data.model.LMashPayload;

/**
 * Пакет тактического приказа целеуказания (C2 Uplink) (ТЗ §4.1, §14).
 */
public class TacticalCommand {

    public static final String CMD_ASSIGN_TARGET = "ASSIGN_TARGET";
    public static final String CMD_CHECK_IN = "CHECK_IN";
    public static final String CMD_HOLD = "HOLD";
    public static final String CMD_RETURN = "RETURN";

    private final String command;
    private final int commandType;
    private final long targetUserId;
    private final String waypointCallsign;
    private final double latitude;
    private final double longitude;
    private final long timestampMs;

    public TacticalCommand(long targetUserId, String waypointCallsign, double latitude, double longitude) {
        this(targetUserId, LMashPayload.CMD_HOLD, CMD_ASSIGN_TARGET, waypointCallsign, latitude, longitude);
    }

    public TacticalCommand(long targetUserId, int commandType, String command, String waypointCallsign, double latitude, double longitude) {
        this.targetUserId = targetUserId;
        this.commandType = commandType;
        this.command = command;
        this.waypointCallsign = waypointCallsign;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestampMs = System.currentTimeMillis();
    }

    public static TacticalCommand createCheckIn(long targetUserId) {
        return new TacticalCommand(targetUserId, LMashPayload.CMD_CHECK_IN, CMD_CHECK_IN, "CHECK_IN", 0.0, 0.0);
    }

    public static TacticalCommand createHold(long targetUserId, double latitude, double longitude) {
        return new TacticalCommand(targetUserId, LMashPayload.CMD_HOLD, CMD_HOLD, "HOLD", latitude, longitude);
    }

    public static TacticalCommand createReturn(long targetUserId) {
        return new TacticalCommand(targetUserId, LMashPayload.CMD_RETURN, CMD_RETURN, "RETURN", 0.0, 0.0);
    }

    public String getCommand() {
        return command;
    }

    public int getCommandType() {
        return commandType;
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
                        commandType
                );
        payload.setLatitudeE7((int) (latitude * 1e7));
        payload.setLongitudeE7((int) (longitude * 1e7));
        return payload;
    }

    public byte[] toBytes(long sequence, long sourceId) {
        return toLMashPayload(sequence, sourceId).toBytes();
    }
}