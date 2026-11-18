package com.example.mobile_embedded_system.domain.network;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MqttTopicBuilderTest {

    @Test
    public void testTelemetryPublishTopic() {
        String topic = MqttTopicBuilder.buildTelemetryPublishTopic(
                "mesh-a",
                "7F10/21A0/0304",
                0x7A831204L,
                0x19AA0221L
        );
        assertEquals("mesh-a/7F10/21A0/0304/telemetry/to/000000007A831204/from/0000000019AA0221", topic);
    }

    @Test
    public void testCommandPublishTopic() {
        String topic = MqttTopicBuilder.buildCommandPublishTopic(
                "mesh-a",
                "7F10/21A0/0304",
                0x19AA0221L,
                0x7A831204L
        );
        assertEquals("mesh-a/7F10/21A0/0304/command/to/0000000019AA0221/from/000000007A831204", topic);
    }

    @Test
    public void testDeviceSubscriptionTopic() {
        String topic = MqttTopicBuilder.buildDeviceSubscriptionTopic(
                "mesh-a",
                "7F10/21A0/0304",
                MqttTopicBuilder.TYPE_COMMAND,
                0x19AA0221L
        );
        assertEquals("mesh-a/7F10/21A0/0304/command/to/0000000019AA0221/#", topic);
    }

    @Test
    public void testSubtreeSubscriptionTopic() {
        String topic = MqttTopicBuilder.buildSubtreeSubscriptionTopic(
                "mesh-a",
                "7F10/21A0"
        );
        assertEquals("mesh-a/7F10/21A0/#", topic);
    }

    @Test
    public void testFull64BitIdFormat() {
        long large64BitId = 0x123456789ABCDEF0L;
        String formatted = MqttTopicBuilder.formatHexId(large64BitId);
        assertEquals("123456789ABCDEF0", formatted);
    }

    @Test
    public void topicTypeConstants() {
        assertEquals("telemetry", MqttTopicBuilder.TYPE_TELEMETRY);
        assertEquals("command", MqttTopicBuilder.TYPE_COMMAND);
    }

    @Test
    public void slashesAndSpacesAroundComponentsAreTrimmed() {
        String topic = MqttTopicBuilder.buildTelemetryPublishTopic(" /mesh-a/ ", "//7F10/21A0//", 1L, 2L);
        assertEquals("mesh-a/7F10/21A0/telemetry/to/0000000000000001/from/0000000000000002", topic);
    }

    @Test
    public void nullNetworkRootIsRejected() {
        try {
            MqttTopicBuilder.buildTelemetryPublishTopic(null, "7F10", 1L, 2L);
            fail("ожидался NullPointerException");
        } catch (NullPointerException expected) {
            assertTrue(expected.getMessage().contains("Topic component"));
        }
    }

    @Test(expected = NullPointerException.class)
    public void nullHierarchyPathIsRejected() {
        MqttTopicBuilder.buildCommandPublishTopic("mesh-a", null, 1L, 2L);
    }

    @Test(expected = NullPointerException.class)
    public void nullPathInSubscriptionIsRejected() {
        MqttTopicBuilder.buildSubtreeSubscriptionTopic("mesh-a", null);
    }

    @Test
    public void hexIdIsFixed16UpperCaseDigits() {
        assertEquals("0000000000000000", MqttTopicBuilder.formatHexId(0L));
        assertEquals("00000000000000FF", MqttTopicBuilder.formatHexId(255L));
    }

    @Test
    public void hexIdOfNegativeLongIsUnsignedRepresentation() {
        assertEquals("FFFFFFFFFFFFFFFF", MqttTopicBuilder.formatHexId(-1L));
        assertEquals("8000000000000000", MqttTopicBuilder.formatHexId(Long.MIN_VALUE));
    }

    @Test
    public void telemetryTopicHasNineSegmentsForThreeLevelPath() {
        String topic = MqttTopicBuilder.buildTelemetryPublishTopic("mesh-a", "7F10/21A0/0304", 0x7A831204L, 0x19AA0221L);
        String[] seg = topic.split("/");
        assertEquals(9, seg.length);
        assertEquals("mesh-a", seg[0]);
        assertEquals("telemetry", seg[4]);
        assertEquals("to", seg[5]);
        assertEquals("from", seg[7]);
    }

    @Test
    public void deviceSubscriptionForTelemetryType() {
        assertEquals("mesh-b/AA/telemetry/to/0000000000000010/#",
                MqttTopicBuilder.buildDeviceSubscriptionTopic("mesh-b", "AA", MqttTopicBuilder.TYPE_TELEMETRY, 0x10L));
    }

    @Test
    public void subtreeSubscriptionTrimsSlashes() {
        assertEquals("mesh-a/7F10/#", MqttTopicBuilder.buildSubtreeSubscriptionTopic("/mesh-a/", "/7F10/"));
    }

    @Test
    public void topicMatchesReadmeExampleForVectorIds() {
        // Идентификаторы векторов протокола: deviceSerial 430572065 = 0x19AA0221, destination 2055410180 = 0x7A831204.
        assertEquals("mesh-a/7F10/21A0/0304/command/to/000000007A831204/from/0000000019AA0221",
                MqttTopicBuilder.buildCommandPublishTopic("mesh-a", "7F10/21A0/0304", 2055410180L, 430572065L));
    }
}
