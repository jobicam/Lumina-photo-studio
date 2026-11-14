package com.example.processing

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.SavedImageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageExporter {

    const val ALBUM_DIRECTORY = "Pictures/Lumina"

    suspend fun saveToGallery(
        context: Context,
        bitmap: Bitmap,
        customFilename: String? = null,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 95,
        targetMaxDimension: Int? = null
    ): Result<SavedImageInfo> = withContext(Dispatchers.IO) {
        val finalBitmap = scaleBitmapIfNeeded(bitmap, targetMaxDimension)
        try {

            val extension = if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"
            val mimeType = if (format == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg"

            val rawBaseName = customFilename?.trim()?.takeIf { it.isNotEmpty() } ?: run {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                "Lumina_$timestamp"
            }
            val baseName = rawBaseName.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().ifEmpty {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                "Lumina_$timestamp"
            }
            val filename = if (baseName.endsWith(".$extension", ignoreCase = true)) {
                baseName
            } else {
                "$baseName.$extension"
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis())
                put(MediaStore.Images.Media.DESCRIPTION, "Edited with Lumina Photo Studio")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Lumina")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(Exception("Could not create MediaStore entry in Pictures/Lumina"))

            var bytesWritten = 0L

            val openedStream = resolver.openOutputStream(uri)
            if (openedStream == null) {
                runCatching { resolver.delete(uri, null, null) }
                return@withContext Result.failure(Exception("Unable to open output stream for saved image"))
            }

            openedStream.use { rawStream: OutputStream ->
                val countingStream = CountingOutputStream(rawStream)
                val success = finalBitmap.compress(format, quality, countingStream)
                countingStream.flush()
                bytesWritten = countingStream.bytesWritten

                if (!success) {
                    runCatching { resolver.delete(uri, null, null) }
                    return@withContext Result.failure(Exception("Failed to compress and save image to gallery"))
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            // Trigger MediaScanner for immediate device gallery recognition
            try {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(uri.toString()),
                    arrayOf(mimeType),
                    null
                )
            } catch (ignored: Exception) {
                // Non-fatal if scan fails
            }

            val info = SavedImageInfo(
                uri = uri,
                filename = filename,
                albumPath = ALBUM_DIRECTORY,
                mimeType = mimeType,
                width = finalBitmap.width,
                height = finalBitmap.height,
                fileSizeBytes = bytesWritten,
                savedTimestamp = System.currentTimeMillis()
            )

            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (finalBitmap !== bitmap && !finalBitmap.isRecycled) {
                finalBitmap.recycle()
            }
        }
    }

    fun createViewInGalleryIntent(uri: Uri, mimeType: String): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    suspend fun shareBitmap(context: Context, bitmap: Bitmap): Result<Intent> = withContext(Dispatchers.IO) {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "lumina_share_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            val contentUri: Uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: Exception) {
                Uri.fromFile(file)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Result.success(Intent.createChooser(shareIntent, "Share edited photo"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int?): Bitmap {
        if (maxDimension == null || maxDimension <= 0) return bitmap
        val width = bitmap.width
        val height = bitmap.height
        val maxSide = maxOf(width, height)
        if (maxSide <= maxDimension) return bitmap

        val ratio = maxDimension.toFloat() / maxSide
        val targetWidth = (width * ratio).toInt().coerceAtLeast(1)
        val targetHeight = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private class CountingOutputStream(private val target: OutputStream) : OutputStream() {
        var bytesWritten: Long = 0L
            private set

        override fun write(b: Int) {
            target.write(b)
            bytesWritten++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            target.write(b, off, len)
            bytesWritten += len
        }

        override fun flush() = target.flush()
        override fun close() = target.close()
    }
}
