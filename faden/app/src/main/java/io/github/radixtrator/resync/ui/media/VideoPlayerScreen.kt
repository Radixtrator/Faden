@file:kotlin.OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.radixtrator.resync.ui.media

import io.github.radixtrator.resync.ui.common.Symbols
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ContainedLoadingIndicator
import android.content.Context
import android.net.ConnectivityManager
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import io.github.radixtrator.resync.data.MediaDownloads
import io.github.radixtrator.resync.data.ClipHosts
import io.github.radixtrator.resync.data.HostedClip
import io.github.radixtrator.resync.data.VideoQuality
import io.github.radixtrator.resync.i18n.t
import io.github.radixtrator.resync.ui.common.AppIcons
import io.github.radixtrator.resync.ui.common.DismissibleMedia
import io.github.radixtrator.resync.ui.common.LocalFeedDisplay
import io.github.radixtrator.resync.ui.common.PlaybackHandoff
import io.github.radixtrator.resync.ui.common.dataSavingActive
import io.github.radixtrator.resync.ui.common.dragShield
import io.github.radixtrator.resync.ui.navigation.LocalNavigator
import io.github.radixtrator.resync.ui.navigation.LocalSettings
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(UnstableApi::class)
private val SoftwareFirst = MediaCodecSelector { mimeType, secure, tunneling ->
    MediaCodecSelector.DEFAULT.getDecoderInfos(mimeType, secure, tunneling).sortedBy { it.hardwareAccelerated }
}

/** Sync's − 1x + speed steps. */
private val SPEEDS = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

/**
 * Full-screen Media3 player for v.redd.it (HLS), mp4 clips and RedGifs and Streamable pages, with Sync's controls: seek bar,
 * then HD, restart, play/pause, mute, time, download and ⋮. The ⋮ adds playback speed, link info
 * and the Comments / Open in browser / Copy / Share bar. [loop] is for GIF-style clips: they repeat
 * silently and hide the controls until tapped. A RedGifs or Streamable [url] is first looked up for its mp4s
 * ([ClipHosts]); if the site can't be reached, [fallback] (Reddit's own copy) plays instead, when there is one.
 */
@Composable
fun VideoPlayerScreen(url: String, loop: Boolean, postId: String?, downloads: MediaDownloads, onClose: () -> Unit, fallback: String? = null) {
    val hosted = remember(url) { ClipHosts.isHosted(url) }
    if (!hosted) {
        VideoPlayer(url, stream = url, hdStream = null, loop = loop, repeat = loop, postId = postId, downloads = downloads, onClose = onClose)
        return
    }
    var attempt by remember { mutableIntStateOf(0) }
    var resolved by remember { mutableStateOf<HostedClip?>(null) }
    var failure by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(url, attempt) {
        failure = null
        runCatching { ClipHosts.resolve(url) }
            .onSuccess { resolved = it }
            .onFailure { failure = it.message ?: t("Couldn't reach {0}", ClipHosts.name(url)) }
    }
    val video = resolved
    when {
        // Clips with sound play like videos (controls up, sound per "Mute videos"); silent ones like GIFs.
        video != null -> VideoPlayer(
            url, stream = video.sd, hdStream = video.hd.takeIf { it != video.sd }, loop = !video.hasAudio, repeat = video.repeats || loop,
            postId = postId, downloads = downloads, onClose = onClose, userAgent = video.userAgent,
        )
        failure != null && fallback != null ->
            VideoPlayer(url, stream = fallback, hdStream = null, loop = loop, repeat = loop, postId = postId, downloads = downloads, onClose = onClose)
        else -> DismissibleMedia(onDismiss = onClose, dismissOnDrag = LocalSettings.current.swipeToClose) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val message = failure
                if (message == null) {
                    ContainedLoadingIndicator()
                } else {
                    val nav = LocalNavigator.current
                    Column(
                        Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(message, color = Color.White, textAlign = TextAlign.Center)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { attempt++ }) { Text(t("Retry")) }
                            OutlinedButton(onClick = { nav.openExternal(url) }) { Text(t("Open in browser"), color = Color.White) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The player itself. [url] is the page the media came from (for the link info, sharing and the browser);
 * [stream] is what plays, and [hdStream], when given, a higher-quality file the HD button switches to.
 * [loop] mutes and hides the controls, GIF-style; [repeat] plays it over and over. [userAgent] replaces
 * Media3's own for hosts that check it (RedGifs).
 */
@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayer(
    url: String,
    stream: String,
    hdStream: String?,
    loop: Boolean,
    repeat: Boolean,
    postId: String?,
    downloads: MediaDownloads,
    onClose: () -> Unit,
    userAgent: String? = null,
) {
    val context = LocalContext.current
    val nav = LocalNavigator.current
    val settings = LocalSettings.current
    val adaptive = stream.contains(".m3u8")
    // Separate SD and HD files (RedGifs) rather than one adaptive stream.
    val twoFiles = hdStream != null && hdStream != stream
    var error by remember { mutableStateOf<String?>(null) }
    var muted by rememberSaveable { mutableStateOf(settings.muteVideos || loop) }
    // Data saving keeps videos in SD whatever "HD Reddit video quality" says; the HD button still switches.
    val savingData = dataSavingActive()
    var hd by rememberSaveable { mutableStateOf(!savingData && startInHd(context, settings.hdVideo)) }
    var speed by rememberSaveable { mutableFloatStateOf(1f) }
    var controls by rememberSaveable { mutableStateOf(!loop) }
    var panelOpen by rememberSaveable { mutableStateOf(false) }
    var playing by remember { mutableStateOf(true) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var buffering by remember { mutableStateOf(false) }
    // Width / height of the picture, once known; the TextureView is sized to it (PlayerView's job before).
    var aspect by remember { mutableFloatStateOf(0f) }
    // Pinch to zoom and pan the video like an image. Swipe-to-dismiss is off while zoomed, as in the image viewer.
    var zoomed by remember { mutableStateOf(false) }
    val zoom = remember { ZoomState { zoomed = it } }
    val download = rememberDownloader(downloads)

    // Hardware decoders occasionally die mid-stream (Media3's own fallback only covers ones that
    // fail to start), so a decoding failure gets one retry with software decoders first.
    var softwareFirst by remember { mutableStateOf(false) }
    val playingUrl = if (twoFiles && hd) hdStream!! else stream
    // With "Autoplay" on in any view, the clip carries on from its preview, and the preview from here when this closes.
    val handoff = LocalFeedDisplay.current.anyAutoplayActive
    // Where to pick up when the player is rebuilt (switching SD/HD files, or retrying with software decoders).
    val resumeAt = remember { longArrayOf(if (handoff) PlaybackHandoff.position(stream) else 0L) }
    val player = remember(playingUrl, softwareFirst) {
        val renderers = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .apply { if (softwareFirst) setMediaCodecSelector(SoftwareFirst) }
        ExoPlayer.Builder(context, renderers)
            .setMediaSourceFactory(DefaultMediaSourceFactory(VideoCache.dataSourceFactory(context, userAgent)))
            .build().apply {
                val item = MediaItem.Builder()
                    .setUri(playingUrl)
                    .apply { if (adaptive) setMimeType(MimeTypes.APPLICATION_M3U8) }
                    .build()
                setMediaItem(item)
                if (resumeAt[0] > 0) seekTo(resumeAt[0])
                repeatMode = if (repeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                volume = if (muted) 0f else 1f
                playWhenReady = true
                prepare()
            }
    }
    DisposableEffect(player) {
        fun updateAspect(size: VideoSize) {
            if (size.width > 0 && size.height > 0) aspect = size.width * size.pixelWidthHeightRatio / size.height
        }
        updateAspect(player.videoSize)
        fun updateState() {
            playing = player.playWhenReady && player.playbackState != Player.STATE_ENDED
            buffering = player.playWhenReady && player.playbackState == Player.STATE_BUFFERING
            duration = player.duration.coerceAtLeast(0L)
        }
        updateState()
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) = updateAspect(videoSize)

            override fun onPlaybackStateChanged(playbackState: Int) = updateState()

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) = updateState()

            override fun onTimelineChanged(timeline: Timeline, reason: Int) = updateState()

            // Seeks and restarts while paused don't get a poll, so the bar and clock catch up here.
            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                position = player.currentPosition
            }

            override fun onPlayerError(e: PlaybackException) {
                if (e.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED && !softwareFirst) {
                    resumeAt[0] = player.currentPosition
                    softwareFirst = true
                } else {
                    error = t("Can't play this video ({0})", e.errorCodeName)
                }
            }
        }
        player.addListener(listener)
        if (handoff) PlaybackHandoff.attach(stream, player)
        onDispose {
            if (handoff) PlaybackHandoff.detach(stream, player)
            player.removeListener(listener)
            player.release()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { player.pause() }
    LaunchedEffect(player, muted) { player.volume = if (muted) 0f else 1f }
    LaunchedEffect(player, speed) { player.setPlaybackSpeed(speed) }
    LaunchedEffect(player, hd) {
        if (!adaptive) return@LaunchedEffect
        // SD caps HLS at 480p (Reddit's next step up is 720p); HD takes the best the device can play.
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .apply { if (hd) clearVideoSizeConstraints() else setMaxVideoSizeSd() }
            .setForceHighestSupportedBitrate(hd)
            .build()
    }
    // The position only matters on screen: poll it while it's moving, the controls are showing and the app is in front.
    val resumed = LocalLifecycleOwner.current.lifecycle.currentStateAsState().value.isAtLeast(Lifecycle.State.RESUMED)
    val controlsShown = controls || panelOpen
    LaunchedEffect(player, playing, controlsShown, resumed) {
        if (!controlsShown || !resumed) return@LaunchedEffect
        position = player.currentPosition
        while (playing) {
            delay(200)
            position = player.currentPosition
        }
    }
    // Media › "Close videos by": by default a fling up or down closes, a tap shows and hides the controls, and ⋮ opens
    // the panel, which Back and a tap close first. With "Single tap", a tap, Back and a fling up or down (with "Swipe to
    // close") close the player whatever is showing, and holding the video hides and shows the controls (as holding a
    // picture does the image viewer's bar).
    val tapCloses = settings.tapToCloseVideos
    val haptics = LocalHapticFeedback.current
    BackHandler(enabled = panelOpen && !tapCloses) { panelOpen = false }
    DismissibleMedia(
        onDismiss = onClose,
        enabled = (!panelOpen || tapCloses) && !zoomed,
        dismissOnDrag = settings.swipeToClose,
    ) {
        Box(Modifier.fillMaxSize()) {
            // A TextureView rather than PlayerView's default SurfaceView: a SurfaceView ignores Compose's
            // graphicsLayer, so it couldn't be zoomed.
            Box(Modifier.fillMaxSize().zoomTransform(zoom), contentAlignment = Alignment.Center) {
                AndroidView(
                    factory = { ctx -> TextureView(ctx) },
                    // Attached once per player (re-attaching would rebuild the surface); the player is
                    // replaced when retrying with software decoders.
                    update = { view ->
                        if (view.tag !== player) {
                            view.tag = player
                            player.setVideoTextureView(view)
                        }
                    },
                    modifier = if (aspect > 0f) Modifier.aspectRatio(aspect) else Modifier.fillMaxSize(),
                )
            }
            if (buffering && error == null) {
                ContainedLoadingIndicator(modifier = Modifier.align(Alignment.Center))
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .zoomGestures(
                        zoom,
                        onTap = {
                            when {
                                tapCloses -> onClose()
                                panelOpen -> panelOpen = false
                                else -> controls = !controls
                            }
                        },
                        onLongPress = if (tapCloses) {
                            {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (controls || panelOpen) {
                                    controls = false
                                    panelOpen = false
                                } else {
                                    controls = true
                                }
                            }
                        } else {
                            null
                        },
                    ),
            )
            error?.let { message ->
                Column(
                    Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(message, color = Color.White, textAlign = TextAlign.Center)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            error = null
                            player.prepare()
                            player.play()
                        }) { Text(t("Retry")) }
                        OutlinedButton(onClick = { nav.openExternal(url) }) { Text(t("Open in browser"), color = Color.White) }
                    }
                }
            }
            if ((controls || panelOpen) && error == null) {
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        // Closing by a tap, the media takes vertical drags whatever shows, but not ones that start here.
                        .then(if (tapCloses) Modifier.dragShield() else Modifier)
                        .background(if (panelOpen) MediaPanelColor else Color.Transparent),
                ) {
                    if (settings.videoSeekBar) {
                        SeekBar(
                            fraction = { if (duration > 0) position.toFloat() / duration else 0f },
                            onSeek = { player.seekTo((it * duration).toLong()) },
                        )
                    }
                    // Sync packs the playback buttons 38dp apart from the left edge.
                    Row(Modifier.fillMaxWidth().padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (adaptive || twoFiles) HdToggle(hd, onToggle = {
                            resumeAt[0] = player.currentPosition
                            hd = !hd
                        })
                        IconButton(onClick = {
                            player.seekTo(0)
                            player.play()
                        }, modifier = Modifier.size(38.dp)) { Icon(AppIcons.Restart, t("Restart"), tint = Color.White) }
                        IconButton(modifier = Modifier.size(38.dp), onClick = {
                            if (playing) {
                                player.pause()
                            } else {
                                if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
                                player.play()
                            }
                        }) { Icon(if (playing) AppIcons.Pause else Symbols.PlayArrowFilled, if (playing) t("Pause") else t("Play"), tint = Color.White) }
                        IconButton(onClick = { muted = !muted }, modifier = Modifier.size(38.dp)) {
                            Icon(if (muted) AppIcons.VolumeOff else AppIcons.VolumeOn, if (muted) t("Unmute") else t("Mute"), tint = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                        PlaybackClock({ position }, { duration })
                        Spacer(Modifier.weight(1f))
                        // The file itself when there's one to hand (RedGifs' HD mp4), else the link as given.
                        IconButton(onClick = { download(hdStream ?: url) }) { Icon(AppIcons.Download, t("Download"), tint = Color.White) }
                        IconButton(onClick = { panelOpen = !panelOpen }) {
                            if (panelOpen) Icon(Symbols.Close, t("Close"), tint = Color.White) else Icon(Symbols.MoreVert, t("More"), tint = Color.White)
                        }
                    }
                    if (panelOpen) {
                        if (settings.videoSpeedControls) SpeedControl(speed, onChange = { speed = it })
                        MediaLinkInfo(url, emptyList())
                        MediaActionBar(url, postId, downloads, video = true, fileUrl = hdStream ?: url)
                    } else {
                        Spacer(Modifier.navigationBarsPadding())
                    }
                }
            }
        }
    }
}

/** "HD": filled while playing in HD, outlined in SD. */
@Composable
private fun HdToggle(hd: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle, modifier = Modifier.size(38.dp)) {
        Text(
            "HD",
            color = if (hd) Color.Black else Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .then(if (hd) Modifier.background(Color.White) else Modifier.border(1.5.dp, Color.White, RoundedCornerShape(3.dp)))
                .padding(horizontal = 3.dp, vertical = 1.dp),
        )
    }
}

/** "00:05 / 01:30"; reads the position itself, so only this text recomposes as playback moves. */
@Composable
private fun PlaybackClock(position: () -> Long, duration: () -> Long) {
    Text("${clock(position())} / ${clock(duration())}", color = Color.White, style = MaterialTheme.typography.bodySmall)
}

/**
 * Sync's thin seek line with a round thumb; tap or drag to seek. A [Slider], so TalkBack can adjust it. Playback
 * feeds [fraction] into the slider's own state from a snapshot flow rather than composition, so the bar moves
 * by relaying out and redrawing alone, not by recomposing every poll. The seek happens when the touch ends.
 */
@Composable
private fun SeekBar(fraction: () -> Float, onSeek: (Float) -> Unit) {
    val state = remember { SliderState(0f) }
    val currentFraction by rememberUpdatedState(fraction)
    LaunchedEffect(state) {
        snapshotFlow { currentFraction().coerceIn(0f, 1f) }.collect { if (!state.isDragging) state.value = it }
    }
    val label = t("Seek bar")
    // The slider insets its track by half the 12dp thumb, so 10dp padding puts the line's ends where they were (16dp).
    Box(Modifier.fillMaxWidth().height(24.dp).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
        Slider(
            state = state,
            onValueChangeFinished = { onSeek(state.value) },
            modifier = Modifier.semantics { contentDescription = label },
            thumb = { Box(Modifier.size(12.dp).background(Color.White, CircleShape)) },
            track = { sliderState ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .drawBehind {
                            // Read here, so playback moves the bar by redrawing it alone.
                            val x = size.width * sliderState.coercedValueAsFraction
                            val y = size.height / 2
                            drawLine(Color.White.copy(alpha = 0.35f), Offset(0f, y), Offset(size.width, y), 2.dp.toPx(), StrokeCap.Round)
                            drawLine(Color.White, Offset(0f, y), Offset(x, y), 2.dp.toPx(), StrokeCap.Round)
                        },
                )
            },
        )
    }
}

/** Sync's "− 1x +". */
@Composable
private fun SpeedControl(speed: Float, onChange: (Float) -> Unit) {
    val index = SPEEDS.indexOf(speed).coerceAtLeast(0)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(SPEEDS[(index - 1).coerceAtLeast(0)]) }, enabled = index > 0) {
            Icon(AppIcons.RemoveCircleFilled, t("Slower"), tint = Color.White)
        }
        Text(
            speedLabel(speed),
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(56.dp),
        )
        IconButton(onClick = { onChange(SPEEDS[(index + 1).coerceAtMost(SPEEDS.lastIndex)]) }, enabled = index < SPEEDS.lastIndex) {
            Icon(Symbols.AddCircleFilled, t("Faster"), tint = Color.White)
        }
    }
}

private fun speedLabel(speed: Float): String = (if (speed % 1f == 0f) speed.toInt().toString() else speed.toString().trimEnd('0')) + "x"

/** "00:05", or "1:02:03" past an hour. */
internal fun clock(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = total % 3600 / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%02d:%02d", m, s)
}

/** Sync's "HD Reddit video quality": WiFi only means any unmetered connection. */
private fun startInHd(context: Context, quality: VideoQuality): Boolean = when (quality) {
    VideoQuality.ALWAYS -> true
    VideoQuality.NEVER -> false
    VideoQuality.WIFI -> context.getSystemService(ConnectivityManager::class.java)?.isActiveNetworkMetered == false
}
