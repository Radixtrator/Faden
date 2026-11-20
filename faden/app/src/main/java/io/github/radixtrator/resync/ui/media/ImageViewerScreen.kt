@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.radixtrator.resync.ui.media

import io.github.radixtrator.resync.ui.common.Symbols
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ContainedLoadingIndicator
import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import io.github.radixtrator.resync.data.MediaDownloads
import io.github.radixtrator.resync.i18n.I18n
import io.github.radixtrator.resync.i18n.t
import io.github.radixtrator.resync.ui.common.AppIcons
import io.github.radixtrator.resync.ui.common.DismissibleMedia
import io.github.radixtrator.resync.ui.common.dragShield
import io.github.radixtrator.resync.ui.navigation.LocalSettings
import io.github.radixtrator.resync.ui.theme.Motion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sync's image viewer, with its control bar (download, host and URL, resolution, file size, then Comments / Open in
 * browser / Copy / Share). Media › "Close images by":
 * - "Single tap": a tap closes it and swiping up slides the bar in from the bottom (a downward fling closes it too,
 *   with "Swipe to close"); with the bar showing, a tap or Back closes the bar first.
 * - "Single tap, hold for the bar": a tap, Back and a fling up or down (with "Swipe to close") close it whether the bar
 *   is showing or not, and holding the picture shows and hides the bar.
 * - "Swiping up or down": a swipe closes it and a tap shows and hides the bar.
 */
@Composable
fun ImageViewerScreen(urls: List<String>, start: Int, postId: String?, downloads: MediaDownloads, onClose: () -> Unit) {
    val pager = rememberPagerState(initialPage = start) { urls.size }
    // Zoomed pages own their drags (panning), so swipe-to-dismiss is off while zoomed. Except when holding for the
    // bar, Back closes the info sheet first.
    val zoomed = remember { mutableStateMapOf<Int, Boolean>() }
    var infoOpen by rememberSaveable { mutableStateOf(false) }
    val download = rememberDownloader(downloads)
    val settings = LocalSettings.current
    val tapCloses = settings.tapToClose
    val holdForBar = tapCloses && settings.holdForBar
    val haptics = LocalHapticFeedback.current
    BackHandler(enabled = infoOpen && !holdForBar) { infoOpen = false }
    DismissibleMedia(
        onDismiss = onClose,
        // With "Single tap", the open bar takes vertical drags. Otherwise a swipe still closes with the bar showing;
        // holding for the bar, except one that starts on the bar (dragShield below).
        enabled = zoomed[pager.currentPage] != true && (!tapCloses || holdForBar || !infoOpen),
        dismissOnDrag = !tapCloses || settings.swipeToClose,
        // Without onSwipeUp, a fling either way closes.
        onSwipeUp = if (tapCloses && !holdForBar) ({ infoOpen = true }) else null,
        // The page it was opened from, blurred.
        backdrop = { BlurredScreenBackdrop() },
    ) {
        Box(Modifier.fillMaxSize()) {
            HorizontalPager(state = pager, beyondViewportPageCount = 1, pageSpacing = 16.dp, modifier = Modifier.fillMaxSize()) { page ->
                ZoomableImage(
                    urls[page],
                    onTap = {
                        when {
                            !tapCloses -> infoOpen = !infoOpen
                            infoOpen && !holdForBar -> infoOpen = false
                            else -> onClose()
                        }
                    },
                    onLongPress = if (holdForBar) {
                        {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            infoOpen = !infoOpen
                        }
                    } else {
                        null
                    },
                    onZoomChange = { zoomed[page] = it },
                )
            }
            if (urls.size > 1) {
                Text(
                    "${pager.currentPage + 1} / ${urls.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(16.dp),
                )
            }
            // Media › "Always show download button": a download button over the image while the bar (which has its own) is closed.
            if (settings.imageDownloadButton && !infoOpen) {
                IconButton(
                    onClick = { download(urls[pager.currentPage]) },
                    modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)
                        .background(MediaPanelColor, CircleShape),
                ) { Icon(AppIcons.Download, t("Download"), tint = Color.White) }
            }
            AnimatedVisibility(
                visible = infoOpen,
                enter = slideInVertically(Motion.enter()) { it },
                exit = slideOutVertically(Motion.exit()) { it },
                modifier = Modifier.align(Alignment.BottomCenter).then(if (holdForBar) Modifier.dragShield() else Modifier),
            ) {
                val url = urls[pager.currentPage]
                ImageInfoSheet(url, postId, downloads, onDownload = { download(url) }, onClose = { infoOpen = false })
            }
        }
    }
}

@Composable
private fun ImageInfoSheet(url: String, postId: String?, downloads: MediaDownloads, onDownload: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    var details by remember(url) { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(url) {
        val cached = cachedImageInfo(context, url)
        val bytes = cached?.second ?: downloads.contentLength(url)
        details = listOfNotNull(
            cached?.first?.let { t("Image resolution: {0}x{1}", it.width, it.height) },
            bytes?.let { t("File size: {0}", String.format(I18n.locale, "%.2fMB", it / 1_048_576.0)) },
        )
    }
    Column(Modifier.fillMaxWidth().background(MediaPanelColor)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onDownload) { Icon(AppIcons.Download, t("Download"), tint = Color.White) }
            IconButton(onClick = onClose) { Icon(Symbols.Close, t("Close"), tint = Color.White) }
        }
        MediaLinkInfo(url, details)
        MediaActionBar(url, postId, downloads, video = false)
    }
}

/** The original's resolution and file size, read from the downloaded file in Coil's disk cache (not from the decoded bitmap). */
private suspend fun cachedImageInfo(context: Context, url: String): Pair<IntSize, Long>? = withContext(Dispatchers.IO) {
    runCatching {
        SingletonImageLoader.get(context).diskCache?.openSnapshot(url)?.use { snapshot ->
            val file = snapshot.data.toFile()
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            if (bounds.outWidth > 0) IntSize(bounds.outWidth, bounds.outHeight) to file.length() else null
        }
    }.getOrNull()
}

/** Zoom past this and the full-resolution original is loaded on top of the screen-sized copy. */
private const val FULL_RES_ZOOM = 2f

/** Pinch/double-tap zoom that leaves single-finger swipes to the pager while not zoomed. */
@Composable
private fun ZoomableImage(url: String, onTap: () -> Unit, onLongPress: (() -> Unit)?, onZoomChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val zoom = remember { ZoomState(onZoomChange) }
    // About twice the screen, enough for 2x zoom to stay sharp; the pager keeps neighbours composed, so full-size
    // bitmaps for three pages would be heavy.
    val request = remember(url) {
        val metrics = context.resources.displayMetrics
        coil3.request.ImageRequest.Builder(context).data(url)
            .size(coil3.size.Size(metrics.widthPixels * 2, metrics.heightPixels * 2)).build()
    }
    // The original is only requested once the user zooms in deep, and then stays; it's drawn over the screen-sized
    // copy, which stays visible while it loads. Kept out of the memory cache so it doesn't evict feed thumbnails.
    var fullRes by remember(url) { mutableStateOf(false) }
    val deep by remember { derivedStateOf { zoom.scale > FULL_RES_ZOOM } }
    if (deep) fullRes = true
    Box(Modifier.fillMaxSize().zoomGestures(zoom, onTap, onLongPress).zoomTransform(zoom)) {
        SubcomposeAsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        ) {
            val loadState by painter.state.collectAsState()
            when (loadState) {
                is AsyncImagePainter.State.Loading, AsyncImagePainter.State.Empty ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { ContainedLoadingIndicator() }
                is AsyncImagePainter.State.Error ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(t("Couldn't load image"), color = Color.White) }
                is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
            }
        }
        if (fullRes) {
            val fullRequest = remember(url) {
                coil3.request.ImageRequest.Builder(context).data(url)
                    .size(coil3.size.Size.ORIGINAL)
                    .memoryCachePolicy(coil3.request.CachePolicy.READ_ONLY)
                    .build()
            }
            AsyncImage(model = fullRequest, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        }
    }
}
