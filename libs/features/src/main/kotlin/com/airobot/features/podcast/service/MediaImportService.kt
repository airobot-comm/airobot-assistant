package com.airobot.features.podcast.service

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of a successful media file import.
 */
data class ImportResult(
    val internalUri: String,       // File URI in app's private storage
    val durationMs: Long,          // Extracted media duration
    val fileSizeBytes: Long,       // File size in bytes
    val mimeType: String           // MIME type string
)

/**
 * Service for importing (copying) media files to app-private storage.
 *
 * Copies from external content:// URI to internal filesDir/podcast_media/
 * and extracts metadata (duration, mime type) via MediaMetadataRetriever.
 */
interface MediaImportService {

    /**
     * Import a media file from [sourceUri] into app-private storage.
     *
     * @param sourceUri Content URI string from MediaStore
     * @param fileName  Target file name for the imported copy
     * @return [ImportResult] on success, or failure with exception
     */
    suspend fun importFile(sourceUri: String, fileName: String): Result<ImportResult>
}

/**
 * Implementation that copies via ContentResolver streams and extracts
 * metadata via [MediaMetadataRetriever].
 */
@Singleton
class MediaImportServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MediaImportService {

    companion object {
        private const val TAG = "MediaImportService"
        private const val MEDIA_DIR = "podcast_media"
    }

    override suspend fun importFile(
        sourceUri: String,
        fileName: String
    ): Result<ImportResult> = withContext(Dispatchers.IO) {
        Log.d(TAG, "importFile: source=$sourceUri, fileName=$fileName")
        try {
            val uri = Uri.parse(sourceUri)

            // Ensure target directory exists
            val mediaDir = File(context.filesDir, MEDIA_DIR)
            if (!mediaDir.exists()) {
                mediaDir.mkdirs()
                Log.d(TAG, "Created media directory: ${mediaDir.absolutePath}")
            }

            // Generate unique file name to prevent collisions
            val timestamp = System.currentTimeMillis()
            val targetFile = File(mediaDir, "${timestamp}_$fileName")

            // Copy stream
            val resolver = context.contentResolver
            resolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().buffered().use { output ->
                    input.copyTo(output, bufferSize = 8192)
                }
            } ?: return@withContext Result.failure(
                IOException("Failed to open input stream for $sourceUri")
            )

            val fileSizeBytes = targetFile.length()
            Log.d(TAG, "File copied: ${targetFile.absolutePath}, size=$fileSizeBytes bytes")

            // Extract metadata
            val retriever = MediaMetadataRetriever()
            var durationMs = 0L
            var mimeType = ""
            try {
                retriever.setDataSource(targetFile.absolutePath)
                durationMs = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull() ?: 0L
                mimeType = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_MIMETYPE
                ) ?: ""
            } catch (e: Exception) {
                Log.w(TAG, "Metadata extraction partially failed", e)
            } finally {
                retriever.release()
            }

            Log.d(TAG, "Import complete: duration=${durationMs}ms, mime=$mimeType")

            val internalUri = Uri.fromFile(targetFile).toString()
            Result.success(
                ImportResult(
                    internalUri = internalUri,
                    durationMs = durationMs,
                    fileSizeBytes = fileSizeBytes,
                    mimeType = mimeType
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            Result.failure(e)
        }
    }
}
