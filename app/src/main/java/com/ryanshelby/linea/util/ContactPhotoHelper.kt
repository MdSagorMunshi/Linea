package com.ryanshelby.linea.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.util.LruCache
import android.media.ExifInterface
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ContactPhotoHelper {

    // 16MB in-memory LRU cache for high-speed scroll and in-call rendering
    private val memoryCache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount
        }
    }

    fun createTempCameraUri(context: Context): Pair<Uri, File> {
        val dir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
        val file = File(dir, "camera_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Pair(uri, file)
    }

    suspend fun saveUriToInternal(context: Context, sourceUri: Uri): Pair<String, ByteArray?> = withContext(Dispatchers.IO) {
        try {
            val bitmap = decodeSampledBitmapFromUri(context, sourceUri, maxDimension = 512) ?: return@withContext Pair("", null)
            val rotatedBitmap = applyExifRotation(context, sourceUri, bitmap)

            val dir = File(context.filesDir, "contact_photos").apply { mkdirs() }
            val file = File(dir, "contact_${System.currentTimeMillis()}.jpg")

            val byteStream = ByteArrayOutputStream()
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 88, byteStream)
            val bytes = byteStream.toByteArray()

            FileOutputStream(file).use { fos ->
                fos.write(bytes)
                fos.flush()
            }

            val fileUri = Uri.fromFile(file).toString()
            memoryCache.put(fileUri, rotatedBitmap)
            Pair(fileUri, bytes)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair("", null)
        }
    }

    suspend fun saveBitmapToInternal(context: Context, bitmap: Bitmap): Pair<String, ByteArray?> = withContext(Dispatchers.IO) {
        try {
            val scaledBitmap = scaleBitmapToMax(bitmap, maxDimension = 512)
            val dir = File(context.filesDir, "contact_photos").apply { mkdirs() }
            val file = File(dir, "contact_${System.currentTimeMillis()}.jpg")

            val byteStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 88, byteStream)
            val bytes = byteStream.toByteArray()

            FileOutputStream(file).use { fos ->
                fos.write(bytes)
                fos.flush()
            }

            val fileUri = Uri.fromFile(file).toString()
            memoryCache.put(fileUri, scaledBitmap)
            Pair(fileUri, bytes)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair("", null)
        }
    }

    suspend fun loadBitmap(context: Context, photoUriString: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (photoUriString.isNullOrBlank()) return@withContext null

        memoryCache.get(photoUriString)?.let { return@withContext it }

        try {
            val uri = Uri.parse(photoUriString)
            var bitmap: Bitmap? = null

            // 1. Try opening via Android ContactsContract high-res photo stream if it's a contact URI
            if (photoUriString.contains("contacts")) {
                try {
                    ContactsContract.Contacts.openContactPhotoInputStream(context.contentResolver, uri, true)?.use { stream ->
                        bitmap = BitmapFactory.decodeStream(stream)
                    }
                } catch (_: Exception) {}
            }

            // 2. Try standard content resolver stream or ImageDecoder
            if (bitmap == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && photoUriString.startsWith("content://")) {
                    try {
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                            val maxDim = 512
                            if (info.size.width > maxDim || info.size.height > maxDim) {
                                val sample = maxOf(info.size.width / maxDim, info.size.height / maxDim)
                                decoder.setTargetSampleSize(sample.coerceAtLeast(1))
                            }
                        }
                    } catch (_: Exception) {}
                }
            }

            // 3. Fallback: stream decoding
            if (bitmap == null) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    bitmap = BitmapFactory.decodeStream(stream, null, options)
                }
            }

            // 4. Fallback: direct file path
            if (bitmap == null && photoUriString.startsWith("file://")) {
                val filePath = uri.path
                if (filePath != null && File(filePath).exists()) {
                    bitmap = BitmapFactory.decodeFile(filePath)
                }
            }

            if (bitmap != null) {
                memoryCache.put(photoUriString, bitmap)
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeSampledBitmapFromUri(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        // Measure bounds first
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        val width = options.outWidth
        val height = options.outHeight
        if (width <= 0 || height <= 0) return null

        var inSampleSize = 1
        if (height > maxDimension || width > maxDimension) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= maxDimension && (halfWidth / inSampleSize) >= maxDimension) {
                inSampleSize *= 2
            }
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    }

    private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (_: Exception) {
            bitmap
        }
    }

    private fun scaleBitmapToMax(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = minOf(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
