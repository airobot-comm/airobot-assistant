package com.airobot.features.podcast.service

import android.content.ContentUris
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.airobot.features.podcast.cards.creator.ScannedFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for scanning media files via MediaStore.
 *
 * Provides audio and video file discovery from standard device directories
 * (Music, Download, Audiobooks, Movies, DCIM) for the podcast DIY creator.
 */
interface MediaScannerService {

    /**
     * Scan audio files from Music/, Download/, and Audiobooks/ directories.
     * @return list of discovered audio files with metadata
     */
    suspend fun scanAudioFiles(): List<ScannedFile>

    /**
     * Scan video files from Movies/, DCIM/, and Download/ directories.
     * @return list of discovered video files with metadata
     */
    suspend fun scanVideoFiles(): List<ScannedFile>

    /**
     * Get the display path for a media type's default directory.
     * @param type "audio" or "video"
     * @return human-readable directory path for UI display
     */
    fun getDefaultDirectoryPath(type: String): String
}

@Singleton
class MediaScannerServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MediaScannerService {

    companion object {
        private const val TAG = "MediaScannerService"

        // Audio scan directories
        private val AUDIO_DIRECTORIES = listOf("Music", "Download", "Audiobooks")

        // Video scan directories
        private val VIDEO_DIRECTORIES = listOf("Movies", "DCIM", "Download")
    }

    override suspend fun scanAudioFiles(): List<ScannedFile> = withContext(Dispatchers.IO) {
        Log.d(TAG, "scanAudioFiles: starting audio scan in directories=$AUDIO_DIRECTORIES")
        val results = queryMediaStore(
            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            directories = AUDIO_DIRECTORIES,
            mediaType = "audio"
        )
        Log.d(TAG, "scanAudioFiles: found ${results.size} audio files")
        results
    }

    override suspend fun scanVideoFiles(): List<ScannedFile> = withContext(Dispatchers.IO) {
        Log.d(TAG, "scanVideoFiles: starting video scan in directories=$VIDEO_DIRECTORIES")
        val results = queryMediaStore(
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            directories = VIDEO_DIRECTORIES,
            mediaType = "video"
        )
        Log.d(TAG, "scanVideoFiles: found ${results.size} video files")
        results
    }

    override fun getDefaultDirectoryPath(type: String): String {
        val dirType = when (type) {
            "audio" -> Environment.DIRECTORY_MUSIC
            "video" -> Environment.DIRECTORY_MOVIES
            else -> Environment.DIRECTORY_DOWNLOADS
        }
        return Environment.getExternalStoragePublicDirectory(dirType).absolutePath
    }

    /**
     * Query MediaStore for media files in the specified directories.
     *
     * @param contentUri MediaStore content URI (Audio or Video)
     * @param directories list of relative path prefixes to filter
     * @param mediaType "audio" or "video" for ScannedFile.type
     * @return list of ScannedFile populated from cursor results
     */
    private fun queryMediaStore(
        contentUri: android.net.Uri,
        directories: List<String>,
        mediaType: String
    ): List<ScannedFile> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DURATION,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.RELATIVE_PATH
        )

        // Build OR selection for multiple directories: RELATIVE_PATH LIKE 'Music/%' OR ...
        val selection = directories.joinToString(" OR ") { dir ->
            "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        }
        val selectionArgs = directories.map { dir -> "$dir/%" }.toTypedArray()

        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

        val files = mutableListOf<ScannedFile>()

        context.contentResolver.query(
            contentUri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: continue
                val sizeBytes = cursor.getLong(sizeColumn)
                val durationMs = cursor.getLong(durationColumn)
                val mimeType = cursor.getString(mimeColumn) ?: ""
                val uri = ContentUris.withAppendedId(contentUri, id)

                files.add(
                    ScannedFile(
                        name = name,
                        size = formatFileSize(sizeBytes),
                        type = mediaType,
                        uri = uri.toString(),
                        durationMs = durationMs,
                        mimeType = mimeType,
                        sizeBytes = sizeBytes
                    )
                )
            }
        }

        return files
    }

    /**
     * Format byte size into human-readable string (KB, MB, GB).
     */
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824L -> String.format(Locale.US, "%.1f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576L -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
            bytes >= 1_024L -> String.format(Locale.US, "%.1f KB", bytes / 1_024.0)
            else -> "$bytes B"
        }
    }
}
