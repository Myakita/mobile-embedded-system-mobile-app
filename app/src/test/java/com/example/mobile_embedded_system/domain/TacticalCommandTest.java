package com.example.mobile_embedded_system.domain;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.example.mobile_embedded_system.data.model.LMashPayload;

import org.junit.Test;

/**
 * Тестирование сериализации бинарных пакетов приказа Uplink (ТЗ §5.1, §14).
 */
public class TacticalCommandTest {

    @Test
    public void testCommandBinaryPayloadSerialization() {
        TacticalCommand cmd = new TacticalCommand(1002L, "ОБУ-1", 55.753912, 37.620811);

        assertEquals("ASSIGN_TARGET", cmd.getCommand());
        assertEquals(1002L, cmd.getTargetUserId());
        assertEquals("ОБУ-1", cmd.getWaypointCallsign());

        LMashPayload payload = cmd.toLMashPayload(10L, 1001L);
        assertNotNull(payload);
        assertEquals(10L, payload.getSequence());
        assertEquals(1001L, payload.getUserId());
        assertEquals(1002L, payload.getDestinationId());
        assertEquals(557539120, payload.getLatitudeE7());
        assertEquals(376208110, payload.getLongitudeE7());

        byte[] bytes = cmd.toBytes(10L, 1001L);
        assertNotNull(bytes);
        assertEquals(LMashPayload.PAYLOAD_SIZE, bytes.length);
    }

    @Test
    public void testCheckInCommandPayloadSerialization() {
        TacticalCommand checkIn = TacticalCommand.createCheckIn(1003L);

        assertEquals("CHECK_IN", checkIn.getCommand());
        assertEquals(1003L, checkIn.getTargetUserId());
        assertEquals(LMashPayload.CMD_CHECK_IN, checkIn.getCommandType());

        LMashPayload payload = checkIn.toLMashPayload(25L, 1001L);
        assertNotNull(payload);
        assertEquals(LMashPayload.MSG_COMMAND, payload.getMessageType());
        assertEquals(LMashPayload.CMD_CHECK_IN, payload.getCommandType());
        assertEquals(25L, payload.getSequence());
        assertEquals(1001L, payload.getUserId());
        assertEquals(1003L, payload.getDestinationId());

        byte[] bytes = checkIn.toBytes(25L, 1001L);
        assertEquals(LMashPayload.PAYLOAD_SIZE, bytes.length);
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x & 0xFF));
        return sb.toString();
    }

    @Test
    public void holdCommandCarriesCoordinatesAndType() {
        TacticalCommand hold = TacticalCommand.createHold(1002L, 10.0, 20.0);
        assertEquals("HOLD", hold.getCommand());
        assertEquals(LMashPayload.CMD_HOLD, hold.getCommandType());
        assertEquals(10.0, hold.getLatitude(), 0.0);
        assertEquals(20.0, hold.getLongitude(), 0.0);
        LMashPayload p = hold.toLMashPayload(1L, 1001L);
        assertEquals(100000000, p.getLatitudeE7());
        assertEquals(200000000, p.getLongitudeE7());
        assertEquals(LMashPayload.CMD_HOLD, p.getCommandType());
    }

    @Test
    public void returnCommandHasZeroCoordinates() {
        TacticalCommand ret = TacticalCommand.createReturn(1003L);
        assertEquals("RETURN", ret.getCommand());
        assertEquals(LMashPayload.CMD_RETURN, ret.getCommandType());
        assertEquals(1003L, ret.getTargetUserId());
        LMashPayload p = ret.toLMashPayload(5L, 1001L);
        assertEquals(0, p.getLatitudeE7());
        assertEquals(0, p.getLongitudeE7());
        assertEquals(LMashPayload.CMD_RETURN, p.getCommandType());
    }

    @Test
    public void negativeCoordinatesAreScaledWithSign() {
        TacticalCommand hold = TacticalCommand.createHold(1002L, -33.5, -70.25);
        LMashPayload p = hold.toLMashPayload(1L, 1001L);
        assertEquals(-335000000, p.getLatitudeE7());
        assertEquals(-702500000, p.getLongitudeE7());
    }

    @Test
    public void payloadIsAlwaysCommandMessageWithDefaults() {
        LMashPayload p = TacticalCommand.createCheckIn(1002L).toLMashPayload(9L, 1001L);
        assertTrue(p.isCommand());
        assertEquals(1, p.getVersion());
        assertEquals(4, p.getTtl());
        assertEquals(0, p.getFlags());
        assertEquals(9L, p.getSequence());
    }

    @Test
    public void zeroSourceIdFallsBackToDefaultDeviceSerial() {
        LMashPayload p = TacticalCommand.createCheckIn(1002L).toLMashPayload(1L, 0L);
        assertEquals(99881100L, p.getDeviceSerial());
        assertEquals(0L, p.getUserId());
    }

    @Test
    public void nonZeroSourceIdIsUsedAsDeviceSerialAndUser() {
        LMashPayload p = TacticalCommand.createCheckIn(1002L).toLMashPayload(1L, 1001L);
        assertEquals(1001L, p.getDeviceSerial());
        assertEquals(1001L, p.getUserId());
    }

    @Test
    public void timestampIsInSecondsOfCreationTime() {
        long before = System.currentTimeMillis() / 1000L;
        TacticalCommand c = TacticalCommand.createCheckIn(1002L);
        LMashPayload p = c.toLMashPayload(1L, 1001L);
        long after = System.currentTimeMillis() / 1000L;
        assertTrue(p.getTimestamp() >= before && p.getTimestamp() <= after);
        assertEquals(c.getTimestampMs() / 1000L, p.getTimestamp());
    }

    @Test
    public void toBytesRoundTripsThroughParser() {
        byte[] bytes = TacticalCommand.createHold(1002L, 55.5, 37.5).toBytes(77L, 1001L);
        LMashPayload parsed = LMashPayload.fromBytes(bytes);
        assertTrue(parsed.isCommand());
        assertEquals(77L, parsed.getSequence());
        assertEquals(1002L, parsed.getDestinationId());
        assertEquals(555000000, parsed.getLatitudeE7());
        assertEquals(375000000, parsed.getLongitudeE7());
    }

    @Test
    public void returnCommandMatchesProtocolVectorWireBytes() {
        // Вектор protocol/vectors/command-return-v1.json: поля, которые зависят от времени и серийника, задаются явно.
        LMashPayload p = TacticalCommand.createReturn(2055410180L).toLMashPayload(102L, 1001L);
        p.setTimestamp(1726000050L);
        p.setDeviceSerial(430572065L);
        assertEquals("010103000466000000b2abe0662102aa1900000000e9030000000000000412837a"
                + "000000000000000000000000000000000000000000", hex(p.toBytes()));
    }

    @Test
    public void defaultConstructorKeepsAssignTargetNameWithHoldWireType() {
        TacticalCommand c = new TacticalCommand(1002L, "ОБУ-5", 1.0, 2.0);
        assertEquals(TacticalCommand.CMD_ASSIGN_TARGET, c.getCommand());
        assertEquals(LMashPayload.CMD_HOLD, c.getCommandType());
        assertEquals("ОБУ-5", c.getWaypointCallsign());
    }

    @Test
    public void commandConstantsAreStable() {
        assertEquals("ASSIGN_TARGET", TacticalCommand.CMD_ASSIGN_TARGET);
        assertEquals("CHECK_IN", TacticalCommand.CMD_CHECK_IN);
        assertEquals("HOLD", TacticalCommand.CMD_HOLD);
        assertEquals("RETURN", TacticalCommand.CMD_RETURN);
    }

    @Test
    public void differentSequencesGiveDifferentBytes() {
        TacticalCommand c = TacticalCommand.createCheckIn(1002L);
        assertNotEquals(hex(c.toBytes(1L, 1001L)), hex(c.toBytes(2L, 1001L)));
    }
}
