package com.example.mobile_embedded_system;

import com.example.mobile_embedded_system.domain.network.MqttTopicBuilder;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MqttTopicBuilderTest {

    @Test
    public void testFormatHexId64BitPreservesHighBits() {
        long large64BitId = 0x123456789ABCDEF0L;
        String formatted = MqttTopicBuilder.formatHexId(large64BitId);
        assertEquals("123456789ABCDEF0", formatted);
    }
}
