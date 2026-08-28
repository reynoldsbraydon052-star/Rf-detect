package com.example

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

enum class DownloadStatus {
    NOT_INSTALLED,
    QUEUED,
    DOWNLOADING,
    AVAILABLE,
    FAILED,
    CANCELLED
}

data class ModelDownloadInfo(
    val id: String,
    val displayName: String,
    val repository: String,
    val filename: String,
    val url: String,
    val sizeEstimate: String,
    val status: DownloadStatus,
    val progress: Float = 0f, // 0.0 to 1.0
    val bytesDownloaded: Long = 0L,
    val bytesTotal: Long = 0L,
    val downloadId: Long? = null,
    val localPath: String? = null
)

class ModelManager(private val context: Context) {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val prefs = context.getSharedPreferences("sr_model_manager_prefs", Context.MODE_PRIVATE)

    fun getModelFile(model: ModelCatalog.GgufModel): File {
        val dir = File(context.getExternalFilesDir(null), "models")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, model.filename)
    }

    fun isModelInstalled(model: ModelCatalog.GgufModel): Boolean {
        val file = getModelFile(model)
        return file.exists() && file.isFile && file.name.endsWith(".gguf")
    }

    fun getDownloadId(modelId: String): Long? {
        if (!prefs.contains("download_id_$modelId")) return null
        return prefs.getLong("download_id_$modelId", -1L)
    }

    private fun saveDownloadId(modelId: String, downloadId: Long?) {
        if (downloadId == null) {
            prefs.edit().remove("download_id_$modelId").apply()
        } else {
            prefs.edit().putLong("download_id_$modelId", downloadId).apply()
        }
    }

    fun getModelStatus(model: ModelCatalog.GgufModel): ModelDownloadInfo {
        if (isModelInstalled(model)) {
            val file = getModelFile(model)
            return ModelDownloadInfo(
                id = model.id,
                displayName = model.displayName,
                repository = model.repository,
                filename = model.filename,
                url = model.url,
                sizeEstimate = model.sizeEstimate,
                status = DownloadStatus.AVAILABLE,
                progress = 1.0f,
                bytesDownloaded = file.length(),
                bytesTotal = file.length(),
                localPath = file.absolutePath
            )
        }

        val dId = getDownloadId(model.id)
        if (dId == null) {
            return ModelDownloadInfo(
                id = model.id,
                displayName = model.displayName,
                repository = model.repository,
                filename = model.filename,
                url = model.url,
                sizeEstimate = model.sizeEstimate,
                status = DownloadStatus.NOT_INSTALLED
            )
        }

        val query = DownloadManager.Query().setFilterById(dId)
        val cursor = downloadManager.query(query)
        
        if (cursor == null || !cursor.moveToFirst()) {
            cursor?.close()
            return ModelDownloadInfo(
                id = model.id,
                displayName = model.displayName,
                repository = model.repository,
                filename = model.filename,
                url = model.url,
                sizeEstimate = model.sizeEstimate,
                status = DownloadStatus.NOT_INSTALLED
            )
        }

        val statusColIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
        val bytesDownloadedColIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
        val bytesTotalColIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
        
        val dmStatus = if (statusColIdx != -1) cursor.getInt(statusColIdx) else DownloadManager.STATUS_FAILED
        val bytesDownloaded = if (bytesDownloadedColIdx != -1) cursor.getLong(bytesDownloadedColIdx) else 0L
        val bytesTotal = if (bytesTotalColIdx != -1) cursor.getLong(bytesTotalColIdx) else 0L
        
        cursor.close()

        val progress = if (bytesTotal > 0) bytesDownloaded.toFloat() / bytesTotal.toFloat() else 0f

        val status = when (dmStatus) {
            DownloadManager.STATUS_PENDING -> DownloadStatus.QUEUED
            DownloadManager.STATUS_RUNNING -> DownloadStatus.DOWNLOADING
            DownloadManager.STATUS_PAUSED -> DownloadStatus.DOWNLOADING
            DownloadManager.STATUS_SUCCESSFUL -> {
                if (isModelInstalled(model)) DownloadStatus.AVAILABLE else DownloadStatus.FAILED
            }
            DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
            else -> DownloadStatus.FAILED
        }

        return ModelDownloadInfo(
            id = model.id,
            displayName = model.displayName,
            repository = model.repository,
            filename = model.filename,
            url = model.url,
            sizeEstimate = model.sizeEstimate,
            status = status,
            progress = progress,
            bytesDownloaded = bytesDownloaded,
            bytesTotal = bytesTotal,
            downloadId = dId
        )
    }

    fun startDownload(model: ModelCatalog.GgufModel): Boolean {
        if (isModelInstalled(model)) return false
        val existingId = getDownloadId(model.id)
        if (existingId != null) {
            return false // Prevent duplicate active downloads
        }

        val file = getModelFile(model)
        if (file.exists()) {
            file.delete()
        }

        val uri = Uri.parse(model.url)
        val request = DownloadManager.Request(uri)
            .setTitle(model.displayName)
            .setDescription("Downloading GGUF LLM weights...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, null, "models/${model.filename}")

        val downloadId = downloadManager.enqueue(request)
        saveDownloadId(model.id, downloadId)
        return true
    }

    fun cancelDownload(model: ModelCatalog.GgufModel): Boolean {
        val dId = getDownloadId(model.id) ?: return false
        downloadManager.remove(dId)
        saveDownloadId(model.id, null)
        val file = getModelFile(model)
        if (file.exists()) {
            file.delete()
        }
        return true
    }

    fun deleteModel(model: ModelCatalog.GgufModel): Boolean {
        val dId = getDownloadId(model.id)
        if (dId != null) {
            downloadManager.remove(dId)
            saveDownloadId(model.id, null)
        }
        val file = getModelFile(model)
        if (file.exists()) {
            return file.delete()
        }
        return false
    }

    fun getActiveModel(): ModelCatalog.GgufModel? {
        val selectedId = prefs.getString("selected_model_id", null) ?: return null
        return ModelCatalog.MODELS.firstOrNull { it.id == selectedId && isModelInstalled(it) }
    }

    fun setActiveModel(model: ModelCatalog.GgufModel?) {
        prefs.edit().putString("selected_model_id", model?.id).apply()
    }

    fun monitorDownload(model: ModelCatalog.GgufModel): Flow<ModelDownloadInfo> = flow {
        while (true) {
            val info = getModelStatus(model)
            emit(info)
            if (info.status == DownloadStatus.AVAILABLE || 
                info.status == DownloadStatus.FAILED || 
                info.status == DownloadStatus.NOT_INSTALLED || 
                info.status == DownloadStatus.CANCELLED) {
                break
            }
            delay(1000)
        }
    }
}
