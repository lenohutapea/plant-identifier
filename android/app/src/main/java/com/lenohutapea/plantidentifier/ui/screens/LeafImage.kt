package com.lenohutapea.plantidentifier.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.lenohutapea.plantidentifier.util.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads a saved photo off the main thread and shows it (tinted box while loading or if missing). */
@Composable
fun LeafImage(path: String, contentDescription: String?, maxSide: Int, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path, maxSide) {
        value = withContext(Dispatchers.IO) {
            ImageCompressor.decodeThumbnail(path, maxSide)?.asImageBitmap()
        }
    }
    ShowBitmap(bitmap, contentDescription, modifier)
}

/** Same as [LeafImage] for a content:// photo that has not been saved yet (camera or gallery). */
@Composable
fun UriImage(uri: Uri, contentDescription: String?, maxSide: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, uri, maxSide) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
                context.contentResolver.openInputStream(uri).use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
                }?.asImageBitmap()
            }.getOrNull()
        }
    }
    ShowBitmap(bitmap, contentDescription, modifier)
}

@Composable
private fun ShowBitmap(bitmap: ImageBitmap?, contentDescription: String?, modifier: Modifier) {
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = contentDescription, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}
