package com.example.mobile_embedded_system.data.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LMashPayloadTest {

    @Test
    public void testSerializationAndDeserialization() {
        LMashPayload source = new LMashPayload();
        source.setVersion(1);
        source.setMessageType(LMashPayload.MSG_TELEMETRY);
        source.setCommandType(LMashPayload.CMD_NONE);
        source.setFlags(0x05);
        source.setTtl(4);
        source.setSequence(471L);
        source.setTimestamp(1726000000L);
        source.setDeviceSerial(0x19AA0221L);
        source.setUserId(1001L);
        source.setDestinationId(0x7A831204L);
        source.setTemperatureX100((short) 3672);
        source.setPulseBpm(84);
        source.setPressureSys(120);
        source.setPressureDia(80);
        source.setLatitudeE7(557539123);
        source.setLongitudeE7(376208110);
        source.setHeadingCdeg(18050);
        source.setPositionQuality(3);

        byte[] serialized = source.toBytes();
        assertEquals(LMashPayload.PAYLOAD_SIZE, serialized.length);

        LMashPayload restored = LMashPayload.fromBytes(serialized);

        assertEquals(source, restored);
        assertEquals(36.72, restored.getTemperatureCelsius(), 0.001);
        assertEquals(55.7539123, restored.getLatitude(), 0.0000001);
        assertEquals(37.6208110, restored.getLongitude(), 0.0000001);
        assertEquals(180.50, restored.getHeadingDegrees(), 0.01);
        assertEquals("120 / 80", restored.getPressureString());
        assertEquals("Хорошее", restored.getPositionQualityText());
        assertTrue(restored.isTelemetry());
    }

    @Test
    public void testCommandCreation() {
        LMashPayload cmd = LMashPayload.createCommand(
                102L,
                1726000050L,
                0x19AA0221L,
                1001L,
                0x7A831204L,
                LMashPayload.CMD_RETURN
        );

        byte[] rawBytes = cmd.toBytes();
        assertEquals(LMashPayload.PAYLOAD_SIZE, rawBytes.length);

        LMashPayload parsed = LMashPayload.fromBytes(rawBytes);
        assertTrue(parsed.isCommand());
        assertEquals(LMashPayload.CMD_RETURN, parsed.getCommandType());
        assertEquals(102L, parsed.getSequence());
    }

    private static final String TELEMETRY_VECTOR_HEX =
            "0100000504d701000080abe0662102aa1900000000e9030000000000000412837a00000000"
                    + "580e54007850335f3b21ee7a6c16824603";
    private static final String COMMAND_VECTOR_HEX =
            "010103000466000000b2abe0662102aa1900000000e9030000000000000412837a"
                    + "000000000000000000000000000000000000000000";

    private static byte[] unhex(String s) {
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        }
        return out;
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x & 0xFF));
        return sb.toString();
    }

    @Test
    public void telemetryVectorDecodesToDocumentedFields() {
        byte[] wire = unhex(TELEMETRY_VECTOR_HEX);
        assertEquals(54, wire.length);
        LMashPayload p = LMashPayload.fromBytes(wire);
        assertEquals(1, p.getVersion());
        assertTrue(p.isTelemetry());
        assertEquals(LMashPayload.CMD_NONE, p.getCommandType());
        assertEquals(5, p.getFlags());
        assertEquals(4, p.getTtl());
        assertEquals(471L, p.getSequence());
        assertEquals(1726000000L, p.getTimestamp());
        assertEquals(430572065L, p.getDeviceSerial());
        assertEquals(1001L, p.getUserId());
        assertEquals(2055410180L, p.getDestinationId());
        assertEquals(3672, p.getTemperatureX100());
        assertEquals(84, p.getPulseBpm());
        assertEquals(120, p.getPressureSys());
        assertEquals(80, p.getPressureDia());
        assertEquals(557539123, p.getLatitudeE7());
        assertEquals(376208110, p.getLongitudeE7());
        assertEquals(18050, p.getHeadingCdeg());
        assertEquals(3, p.getPositionQuality());
    }

    @Test
    public void telemetryVectorEncodesToExactWireBytes() {
        LMashPayload p = new LMashPayload();
        p.setFlags(5);
        p.setSequence(471L);
        p.setTimestamp(1726000000L);
        p.setDeviceSerial(430572065L);
        p.setUserId(1001L);
        p.setDestinationId(2055410180L);
        p.setTemperatureX100((short) 3672);
        p.setPulseBpm(84);
        p.setPressureSys(120);
        p.setPressureDia(80);
        p.setLatitudeE7(557539123);
        p.setLongitudeE7(376208110);
        p.setHeadingCdeg(18050);
        p.setPositionQuality(3);
        assertEquals(TELEMETRY_VECTOR_HEX, hex(p.toBytes()));
    }

    @Test
    public void commandVectorDecodesAndEncodesBack() {
        LMashPayload p = LMashPayload.fromBytes(unhex(COMMAND_VECTOR_HEX));
        assertTrue(p.isCommand());
        assertEquals(LMashPayload.CMD_RETURN, p.getCommandType());
        assertEquals(102L, p.getSequence());
        assertEquals(1726000050L, p.getTimestamp());
        assertEquals(430572065L, p.getDeviceSerial());
        assertEquals(1001L, p.getUserId());
        assertEquals(2055410180L, p.getDestinationId());
        assertEquals(COMMAND_VECTOR_HEX, hex(p.toBytes()));
    }

    @Test
    public void createCommandMatchesCommandVector() {
        LMashPayload p = LMashPayload.createCommand(102L, 1726000050L, 430572065L, 1001L, 2055410180L, LMashPayload.CMD_RETURN);
        assertEquals(COMMAND_VECTOR_HEX, hex(p.toBytes()));
    }

    @Test
    public void commandCarriesZeroTelemetryFields() {
        LMashPayload p = LMashPayload.createCommand(1L, 2L, 3L, 4L, 5L, LMashPayload.CMD_CHECK_IN);
        assertEquals(0, p.getTemperatureX100());
        assertEquals(0, p.getPulseBpm());
        assertEquals(0, p.getLatitudeE7());
        assertEquals(0, p.getPositionQuality());
        assertEquals(0, p.getFlags());
        assertEquals(4, p.getTtl());
        assertEquals(1, p.getVersion());
    }

    @Test
    public void defaultPayloadIsVersion1Ttl4Telemetry() {
        LMashPayload p = new LMashPayload();
        assertEquals(1, p.getVersion());
        assertEquals(4, p.getTtl());
        assertTrue(p.isTelemetry());
        assertFalse(p.isCommand());
        assertEquals(0L, p.getSequence());
    }

    @Test
    public void nullArrayIsRejected() {
        try {
            LMashPayload.fromBytes(null);
            fail("ожидался IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("54"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyArrayIsRejected() {
        LMashPayload.fromBytes(new byte[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shortArrayOf53BytesIsRejected() {
        LMashPayload.fromBytes(new byte[53]);
    }

    @Test
    public void exactly54ZeroBytesParse() {
        LMashPayload p = LMashPayload.fromBytes(new byte[54]);
        assertEquals(0, p.getVersion());
        assertTrue(p.isTelemetry());
        assertEquals(0L, p.getSequence());
    }

    @Test
    public void allOnesBytesParseAsUnsignedFields() {
        byte[] ff = new byte[54];
        java.util.Arrays.fill(ff, (byte) 0xFF);
        LMashPayload p = LMashPayload.fromBytes(ff);
        assertEquals(255, p.getVersion());
        assertEquals(255, p.getMessageType());
        assertEquals(255, p.getTtl());
        assertEquals(4294967295L, p.getSequence());
        assertEquals(4294967295L, p.getTimestamp());
        assertEquals(65535, p.getPulseBpm());
        assertEquals(255, p.getPressureSys());
        assertEquals(255, p.getPressureDia());
        assertEquals(65535, p.getHeadingCdeg());
        assertEquals(255, p.getPositionQuality());
        assertEquals(-1, p.getLatitudeE7());
        assertEquals(-1, p.getTemperatureX100());
        assertFalse(p.isTelemetry());
        assertFalse(p.isCommand());
    }

    @Test
    public void allOnesBytesRoundTripUnchanged() {
        byte[] ff = new byte[54];
        java.util.Arrays.fill(ff, (byte) 0xFF);
        assertArrayEquals(ff, LMashPayload.fromBytes(ff).toBytes());
    }

    @Test
    public void multiByteFieldsAreLittleEndian() {
        LMashPayload p = new LMashPayload();
        p.setSequence(0x01020304L);
        p.setTimestamp(0x0A0B0C0DL);
        p.setDeviceSerial(0x0102030405060708L);
        p.setPulseBpm(0x1234);
        p.setLatitudeE7(0x11223344);
        p.setHeadingCdeg(0xABCD);
        byte[] b = p.toBytes();
        assertEquals(54, b.length);
        assertEquals(0x04, b[5]);
        assertEquals(0x01, b[8]);
        assertEquals(0x0D, b[9]);
        assertEquals(0x0A, b[12]);
        assertEquals(0x08, b[13]);
        assertEquals(0x01, b[20]);
        assertEquals(0x34, b[39]);
        assertEquals(0x12, b[40]);
        assertEquals(0x44, b[43]);
        assertEquals(0x11, b[46]);
        assertEquals((byte) 0xCD, b[51]);
        assertEquals((byte) 0xAB, b[52]);
    }

    @Test
    public void headerBytesAreInSpecOrder() {
        LMashPayload p = new LMashPayload();
        p.setVersion(1);
        p.setMessageType(LMashPayload.MSG_COMMAND);
        p.setCommandType(LMashPayload.CMD_HOLD);
        p.setFlags(0x80);
        p.setTtl(7);
        p.setPositionQuality(2);
        byte[] b = p.toBytes();
        assertEquals(1, b[0]);
        assertEquals(1, b[1]);
        assertEquals(2, b[2]);
        assertEquals((byte) 0x80, b[3]);
        assertEquals(7, b[4]);
        assertEquals(2, b[53]);
    }

    @Test
    public void negativeValuesRoundTrip() {
        LMashPayload p = new LMashPayload();
        p.setTemperatureX100((short) -1050);
        p.setLatitudeE7(-335000000);
        p.setLongitudeE7(-702500000);
        LMashPayload r = LMashPayload.fromBytes(p.toBytes());
        assertEquals(-10.5, r.getTemperatureCelsius(), 1e-9);
        assertEquals(-33.5, r.getLatitude(), 1e-9);
        assertEquals(-70.25, r.getLongitude(), 1e-9);
    }

    @Test
    public void sequenceAboveUint32IsTruncatedToLow32Bits() {
        LMashPayload p = new LMashPayload();
        p.setSequence(0x1_0000_0001L);
        assertEquals(1L, LMashPayload.fromBytes(p.toBytes()).getSequence());
    }

    @Test
    public void maxUint32SequenceStaysPositive() {
        LMashPayload p = new LMashPayload();
        p.setSequence(4294967295L);
        assertEquals(4294967295L, LMashPayload.fromBytes(p.toBytes()).getSequence());
    }

    @Test
    public void scalingHelpers() {
        LMashPayload p = new LMashPayload();
        p.setTemperatureX100((short) 3650);
        p.setLatitudeE7(900000000);
        p.setLongitudeE7(-1800000000);
        p.setHeadingCdeg(36000);
        assertEquals(36.5, p.getTemperatureCelsius(), 1e-9);
        assertEquals(90.0, p.getLatitude(), 1e-9);
        assertEquals(-180.0, p.getLongitude(), 1e-9);
        assertEquals(360.0, p.getHeadingDegrees(), 1e-9);
    }

    @Test
    public void positionQualityTexts() {
        LMashPayload p = new LMashPayload();
        p.setPositionQuality(0);
        assertEquals("Недоступно", p.getPositionQualityText());
        p.setPositionQuality(1);
        assertEquals("Низкое", p.getPositionQualityText());
        p.setPositionQuality(2);
        assertEquals("Ограниченное", p.getPositionQualityText());
        p.setPositionQuality(3);
        assertEquals("Хорошее", p.getPositionQualityText());
        p.setPositionQuality(7);
        assertEquals("Неизвестно (7)", p.getPositionQualityText());
    }

    @Test
    public void commandAndTelemetryConstants() {
        assertEquals(54, LMashPayload.PAYLOAD_SIZE);
        assertEquals(0, LMashPayload.MSG_TELEMETRY);
        assertEquals(1, LMashPayload.MSG_COMMAND);
        assertEquals(0, LMashPayload.CMD_NONE);
        assertEquals(1, LMashPayload.CMD_CHECK_IN);
        assertEquals(2, LMashPayload.CMD_HOLD);
        assertEquals(3, LMashPayload.CMD_RETURN);
    }

    @Test
    public void equalsAndHashCodeFollowAllFields() {
        LMashPayload a = LMashPayload.fromBytes(unhex(TELEMETRY_VECTOR_HEX));
        LMashPayload b = LMashPayload.fromBytes(unhex(TELEMETRY_VECTOR_HEX));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        b.setPositionQuality(2);
        assertNotEquals(a, b);
        assertNotEquals(a, null);
        assertNotEquals(a, "payload");
    }

    @Test
    public void pressureStringFormat() {
        LMashPayload p = new LMashPayload();
        p.setPressureSys(135);
        p.setPressureDia(85);
        assertEquals("135 / 85", p.getPressureString());
    }

    @Test
    public void toBytesReturnsIndependentArrays() {
        LMashPayload p = new LMashPayload();
        byte[] first = p.toBytes();
        first[0] = 9;
        assertEquals(1, p.toBytes()[0]);
    }
}
