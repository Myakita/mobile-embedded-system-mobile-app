package com.example.mobile_embedded_system.domain;

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
}
