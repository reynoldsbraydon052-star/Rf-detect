package com.example

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class SigintExportRecord(
    val timestamp: Long,
    val emitterId: String,          // Hardware signature or MAC
    val emitterClass: String,       // BLE, WIFI, CELLULAR
    val rssi: Int,
    val filteredDistanceMeters: Double,
    val latitude: Double? = null,   // Optional GNSS
    val longitude: Double? = null,
    val relativeX: Double? = null,  // Dead-reckoned spatial map coordinates
    val relativeY: Double? = null,
    val threatFlags: List<String> = emptyList()
)

object TacticalExportEngine {

    private val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Generates a clean, standard RFC-4180 compliant CSV log with header metadata.
     */
    fun generateCsvLog(records: List<SigintExportRecord>): String {
        val builder = StringBuilder()
        // Header
        builder.append("Timestamp,ISO_Time,Emitter_ID,Emitter_Class,RSSI_dBm,Distance_m,Latitude,Longitude,Relative_X,Relative_Y,Threat_Flags\n")
        
        for (rec in records) {
            val isoTime = isoFormatter.format(rec.timestamp)
            val threatStr = rec.threatFlags.joinToString("|")
            
            val csvLine = listOf(
                rec.timestamp.toString(),
                isoTime,
                escapeCsvField(rec.emitterId),
                escapeCsvField(rec.emitterClass),
                rec.rssi.toString(),
                String.format(Locale.US, "%.2f", rec.filteredDistanceMeters),
                rec.latitude?.toString() ?: "",
                rec.longitude?.toString() ?: "",
                rec.relativeX?.let { String.format(Locale.US, "%.2f", it) } ?: "",
                rec.relativeY?.let { String.format(Locale.US, "%.2f", it) } ?: "",
                escapeCsvField(threatStr)
            ).joinToString(",")
            builder.append(csvLine).append("\n")
        }
        return builder.toString()
    }

    /**
     * Generates valid XML/KML markup for ATAK, WinTAK, or Google Earth mapping.
     */
    fun generateKmlPlacemarks(records: List<SigintExportRecord>, layerName: String = "SIGINT Sweep"): String {
        val builder = StringBuilder()
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        builder.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n")
        builder.append("  <Document>\n")
        builder.append("    <name>${escapeXml(layerName)}</name>\n")
        
        // Define Styles for RSSI thresholds
        builder.append(
            """
            <Style id="sig_high">
              <IconStyle>
                <color>ff00ff00</color> <!-- Green -->
                <scale>1.1</scale>
              </IconStyle>
            </Style>
            <Style id="sig_low">
              <IconStyle>
                <color>ff0000ff</color> <!-- Red -->
                <scale>0.9</scale>
              </IconStyle>
            </Style>
            """.trimIndent()
        ).append("\n")

        for (rec in records) {
            // Coordinate points require valid GNSS coordinates
            val lat = rec.latitude ?: continue
            val lon = rec.longitude ?: continue
            val isoTime = isoFormatter.format(rec.timestamp)
            
            val styleId = if (rec.rssi > -65) "sig_high" else "sig_low"

            builder.append("    <Placemark>\n")
            builder.append("      <name>${escapeXml(rec.emitterId)} [${rec.emitterClass}]</name>\n")
            builder.append("      <description><![CDATA[")
            builder.append("Class: ${rec.emitterClass}<br/>")
            builder.append("RSSI: ${rec.rssi} dBm<br/>")
            builder.append("Est. Distance: ${String.format(Locale.US, "%.1f m", rec.filteredDistanceMeters)}<br/>")
            if (rec.threatFlags.isNotEmpty()) {
                builder.append("Threats: ${rec.threatFlags.joinToString(", ")}<br/>")
            }
            builder.append("]]></description>\n")
            builder.append("      <styleUrl>#$styleId</styleUrl>\n")
            builder.append("      <TimeStamp>\n")
            builder.append("        <when>$isoTime</when>\n")
            builder.append("      </TimeStamp>\n")
            builder.append("      <Point>\n")
            builder.append("        <coordinates>$lon,$lat,0</coordinates>\n")
            builder.append("      </Point>\n")
            builder.append("    </Placemark>\n")
        }
        
        builder.append("  </Document>\n")
        builder.append("</kml>")
        return builder.toString()
    }

    /**
     * Safely saves export payload text using Android 10+ MediaStore (Scoped Storage),
     * avoiding deprecated manifest flags or legacy write permissions.
     */
    fun writeExportToFile(context: Context, filename: String, mimeType: String, content: String): Uri? {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Save under Documents/SIGINT or Downloads/SIGINT subdirectories
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/SIGINT")
            }
        }

        val targetCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI // Fallback
        }

        return try {
            val fileUri = resolver.insert(targetCollection, contentValues) ?: return null
            resolver.openOutputStream(fileUri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            }
            fileUri
        } catch (e: Exception) {
            null
        }
    }

    private fun escapeCsvField(field: String): String {
        if (!field.contains(",") && !field.contains("\"") && !field.contains("\n")) {
            return field
        }
        return "\"" + field.replace("\"", "\"\"") + "\""
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
