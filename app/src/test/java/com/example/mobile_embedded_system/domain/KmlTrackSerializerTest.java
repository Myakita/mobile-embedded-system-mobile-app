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
 * Модульные тесты генератора тактических KML-треков (ТЗ §4.3, §6.10).
 */
public class KmlTrackSerializerTest {

    @Test
    public void testEmptyListSerializesValidKml() {
        String kml = KmlTrackSerializer.serialize("EMPTY_MISSION", new ArrayList<>());
        assertNotNull(kml);
        assertTrue(kml.contains("<kml xmlns=\"http://www.opengis.net/kml/2.2\">"));
        assertTrue(kml.contains("<LineString>"));
        assertTrue(kml.contains("<coordinates>"));
        assertTrue(kml.contains("</coordinates>"));
        assertTrue(kml.contains("EMPTY_MISSION"));
    }

    @Test
    public void testPointsCoordinatesFormatting() {
        List<TelemetryEntity> points = new ArrayList<>();
        TelemetryEntity e = new TelemetryEntity();
        e.latitude = 55.753912;
        e.longitude = 37.620811;
        e.timestamp = 1700000000L;
        points.add(e);

        String kml = KmlTrackSerializer.serialize("ALPHA_TRACK", points);
        assertNotNull(kml);
        // В KML порядок: longitude,latitude,altitude
        assertTrue(kml.contains("37.620811,55.753912,0"));
        assertTrue(kml.contains("ALPHA_TRACK"));
        assertTrue(kml.contains("</kml>"));
    }

    @Test
    public void testSpecialCharactersEscaped() {
        String kml = KmlTrackSerializer.serialize("Track & <Mission> \"1\"", new ArrayList<>());
        assertTrue(kml.contains("Track &amp; &lt;Mission&gt; &quot;1&quot;"));
    }

    private static TelemetryEntity pt(double lat, double lon) {
        TelemetryEntity e = new TelemetryEntity();
        e.latitude = lat;
        e.longitude = lon;
        return e;
    }

    private static org.w3c.dom.Document parse(String xml) throws Exception {
        javax.xml.parsers.DocumentBuilderFactory f = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        return f.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(xml.getBytes("UTF-8")));
    }

    @Test
    public void nullNameFallsBackToTrack() {
        String kml = KmlTrackSerializer.serialize(null, null);
        assertTrue(kml.contains("<name>Track</name>"));
    }

    @Test
    public void nullPointsGiveEmptyCoordinates() throws Exception {
        String kml = KmlTrackSerializer.serialize("T", null);
        assertEquals("", parse(kml).getElementsByTagName("coordinates").item(0).getTextContent().trim());
    }

    @Test
    public void outputIsWellFormedKml() throws Exception {
        String kml = KmlTrackSerializer.serialize("T", java.util.Arrays.asList(pt(1, 2), pt(3, 4)));
        assertTrue(kml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        org.w3c.dom.Document doc = parse(kml);
        assertEquals("kml", doc.getDocumentElement().getLocalName());
    }

    @Test
    public void coordinatesAreLonLatAltPerLineInOrder() throws Exception {
        String kml = KmlTrackSerializer.serialize("T", java.util.Arrays.asList(pt(55.5, 37.5), pt(-33.25, -70.75)));
        String[] lines = parse(kml).getElementsByTagName("coordinates").item(0).getTextContent().trim().split("\\s+");
        assertEquals(2, lines.length);
        assertEquals("37.500000,55.500000,0", lines[0]);
        assertEquals("-70.750000,-33.250000,0", lines[1]);
    }

    @Test
    public void decimalSeparatorIsDotInAnyDefaultLocale() {
        java.util.Locale old = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(java.util.Locale.FRANCE);
            assertTrue(KmlTrackSerializer.serialize("T", java.util.Collections.singletonList(pt(55.5, 37.5)))
                    .contains("37.500000,55.500000,0"));
        } finally {
            java.util.Locale.setDefault(old);
        }
    }

    @Test
    public void apostropheAndAmpersandInNameAreEscaped() throws Exception {
        String kml = KmlTrackSerializer.serialize("O'Neil & Co", null);
        assertTrue(kml.contains("O&apos;Neil &amp; Co"));
        parse(kml);
    }

    @Test
    public void emptyTrackHasNoCoordinateLines() {
        String kml = KmlTrackSerializer.serialize("T", new java.util.ArrayList<TelemetryEntity>());
        assertFalse(kml.contains(",0\n"));
    }
}
