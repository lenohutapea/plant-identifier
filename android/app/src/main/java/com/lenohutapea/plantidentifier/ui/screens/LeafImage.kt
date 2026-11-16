package com.lenohutapea.plantidentifier.ui.screens

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
import com.lenohutapea.plantidentifier.util.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads a saved photo off the main thread and shows it (grey box while loading or if missing). */
@Composable
fun LeafImage(path: String, contentDescription: String?, maxSide: Int, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path, maxSide) {
        value = withContext(Dispatchers.IO) {
            ImageCompressor.decodeThumbnail(path, maxSide)?.asImageBitmap()
        }
    }
    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}
