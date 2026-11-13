package com.vlcsheets.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.vlcsheets.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.vlcsheets.app.data.Song
import com.vlcsheets.app.media.ThumbnailCache
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import com.vlcsheets.app.ui.theme.Brass
import com.vlcsheets.app.ui.theme.BrandGradient
import com.vlcsheets.app.ui.theme.Ink
import com.vlcsheets.app.ui.theme.InkSoft
import com.vlcsheets.app.ui.theme.Paper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Same shapes as the launcher icon (res/drawable/ic_launcher_foreground.xml), 108-unit grid.
private val markPage = PathParser().parsePathString(
    "M39,27 L63,27 L73,37 L73,77 Q73,81 69,81 L39,81 Q35,81 35,77 L35,31 Q35,27 39,27 Z",
).toPath()
private val markFold = PathParser().parsePathString("M63,27 L63,34 Q63,37 66,37 L73,37 Z").toPath()
private val markRibbon = PathParser().parsePathString("M41,24 L47,24 L47,43 L44,40 L41,43 Z").toPath()
private val markNote = PathParser().parsePathString(
    "M56.5,46 L59,46 L59,70 L56.5,70 Z M59,46 C59.5,52 67,53 65.5,62 C64.5,57.5 62,55.5 59,54.5 Z",
).toPath()

/** The VLC Sheets mark: a score page with a folded corner, an eighth note and a brass bookmark. */
@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension * 0.24f
        drawRoundRect(brush = BrandGradient, cornerRadius = CornerRadius(r, r))
        // Show the 72-unit visible area of the adaptive icon (18..90).
        val unit = this.size.minDimension / 72f
        withTransform({
            scale(unit, unit, pivot = Offset.Zero)
            translate(-18f, -18f)
        }) {
            drawPath(markPage, Paper)
            drawPath(markFold, Color(0xFFD9CFB8))
            drawPath(markRibbon, Brass)
            drawPath(markNote, Ink)
            rotate(-20f, pivot = Offset(52f, 70f)) {
                drawOval(Ink, topLeft = Offset(45.5f, 65.4f), size = Size(13f, 9.2f))
            }
        }
    }
}

@Composable
fun SongThumbnail(
    song: Song,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(
        initialValue = ThumbnailCache.get(song.id),
        song.id,
        song.thumbFile,
        song.updatedAt,
    ) {
        if (value == null && song.thumbFile != null) {
            value = withContext(Dispatchers.IO) {
                ThumbnailCache.get(song.id) ?: run {
                    val file = File(
                        File(context.filesDir, "library/songs/${song.id}"),
                        song.thumbFile,
                    )
                    if (file.exists()) {
                        val opts = BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.RGB_565
                        }
                        runCatching { BitmapFactory.decodeFile(file.absolutePath, opts) }.getOrNull()
                            ?.also { ThumbnailCache.put(song.id, it) }
                    } else null
                }
            }
        }
    }

    Box(
        modifier = modifier.clip(RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = song.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Ink, InkSoft),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = song.initials,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Paper,
                )
            }
        }
    }
}

@Composable
fun FavoriteBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(28.dp),
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.38f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = Brass,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
fun MoreButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    // 30 dp circle to look at, 48 dp to touch.
    IconButton(onClick = onClick, modifier = modifier.size(48.dp)) {
        Surface(modifier = Modifier.size(30.dp), shape = CircleShape, color = Color.Black.copy(alpha = 0.34f)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.more_options),
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
fun AnnotationBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(26.dp),
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.38f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.EditNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

@Composable
fun EmptyState(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(88.dp),
        ) {
            Box(contentAlignment = Alignment.Center) { icon() }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text(
                    confirmLabel,
                    color = if (destructive) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(android.R.string.cancel))
            }
        },
    )
}

/**
 * Shrinks the element a little while it is pressed and springs back on release. It only watches
 * the touch (last pass, nothing consumed), so clicks, long presses and scrolling work as before.
 */
fun Modifier.pressScale(pressedScale: Float = 0.96f): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) pressedScale else 1f, spring(stiffness = 900f), label = "press")
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            pressed = true
            // Ends on release, and also when a list starts scrolling under the finger.
            waitForUpOrCancellation(PointerEventPass.Final)
            pressed = false
        }
    }
}
