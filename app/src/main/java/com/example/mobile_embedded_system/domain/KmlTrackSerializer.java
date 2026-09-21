package com.example.mobile_embedded_system.domain;

import com.example.mobile_embedded_system.data.local.TelemetryEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Тактический сериализатор сессии в стандартный формат OGC KML 2.2 (ТЗ §4.3, §6.10).
 */
public final class KmlTrackSerializer {

    private KmlTrackSerializer() {
        // Утилитный класс
    }

    public static String serialize(String trackName, List<TelemetryEntity> points) {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        String safeName = escapeXml(trackName != null ? trackName : "Track");

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
        sb.append("  <Document>\n");
        sb.append("    <name>").append(safeName).append("</name>\n");
        sb.append("    <description>Тактический телеметрический трек BMS</description>\n");
        sb.append("    <Style id=\"trackStyle\">\n");
        sb.append("      <LineStyle>\n");
        sb.append("        <color>ff0000ff</color>\n");
        sb.append("        <width>3</width>\n");
        sb.append("      </LineStyle>\n");
        sb.append("    </Style>\n");
        sb.append("    <Placemark>\n");
        sb.append("      <name>").append(safeName).append("</name>\n");
        sb.append("      <styleUrl>#trackStyle</styleUrl>\n");
        sb.append("      <LineString>\n");
        sb.append("        <extrude>1</extrude>\n");
        sb.append("        <tessellate>1</tessellate>\n");
        sb.append("        <coordinates>\n");

        if (points != null && !points.isEmpty()) {
            for (TelemetryEntity pt : points) {
                // В KML стандартная последовательность: longitude,latitude,altitude
                sb.append(String.format(Locale.US, "          %.6f,%.6f,0\n", pt.longitude, pt.latitude));
            }
        }

        sb.append("        </coordinates>\n");
        sb.append("      </LineString>\n");
        sb.append("    </Placemark>\n");
        sb.append("  </Document>\n");
        sb.append("</kml>\n");

        return sb.toString();
    }

    private static String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
