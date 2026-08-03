package com.example

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SignalHistoryLogger(private val context: Context) {

    private val logFile: File by lazy {
        File(context.filesDir, "rf_signals_history.csv")
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    init {
        ensureHeaderExists()
    }

    private fun ensureHeaderExists() {
        try {
            if (!logFile.exists() || logFile.length() == 0L) {
                FileWriter(logFile, false).use { writer ->
                    writer.append("TIMESTAMP,DEVICE_NAME,DISTANCE_M,TYPE,FREQ_MHZ,PERIMETER_BREACH\n")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun logSignalEntry(
        deviceName: String,
        distanceMeters: Float,
        type: String,
        freqMhz: Double = 2412.0,
        isBreach: Boolean = false
    ) {
        try {
            ensureHeaderExists()
            val timestamp = dateFormat.format(Date())
            val cleanName = deviceName.replace(",", " ")
            val line = "$timestamp,$cleanName,${String.format(Locale.US, "%.2f", distanceMeters)},$type,${freqMhz.toInt()},$isBreach\n"

            FileWriter(logFile, true).use { writer ->
                writer.append(line)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun readLogTail(lineCount: Int = 15): String {
        return try {
            if (!logFile.exists()) return "Log file empty."
            val lines = logFile.readLines()
            if (lines.isEmpty()) return "Log file empty."
            val tail = lines.takeLast(lineCount)
            tail.joinToString("\n")
        } catch (e: Exception) {
            "Error reading CSV logs: ${e.localizedMessage}"
        }
    }

    fun getAllLogLines(): List<String> {
        return try {
            if (!logFile.exists()) emptyList()
            else logFile.readLines()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getStructuredHistory(limit: Int = 100): List<SignalHistoryItem> {
        val lines = getAllLogLines()
        if (lines.size <= 1) return emptyList()

        return lines.drop(1).takeLast(limit).mapNotNull { line ->
            val parts = line.split(",")
            if (parts.size >= 6) {
                SignalHistoryItem(
                    timestamp = parts[0],
                    deviceName = parts[1],
                    distanceMeters = parts[2].toFloatOrNull() ?: 1.0f,
                    type = parts[3],
                    frequencyMhz = parts[4].toDoubleOrNull() ?: 2412.0,
                    isBreach = parts[5].toBooleanStrictOrNull() ?: false
                )
            } else null
        }.reversed()
    }

    fun clearLog() {
        try {
            if (logFile.exists()) {
                logFile.delete()
            }
            ensureHeaderExists()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCsvFile(): File = logFile
}

data class SignalHistoryItem(
    val timestamp: String,
    val deviceName: String,
    val distanceMeters: Float,
    val type: String,
    val frequencyMhz: Double,
    val isBreach: Boolean
)
