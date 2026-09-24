package io.github.mdshakib007.appwall.ui.common

import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.github.mdshakib007.appwall.Graph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val iconCache = LruCache<String, ImageBitmap>(200)

private fun Drawable.toImageBitmap(px: Int): ImageBitmap = toBitmap(px, px).asImageBitmap()

/** App icon from PackageManager, decoded off the main thread and cached. */
@Composable
fun AppIcon(packageName: String, size: Dp = 44.dp, modifier: Modifier = Modifier) {
    var bitmap by remember(packageName) { mutableStateOf(iconCache.get(packageName)) }
    LaunchedEffect(packageName) {
        if (bitmap == null) {
            val bmp = withContext(Dispatchers.IO) {
                runCatching { Graph.installedApps.icon(packageName)?.toImageBitmap(192) }.getOrNull()
            }
            if (bmp != null) { iconCache.put(packageName, bmp); bitmap = bmp }
        }
    }
    Box(modifier.size(size).clip(CircleShape), contentAlignment = Alignment.Center) {
        val b = bitmap
        if (b != null) {
            Image(bitmap = b, contentDescription = null, modifier = Modifier.size(size))
        } else {
            Box(Modifier.size(size).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape))
        }
    }
}

/** Globe badge for websites. */
@Composable
fun SiteIcon(size: Dp = 44.dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Language, contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(size * 0.55f),
        )
    }
}
