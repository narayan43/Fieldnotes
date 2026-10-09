package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object FileUtils {

    private const val IMAGES_DIR = "images"
    private const val EXPORTS_DIR = "exports"

    fun getImagesDirectory(context: Context): File {
        val dir = File(context.filesDir, IMAGES_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getExportsDirectory(context: Context): File {
        val dir = File(context.cacheDir, EXPORTS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Resolves a relative path (e.g. "images/img_123.jpg") to an absolute File in app-private storage.
     */
    fun getFileFromRelativePath(context: Context, relativePath: String): File {
        return File(context.filesDir, relativePath)
    }

    /**
     * Copies an external content URI into app-private internal storage.
     * Returns the relative path to store in Room (e.g. "images/img_UUID.jpg").
     */
    fun saveUriToPrivateStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val imagesDir = getImagesDirectory(context)
            val fileName = "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
            val targetFile = File(imagesDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            "$IMAGES_DIR/$fileName"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Deletes a private internal image file if it exists.
     */
    fun deletePrivateFile(context: Context, relativePath: String): Boolean {
        return try {
            val file = getFileFromRelativePath(context, relativePath)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Loads a downsampled Bitmap safely for thumbnail / PDF embedding to avoid OOM.
     */
    fun decodeSampledBitmap(filePath: String, reqWidth: Int = 800, reqHeight: Int = 800): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)

            var inSampleSize = 1
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            BitmapFactory.decodeFile(filePath, decodeOptions)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
