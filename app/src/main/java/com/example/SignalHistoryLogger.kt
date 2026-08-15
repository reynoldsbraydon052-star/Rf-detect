package com.example

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque

class SignalHistoryLogger(private val context: Context) {

    private val logFile: File by lazy {
        File(context.filesDir, "rf_signals_history.csv")
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val writeChannel = Channel<String>(capacity = 500)

    // Fast in-memory cache to eliminate any disk I/O on UI thread
    private val inMemoryHistory = ConcurrentLinkedDeque<SignalHistoryItem>()
    private val inMemoryLogLines = Collections.synchronizedList(ArrayList<String>())
    private val MAX_IN_MEMORY_ITEMS = 200

    init {
        scope.launch {
            ensureHeaderExists()
            // Drain background write channel in batches
            for (line in writeChannel) {
                try {
                    FileWriter(logFile, true).use { writer ->
                        writer.append(line)
                    }
                } catch (e: Exception) {
                    // Suppress write errors safely
                }
            }
        }
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
        val timestamp = dateFormat.format(Date())
        val cleanName = deviceName.replace(",", " ")
        val line = "$timestamp,$cleanName,${String.format(Locale.US, "%.2f", distanceMeters)},$type,${freqMhz.toInt()},$isBreach\n"

        val item = SignalHistoryItem(
            timestamp = timestamp,
            deviceName = cleanName,
            distanceMeters = distanceMeters,
            type = type,
            frequencyMhz = freqMhz,
            isBreach = isBreach
        )

        inMemoryHistory.addFirst(item)
        while (inMemoryHistory.size > MAX_IN_MEMORY_ITEMS) {
            inMemoryHistory.pollLast()
        }

        synchronized(inMemoryLogLines) {
            inMemoryLogLines.add("$timestamp - $cleanName ($type) [${String.format(Locale.US, "%.1f", distanceMeters)}m]${if (isBreach) " [BREACH]" else ""}")
            if (inMemoryLogLines.size > 50) {
                inMemoryLogLines.removeAt(0)
            }
        }

        // Non-blocking try-send to disk queue
        writeChannel.trySend(line)
    }

    fun readLogTail(lineCount: Int = 15): String {
        return synchronized(inMemoryLogLines) {
            if (inMemoryLogLines.isEmpty()) {
                "Log console active. Listening for RF emitter signals..."
            } else {
                inMemoryLogLines.takeLast(lineCount).joinToString("\n")
            }
        }
    }

    fun getAllLogLines(): List<String> {
        return synchronized(inMemoryLogLines) {
            inMemoryLogLines.toList()
        }
    }

    fun getStructuredHistory(limit: Int = 100): List<SignalHistoryItem> {
        return inMemoryHistory.take(limit)
    }

    fun clearLog() {
        inMemoryHistory.clear()
        synchronized(inMemoryLogLines) {
            inMemoryLogLines.clear()
        }
        scope.launch {
            try {
                if (logFile.exists()) {
                    logFile.delete()
                }
                ensureHeaderExists()
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
