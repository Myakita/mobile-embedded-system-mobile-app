package com.example.mobile_embedded_system.domain;

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
}
