package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.example.security.AppUpdateInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateDownloadState {
    object Idle : UpdateDownloadState()
    data class Downloading(val progress: Float, val bytesRead: Long, val totalBytes: Long) : UpdateDownloadState()
    data class ReadyToInstall(val apkFile: File) : UpdateDownloadState()
    data class Error(val message: String) : UpdateDownloadState()
}

/**
 * Manages downloading and over-the-top installation of APK updates.
 * Guarantees smooth upgrades without signature or package conflicts.
 */
class UpdateManager(private val context: Context) {

    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    fun startDownload(updateInfo: AppUpdateInfo) {
        val downloadUrl = updateInfo.downloadUrl.ifBlank {
            "https://github.com/Dhyan2404/pw-dhyan/releases/latest/download/PW-DHYAN.apk"
        }

        _downloadState.value = UpdateDownloadState.Downloading(0f, 0L, 0L)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
                val targetFile = File(updatesDir, "PW-DHYAN-v${updateInfo.latestVersionCode}.apk")
                if (targetFile.exists()) {
                    targetFile.delete()
                }

                val url = URL(downloadUrl)
                var connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 15000
                connection.readTimeout = 30000

                var responseCode = connection.responseCode
                var redirectCount = 0
                while ((responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                            responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                            responseCode == HttpURLConnection.HTTP_SEE_OTHER) && redirectCount < 5
                ) {
                    val newUrl = connection.getHeaderField("Location")
                    connection.disconnect()
                    connection = URL(newUrl).openConnection() as HttpURLConnection
                    connection.connectTimeout = 15000
                    connection.readTimeout = 30000
                    responseCode = connection.responseCode
                    redirectCount++
                }

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("Server returned HTTP $responseCode")
                }

                val totalBytes = connection.contentLengthLong
                var bytesReadTotal = 0L

                connection.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        var lastReportTime = 0L

                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesReadTotal += read

                            val now = System.currentTimeMillis()
                            if (now - lastReportTime > 150L) {
                                lastReportTime = now
                                val progress = if (totalBytes > 0) {
                                    (bytesReadTotal.toFloat() / totalBytes).coerceIn(0f, 1f)
                                } else 0f
                                _downloadState.value = UpdateDownloadState.Downloading(progress, bytesReadTotal, totalBytes)
                            }
                        }
                    }
                }

                _downloadState.value = UpdateDownloadState.ReadyToInstall(targetFile)
                withContext(Dispatchers.Main) {
                    installApk(targetFile)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Download failed: ${e.message}", e)
                _downloadState.value = UpdateDownloadState.Error(e.message ?: "Download failed. Please check network connection.")
            }
        }
    }

    fun installApk(apkFile: File) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer: ${e.message}", e)
            _downloadState.value = UpdateDownloadState.Error("Could not launch package installer: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "UpdateManager"
    }
}
