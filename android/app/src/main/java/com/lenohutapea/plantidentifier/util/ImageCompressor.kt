package com.lenohutapea.plantidentifier.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

/**
 * Shrinks a photo before upload: longest side at most [MAX_SIDE] px, EXIF rotation applied,
 * saved as JPEG. A 12 MP phone photo (~4 MB) becomes roughly 150-300 KB.
 */
object ImageCompressor {
    const val MAX_SIDE = 1024
    private const val QUALITY = 85

    fun compress(context: Context, source: Uri, outDir: File): File {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(source).use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Cannot read this image." }

        val sampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
        val decoded = resolver.openInputStream(source).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
        } ?: throw IllegalArgumentException("Cannot read this image.")

        val rotation = resolver.openInputStream(source).use { stream ->
            if (stream == null) 0 else ExifInterface(stream).rotationDegrees
        }
        val scale = MAX_SIDE.toFloat() / maxOf(decoded.width, decoded.height)
        val matrix = Matrix().apply {
            if (scale < 1f) postScale(scale, scale)
            if (rotation != 0) postRotate(rotation.toFloat())
        }
        val output = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

        outDir.mkdirs()
        val file = File(outDir, "leaf_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { output.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
        if (output !== decoded) decoded.recycle()
        output.recycle()
        return file
    }

    /** Largest power of two that keeps the decoded image at or above [MAX_SIDE] on its longest side. */
    fun sampleSizeFor(width: Int, height: Int): Int {
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= MAX_SIDE) sample *= 2
        return sample
    }

    /** Decode a small bitmap for thumbnails and the result screen. */
    fun decodeThumbnail(path: String, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
