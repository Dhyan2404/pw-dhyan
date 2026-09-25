package com.example.download

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DownloadedItem(
    val file: File,
    val name: String,
    val sizeFormatted: String,
    val dateFormatted: String,
    val extension: String,
    val lastModified: Long
)

object DownloadedFilesManager {

    /**
     * Lists downloaded files from the user's mobile public Downloads directory.
     * Prioritizes study materials: PDFs, notes, documents, media, and packages.
     */
    fun getDownloadedFiles(): List<DownloadedItem> {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists() || !downloadsDir.isDirectory) {
            return emptyList()
        }

        val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        val files = downloadsDir.listFiles() ?: return emptyList()

        return files
            .filter { it.isFile && !it.name.startsWith(".") && it.length() > 0 }
            .sortedByDescending { it.lastModified() }
            .map { f ->
                val ext = f.extension.lowercase()
                DownloadedItem(
                    file = f,
                    name = f.name,
                    sizeFormatted = formatFileSize(f.length()),
                    dateFormatted = dateFormat.format(Date(f.lastModified())),
                    extension = ext,
                    lastModified = f.lastModified()
                )
            }
    }

    /**
     * Opens a downloaded file using Android's system viewer (PDF reader, video player, etc.)
     * via FileProvider with granted read URI permission.
     */
    fun openFile(context: Context, file: File) {
        try {
            if (!file.exists()) {
                Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
                return
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val mimeType = getMimeType(file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open with..."))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shares a downloaded file using Android's share sheet.
     */
    fun shareFile(context: Context, file: File) {
        try {
            if (!file.exists()) return

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val mimeType = getMimeType(file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share file via..."))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Launches the phone's native Downloads folder or Files app.
     */
    fun openSystemDownloadsFolder(context: Context) {
        try {
            val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    setDataAndType(
                        Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path),
                        "*/*"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(context, "Please open the 'Files' app to view Downloads", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getMimeType(file: File): String {
        val ext = file.extension.lowercase()
        return when (ext) {
            "pdf" -> "application/pdf"
            "mp4", "mkv", "webm" -> "video/*"
            "mp3", "m4a", "wav" -> "audio/*"
            "doc", "docx" -> "application/msword"
            "zip" -> "application/zip"
            "apk" -> "application/vnd.android.package-archive"
            "png", "jpg", "jpeg", "webp" -> "image/*"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
            else -> "$bytes B"
        }
    }
}
