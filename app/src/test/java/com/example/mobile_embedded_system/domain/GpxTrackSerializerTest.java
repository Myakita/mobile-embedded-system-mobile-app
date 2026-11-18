package com.example.mobile_embedded_system.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.mobile_embedded_system.data.local.TelemetryEntity;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Модульные тесты генератора тактических GPX-треков (ТЗ §5.1, §6.9).
 */
public class GpxTrackSerializerTest {

    @Test
    public void testEmptyListSerializesValidGpx() {
        String gpx = GpxTrackSerializer.serialize("EMPTY_MISSION", new ArrayList<>());
        assertNotNull(gpx);
        assertTrue(gpx.contains("<trkseg>"));
        assertTrue(gpx.contains("</trkseg>"));
        assertTrue(gpx.contains("EMPTY_MISSION"));
    }

    @Test
    public void testPointsFormattingAndExtensions() {
        List<TelemetryEntity> points = new ArrayList<>();
        TelemetryEntity e = new TelemetryEntity();
        e.latitude = 55.753912;
        e.longitude = 37.620811;
        e.timestamp = 1700000000L;
        e.pulseBpm = 85;
        e.temperatureCelsius = 36.6;
        e.headingDegrees = 42.0;
        points.add(e);

        String gpx = GpxTrackSerializer.serialize("COMBAT_OP_1", points);
        assertTrue(gpx.contains("lat=\"55.753912\""));
        assertTrue(gpx.contains("lon=\"37.620811\""));
        assertTrue(gpx.contains("<pulse>85</pulse>"));
        assertTrue(gpx.contains("<temp>36.6</temp>"));
        assertTrue(gpx.contains("<heading>42</heading>"));
    }

    private static TelemetryEntity pt(double lat, double lon, long ts) {
        TelemetryEntity e = new TelemetryEntity();
        e.latitude = lat;
        e.longitude = lon;
        e.timestamp = ts;
        e.pulseBpm = 70;
        e.temperatureCelsius = 36.6;
        e.headingDegrees = 10;
        return e;
    }

    private static org.w3c.dom.Document parse(String xml) throws Exception {
        javax.xml.parsers.DocumentBuilderFactory f = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        return f.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(xml.getBytes("UTF-8")));
    }

    @Test
    public void nullPointsListGivesEmptyTrack() throws Exception {
        String gpx = GpxTrackSerializer.serialize("T", null);
        assertEquals(0, parse(gpx).getElementsByTagName("trkpt").getLength());
    }

    @Test
    public void outputIsWellFormedGpx11() throws Exception {
        String gpx = GpxTrackSerializer.serialize("T", java.util.Arrays.asList(pt(1, 2, 1700000000L), pt(3, 4, 1700000005L)));
        assertTrue(gpx.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(gpx.contains("version=\"1.1\""));
        assertTrue(gpx.contains("xmlns=\"http://www.topografix.com/GPX/1/1\""));
        assertEquals(2, parse(gpx).getElementsByTagName("trkpt").getLength());
    }

    @Test
    public void pointTimeIsUtcIso8601() {
        String gpx = GpxTrackSerializer.serialize("T", java.util.Collections.singletonList(pt(1, 2, 1700000000L)));
        assertTrue(gpx.contains("<time>2023-11-14T22:13:20Z</time>"));
    }

    @Test
    public void negativeCoordinatesKeepSignAndSixDecimals() {
        String gpx = GpxTrackSerializer.serialize("T", java.util.Collections.singletonList(pt(-33.8688, -70.6483, 1L)));
        assertTrue(gpx.contains("lat=\"-33.868800\" lon=\"-70.648300\""));
    }

    @Test
    public void decimalSeparatorIsDotInAnyDefaultLocale() {
        java.util.Locale old = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(java.util.Locale.GERMANY);
            String gpx = GpxTrackSerializer.serialize("T", java.util.Collections.singletonList(pt(55.5, 37.5, 1L)));
            assertTrue(gpx.contains("lat=\"55.500000\""));
            assertTrue(gpx.contains("<temp>36.6</temp>"));
        } finally {
            java.util.Locale.setDefault(old);
        }
    }

    @Test
    public void trackNameIsEscaped() throws Exception {
        String gpx = GpxTrackSerializer.serialize("A & <B> \"q\" 'a'", null);
        assertTrue(gpx.contains("A &amp; &lt;B&gt; &quot;q&quot; &apos;a&apos;"));
        parse(gpx); // XML остаётся корректным
    }

    @Test
    public void nullNameGivesEmptyNameElement() {
        assertTrue(GpxTrackSerializer.serialize(null, null).contains("<name></name>"));
    }

    @Test
    public void headingIsRoundedToInteger() {
        TelemetryEntity e = pt(1, 2, 1L);
        e.headingDegrees = 359.6;
        assertTrue(GpxTrackSerializer.serialize("T", java.util.Collections.singletonList(e)).contains("<heading>360</heading>"));
        e.headingDegrees = 0.4;
        assertTrue(GpxTrackSerializer.serialize("T", java.util.Collections.singletonList(e)).contains("<heading>0</heading>"));
    }

    @Test
    public void temperatureHasOneDecimal() {
        TelemetryEntity e = pt(1, 2, 1L);
        e.temperatureCelsius = 36.66;
        assertTrue(GpxTrackSerializer.serialize("T", java.util.Collections.singletonList(e)).contains("<temp>36.7</temp>"));
    }

    @Test
    public void pointsKeepInputOrder() {
        String gpx = GpxTrackSerializer.serialize("T", java.util.Arrays.asList(pt(10, 1, 1L), pt(20, 2, 2L), pt(30, 3, 3L)));
        int a = gpx.indexOf("lat=\"10.000000\"");
        int b = gpx.indexOf("lat=\"20.000000\"");
        int c = gpx.indexOf("lat=\"30.000000\"");
        assertTrue(a > 0 && b > a && c > b);
    }

    @Test
    public void emptyTrackContainsNoPoints() {
        assertFalse(GpxTrackSerializer.serialize("T", new java.util.ArrayList<TelemetryEntity>()).contains("<trkpt"));
    }
}
