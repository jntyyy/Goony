package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.network.StreamQuality
import com.example.network.SubtitleTrack
import com.example.ui.theme.LocalAccentColor
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val BROWSER_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GoPlayer(
    title: String,
    qualities: List<StreamQuality>,
    subtitles: List<SubtitleTrack> = emptyList(),
    defaultHeaders: Map<String, String> = emptyMap(),
    initialPositionMs: Long = 0L,
    startInLandscape: Boolean = false,
    exoPlayer: ExoPlayer? = null,
    onClose: () -> Unit
) {
    ExoPlayerOverlay(
        title = title,
        qualities = qualities,
        subtitles = subtitles,
        defaultHeaders = defaultHeaders,
        initialPositionMs = initialPositionMs,
        startInLandscape = startInLandscape,
        exoPlayer = exoPlayer,
        onClose = onClose
    )
}

@Composable
fun VerticalSideBarIndicator(
    visible: Boolean,
    percent: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    alignment: Alignment,
    modifier: Modifier = Modifier,
    barHeight: androidx.compose.ui.unit.Dp = 110.dp,
    barWidth: androidx.compose.ui.unit.Dp = 6.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp,
    iconSize: androidx.compose.ui.unit.Dp = 18.dp,
    sidePadding: androidx.compose.ui.unit.Dp = 36.dp
) {
    val displayPercent = percent.coerceIn(0, 100)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(180)),
        exit = fadeOut(animationSpec = tween(250)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = sidePadding, vertical = 16.dp)
                .widthIn(min = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$displayPercent%",
                color = Color.White,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.85f),
                        blurRadius = 8f
                    )
                )
            )

            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(barHeight)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight((displayPercent / 100f).coerceIn(0f, 1f))
                        .clip(CircleShape)
                        .background(LocalAccentColor.current)
                )
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExoPlayerOverlay(
    title: String,
    qualities: List<StreamQuality>,
    subtitles: List<SubtitleTrack> = emptyList(),
    defaultHeaders: Map<String, String> = emptyMap(),
    initialPositionMs: Long = 0L,
    startInLandscape: Boolean = false,
    exoPlayer: ExoPlayer? = null,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val fallbackPlayer = remember(context) {
        if (exoPlayer == null) {
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(10_000, 45_000, 500, 1_000)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
            val renderersFactory = DefaultRenderersFactory(context).setEnableDecoderFallback(true)
            ExoPlayer.Builder(context, renderersFactory)
                .setLoadControl(loadControl)
                .setSeekBackIncrementMs(10_000)
                .setSeekForwardIncrementMs(10_000)
                .build().apply { playWhenReady = true }
        } else null
    }
    val activeExoPlayer = exoPlayer ?: fallbackPlayer!!

    DisposableEffect(fallbackPlayer) {
        onDispose {
            fallbackPlayer?.release()
        }
    }

    // Auto rotate to landscape if requested from inline player transition
    LaunchedEffect(startInLandscape) {
        if (startInLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    var selectedQuality by remember(qualities) {
        mutableStateOf(qualities.firstOrNull { it.isDefault } ?: qualities.firstOrNull())
    }
    var selectedSubtitle by remember(subtitles) { mutableStateOf<SubtitleTrack?>(null) }

    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPos by remember { mutableLongStateOf(initialPositionMs) }
    var duration by remember { mutableLongStateOf(0L) }
    var bufferedPos by remember { mutableLongStateOf(0L) }
    var videoResolution by remember { mutableStateOf("Detecting...") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var errorDetails by remember { mutableStateOf<String?>(null) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    var showBottomSheetMenu by remember { mutableStateOf(false) }
    var bottomSheetTab by remember { mutableStateOf(0) } // 0: Quality, 1: Subtitle, 2: Diagnostics

    val coroutineScope = rememberCoroutineScope()
    var isScrubbing by remember { mutableStateOf(false) }
    var lastSeekTime by remember { mutableLongStateOf(0L) }
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxAudioVolume = remember(audioManager) { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 }

    var gestureVolumePercent by remember { mutableStateOf<Int?>(null) }
    var gestureBrightnessPercent by remember { mutableStateOf<Int?>(null) }
    var volumeHideJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var brightnessHideJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    var isInPipMode by remember { mutableStateOf(activity?.isInPictureInPictureMode == true) }

    DisposableEffect(activity) {
        val compAct = activity as? androidx.activity.ComponentActivity
        if (compAct != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val listener = androidx.core.util.Consumer<androidx.core.app.PictureInPictureModeChangedInfo> { info ->
                isInPipMode = info.isInPictureInPictureMode
            }
            compAct.addOnPictureInPictureModeChangedListener(listener)
            onDispose {
                compAct.removeOnPictureInPictureModeChangedListener(listener)
            }
        } else {
            onDispose { }
        }
    }

    // Helper to handle Back action (Reverts landscape to portrait, or closes player overlay)
    val handleBackAction = {
        if (isLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            onClose()
        }
    }

    BackHandler {
        handleBackAction()
    }

    // Helper to toggle landscape/portrait
    fun toggleOrientation() {
        activity?.let { act ->
            if (isLandscape) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }
    }

    // Auto-hide controls after 4.5 seconds of inactivity
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4500)
            showControls = false
        }
    }

    // Handle system bars and screen orientation cleanup on dispose
    DisposableEffect(activity) {
        activity?.let { act ->
            val windowInsetsController = WindowCompat.getInsetsController(act.window, act.window.decorView)
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            activity?.let { act ->
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                val windowInsetsController = WindowCompat.getInsetsController(act.window, act.window.decorView)
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Back button behavior inside player
    BackHandler {
        if (isLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            onClose()
        }
    }

    val activeHeaders = remember(selectedQuality, defaultHeaders) {
        val streamHeaders = selectedQuality?.headers ?: emptyMap()
        defaultHeaders + streamHeaders
    }

    // Open in external player helper (VLC, MPV, Nova, MX Player, etc.)
    fun openInExternalPlayer() {
        val streamUrl = selectedQuality?.url?.trim() ?: qualities.firstOrNull()?.url?.trim()
        if (!streamUrl.isNullOrBlank()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(streamUrl), "video/*")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(Intent.createChooser(intent, "Play with..."))
            } catch (e: Exception) {
                Toast.makeText(context, "No external player found: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "No video stream URL available", Toast.LENGTH_SHORT).show()
        }
    }

    val exoPlayer = remember(context) {
        // High performance LoadControl: Instant playback startup (500ms), deep buffer for smooth streaming
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 10_000,
                /* maxBufferMs = */ 45_000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)

        ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build().apply {
                playWhenReady = true
            }
    }

    DisposableEffect(activeExoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> isBuffering = true
                    Player.STATE_READY -> {
                        isBuffering = false
                        errorMessage = null
                        errorDetails = null
                        duration = activeExoPlayer.duration.coerceAtLeast(0L)
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        isPlaying = false
                    }
                    Player.STATE_IDLE -> isBuffering = false
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoResolution = "${videoSize.width}x${videoSize.height}"
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                errorMessage = "Playback Error (${error.errorCodeName})"
                errorDetails = error.message ?: "Failed to stream or decode video."
            }
        }

        activeExoPlayer.addListener(listener)
        onDispose {
            activeExoPlayer.removeListener(listener)
        }
    }

    fun enterPipMode() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && activity != null) {
            try {
                showControls = false
                val format = activeExoPlayer.videoFormat
                val rational = if (format != null && format.width > 0 && format.height > 0) {
                    val w = format.width
                    val h = format.height
                    val ratio = w.toFloat() / h.toFloat()
                    if (ratio in 0.42f..2.38f) {
                        android.util.Rational(w, h)
                    } else {
                        android.util.Rational(16, 9)
                    }
                } else {
                    android.util.Rational(16, 9)
                }
                val params = android.app.PictureInPictureParams.Builder()
                    .setAspectRatio(rational)
                    .build()
                activity.enterPictureInPictureMode(params)
            } catch (_: Exception) {
                openInExternalPlayer()
            }
        } else {
            openInExternalPlayer()
        }
    }

    // Periodic time tracker for smooth seekbar updates
    LaunchedEffect(activeExoPlayer, selectedQuality) {
        while (true) {
            if (!isScrubbing && (System.currentTimeMillis() - lastSeekTime > 500L)) {
                if (activeExoPlayer.playbackState == Player.STATE_READY || activeExoPlayer.isPlaying) {
                    currentPos = activeExoPlayer.currentPosition.coerceAtLeast(0L)
                    bufferedPos = activeExoPlayer.bufferedPosition.coerceAtLeast(0L)
                    duration = activeExoPlayer.duration.coerceAtLeast(0L)
                }
            } else {
                duration = activeExoPlayer.duration.coerceAtLeast(0L)
                bufferedPos = activeExoPlayer.bufferedPosition.coerceAtLeast(0L)
            }
            delay(200)
        }
    }

    // Reconfigure media source on quality or subtitle change
    LaunchedEffect(selectedQuality, selectedSubtitle, activeExoPlayer) {
        val quality = selectedQuality ?: return@LaunchedEffect
        val url = quality.url.trim()

        if (url.isBlank()) {
            errorMessage = "Invalid Stream URL"
            errorDetails = "The provided media link is empty."
            return@LaunchedEffect
        }

        val currentUri = activeExoPlayer.currentMediaItem?.localConfiguration?.uri?.toString()
        if (currentUri == url && activeExoPlayer.playbackState != Player.STATE_IDLE) {
            // Stream is already loaded and playing seamlessly!
            errorMessage = null
            errorDetails = null
            isBuffering = activeExoPlayer.playbackState == Player.STATE_BUFFERING
            duration = activeExoPlayer.duration.coerceAtLeast(0L)
            if (!activeExoPlayer.isPlaying && activeExoPlayer.playbackState == Player.STATE_READY) {
                activeExoPlayer.play()
            }
            return@LaunchedEffect
        }

        errorMessage = null
        errorDetails = null
        isBuffering = true

        try {
            val httpFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(activeHeaders["User-Agent"] ?: BROWSER_UA)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(20000)

            val customHeaders = HashMap<String, String>()
            activeHeaders.forEach { (k, v) ->
                if (!k.equals("User-Agent", ignoreCase = true)) {
                    customHeaders[k] = v
                }
            }
            if (customHeaders.isNotEmpty()) {
                httpFactory.setDefaultRequestProperties(customHeaders)
            }

            val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

            val mediaItemBuilder = MediaItem.Builder().setUri(url)

            when {
                url.contains(".mpd", ignoreCase = true) || quality.format.contains("dash", ignoreCase = true) -> {
                    mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
                }
                url.contains(".m3u8", ignoreCase = true) || quality.format.contains("hls", ignoreCase = true) -> {
                    mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
                }
                url.contains(".mkv", ignoreCase = true) || quality.format.contains("mkv", ignoreCase = true) -> {
                    mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MATROSKA)
                }
                url.contains(".webm", ignoreCase = true) -> {
                    mediaItemBuilder.setMimeType(MimeTypes.VIDEO_WEBM)
                }
                url.contains(".ts", ignoreCase = true) -> {
                    mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP2T)
                }
                url.contains(".mp4", ignoreCase = true) || quality.format.contains("mp4", ignoreCase = true) -> {
                    mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP4)
                }
                else -> {
                    // Let ExoPlayer infer automatically if unknown extension
                }
            }

            selectedSubtitle?.let { sub ->
                val subConfig = MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(sub.url))
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage(sub.language)
                    .setLabel(sub.label)
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
                mediaItemBuilder.setSubtitleConfigurations(listOf(subConfig))
            }

            val mediaItem = mediaItemBuilder.build()
            val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)

            val resumePosition = initialPositionMs
            activeExoPlayer.setMediaSource(mediaSource)
            activeExoPlayer.prepare()
            if (resumePosition > 0) {
                activeExoPlayer.seekTo(resumePosition)
            }
            activeExoPlayer.play()
        } catch (e: Exception) {
            errorMessage = "Playback Setup Failed"
            errorDetails = e.message ?: "Could not build media source."
            isBuffering = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(duration) {
                var lastTapTime = 0L
                var lastTapPos = androidx.compose.ui.geometry.Offset.Zero

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPos = down.position
                    val startX = startPos.x
                    val startY = startPos.y
                    val screenWidth = size.width.toFloat().coerceAtLeast(1f)
                    val screenHeight = size.height.toFloat().coerceAtLeast(1f)
                    val isLeftSide = startX < screenWidth * 0.5f

                    val initialVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                    val winAttributes = activity?.window?.attributes
                    val initialBrightness = if ((winAttributes?.screenBrightness ?: -1f) < 0f) 0.5f else winAttributes!!.screenBrightness
                    val initialPosition = activeExoPlayer.currentPosition.coerceAtLeast(0L)

                    var gestureType = 0 // 0: None yet, 1: Seek (horizontal), 2: Volume (vert left), 3: Brightness (vert right)
                    var hasMoved = false
                    var currentSeekTargetMs = initialPosition
                    val pointerId = down.id

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                        if (change.pressed) {
                            val totalDx = change.position.x - startX
                            val totalDy = change.position.y - startY
                            val absDx = kotlin.math.abs(totalDx)
                            val absDy = kotlin.math.abs(totalDy)

                            if (gestureType == 0) {
                                val touchSlop = 10f
                                if (absDx > touchSlop || absDy > touchSlop) {
                                    hasMoved = true
                                    gestureType = if (absDx > absDy) {
                                        1 // Horizontal Seek
                                    } else {
                                        if (isLeftSide) 2 else 3 // 2: Volume, 3: Brightness
                                    }
                                    if (gestureType == 2 || gestureType == 3) {
                                        showControls = false
                                    }
                                }
                            }

                            if (gestureType == 1) {
                                // Horizontal Seek: High Sensitivity
                                change.consume()
                                showControls = true
                                val seekRangeMs = maxOf(duration * 0.45f, 240_000f).toLong().coerceAtLeast(60_000L)
                                val deltaMs = ((totalDx / (screenWidth * 0.45f)) * seekRangeMs).toLong()
                                val targetMs = (initialPosition + deltaMs).coerceIn(0L, duration.coerceAtLeast(1L))
                                currentSeekTargetMs = targetMs
                                currentPos = targetMs
                                lastSeekTime = System.currentTimeMillis()
                            } else if (gestureType == 2) {
                                // Volume Gesture (Left Side: Drag UP increases, DOWN decreases)
                                change.consume()
                                showControls = false
                                val fractionChange = -totalDy / (screenHeight * 0.45f)
                                val newVolume = (initialVolume + (fractionChange * maxAudioVolume)).toInt().coerceIn(0, maxAudioVolume)
                                try {
                                    audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                                } catch (_: Exception) {}
                                val percent = if (maxAudioVolume > 0) ((newVolume.toFloat() / maxAudioVolume) * 100).toInt() else 0
                                gestureVolumePercent = percent
                                volumeHideJob?.cancel()
                            } else if (gestureType == 3) {
                                // Brightness Gesture (Right Side: Drag UP increases, DOWN decreases)
                                change.consume()
                                showControls = false
                                val fractionChange = -totalDy / (screenHeight * 0.45f)
                                val newBrightness = (initialBrightness + fractionChange).coerceIn(0.01f, 1.0f)
                                try {
                                    activity?.window?.let { win ->
                                        val lp = win.attributes
                                        lp.screenBrightness = newBrightness
                                        win.attributes = lp
                                    }
                                } catch (_: Exception) {}
                                val percent = (newBrightness * 100).toInt()
                                gestureBrightnessPercent = percent
                                brightnessHideJob?.cancel()
                            }
                        } else {
                            // Released!
                            if (gestureType == 1) {
                                // Finalize horizontal seek
                                activeExoPlayer.seekTo(currentSeekTargetMs)
                                currentPos = currentSeekTargetMs
                                lastSeekTime = System.currentTimeMillis()
                            } else if (gestureType == 2) {
                                volumeHideJob = coroutineScope.launch {
                                    kotlinx.coroutines.delay(800)
                                    gestureVolumePercent = null
                                }
                            } else if (gestureType == 3) {
                                brightnessHideJob = coroutineScope.launch {
                                    kotlinx.coroutines.delay(800)
                                    gestureBrightnessPercent = null
                                }
                            } else if (gestureType == 0 && !hasMoved) {
                                // TAP or DOUBLE TAP
                                val now = System.currentTimeMillis()
                                val distFromLastTap = (change.position - lastTapPos).getDistance()
                                if (now - lastTapTime < 320L && distFromLastTap < 100f) {
                                    // Double Tap!
                                    if (startX < screenWidth * 0.5f) {
                                        val target = (activeExoPlayer.currentPosition - 10000).coerceAtLeast(0L)
                                        activeExoPlayer.seekTo(target)
                                        currentPos = target
                                        lastSeekTime = System.currentTimeMillis()
                                    } else {
                                        val target = (activeExoPlayer.currentPosition + 10000).coerceAtMost(duration)
                                        activeExoPlayer.seekTo(target)
                                        currentPos = target
                                        lastSeekTime = System.currentTimeMillis()
                                    }
                                    lastTapTime = 0L
                                } else {
                                    // Single tap -> toggle controls
                                    showControls = !showControls
                                    lastTapTime = now
                                    lastTapPos = change.position
                                }
                            }
                            break
                        }
                    }
                }
            }
            .testTag("video_player_overlay")
    ) {
        // 1. AndroidView ExoPlayer Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = activeExoPlayer
                    useController = false
                    keepScreenOn = true
                    this.resizeMode = resizeMode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                if (playerView.player != activeExoPlayer) {
                    playerView.player = activeExoPlayer
                }
                playerView.resizeMode = if (isInPipMode) AspectRatioFrameLayout.RESIZE_MODE_FIT else resizeMode
                playerView.keepScreenOn = true
            },
            onReset = { playerView ->
                playerView.player = null
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isInPipMode) {
            // 2. Center Buffering Indicator
        if (isBuffering && errorMessage == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                SmoothProgressIndicator(
                    modifier = Modifier.size(54.dp),
                    color = LocalAccentColor.current,
                    strokeWidth = 4.dp
                )
            }
        }

        // 3. Error Overlay
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = errorMessage ?: "Playback Error",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = errorDetails ?: "Unable to stream media content.",
                            color = Color(0xFFD1D5DB),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    bottomSheetTab = 2
                                    showBottomSheetMenu = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Info")
                            }

                            Button(
                                onClick = {
                                    errorMessage = null
                                    errorDetails = null
                                    isBuffering = true
                                    activeExoPlayer.prepare()
                                    activeExoPlayer.play()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LocalAccentColor.current),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Retry")
                            }

                            FilledTonalButton(
                                onClick = { openInExternalPlayer() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Text("Open in MPV/VLC", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // Sleek Native Side Vertical Indicators (Volume & Brightness) - Dynamic Padding based on Mode
        val overlaySidePadding = if (isLandscape) 32.dp else 16.dp
        VerticalSideBarIndicator(
            visible = gestureVolumePercent != null,
            percent = gestureVolumePercent ?: 0,
            icon = when {
                (gestureVolumePercent ?: 0) == 0 -> Icons.Default.VolumeOff
                (gestureVolumePercent ?: 0) < 50 -> Icons.Default.VolumeDown
                else -> Icons.Default.VolumeUp
            },
            alignment = Alignment.CenterStart,
            barHeight = if (isLandscape) 160.dp else 130.dp,
            barWidth = 8.dp,
            fontSize = 13.sp,
            iconSize = 22.dp,
            sidePadding = overlaySidePadding,
            modifier = Modifier.align(Alignment.CenterStart)
        )

        VerticalSideBarIndicator(
            visible = gestureBrightnessPercent != null,
            percent = gestureBrightnessPercent ?: 0,
            icon = if ((gestureBrightnessPercent ?: 0) < 50) Icons.Default.BrightnessLow else Icons.Default.BrightnessHigh,
            alignment = Alignment.CenterEnd,
            barHeight = if (isLandscape) 160.dp else 130.dp,
            barWidth = 8.dp,
            fontSize = 13.sp,
            iconSize = 22.dp,
            sidePadding = overlaySidePadding,
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        // 4. Native Overlay Controls (YouTube/Netflix Clean Style)
        AnimatedVisibility(
            visible = showControls && errorMessage == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    // Back Button with enlarged circle on the TOP RIGHT
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White.copy(alpha = 0.20f), CircleShape)
                            .clip(CircleShape)
                            .clickable(onClick = handleBackAction)
                            .testTag("back_player_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Center Clean Media Controls (10s Rewind | Large Play/Pause | 10s Forward)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(40.dp)
                ) {
                    IconButton(
                        onClick = { activeExoPlayer.seekTo((activeExoPlayer.currentPosition - 10000).coerceAtLeast(0L)) },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (activeExoPlayer.isPlaying) {
                                activeExoPlayer.pause()
                            } else {
                                activeExoPlayer.play()
                            }
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    IconButton(
                        onClick = { activeExoPlayer.seekTo((activeExoPlayer.currentPosition + 10000).coerceAtMost(activeExoPlayer.duration)) },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Bottom Timeline & Orientation Actions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    // Time and info row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatDuration(currentPos),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = " / ${formatDuration(duration)}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Pop-Up Window / Picture in Picture Button
                            IconButton(
                                onClick = { enterPipMode() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("popup_player_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PictureInPictureAlt,
                                    contentDescription = "Pop Up Window / Picture in Picture",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // External Player Button (MPV / VLC / System Player)
                            IconButton(
                                onClick = { openInExternalPlayer() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("open_external_player_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.OpenInNew,
                                    contentDescription = "Open in External Player (MPV/VLC)",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Fullscreen / Screen Rotation Toggle Button
                            IconButton(
                                onClick = { toggleOrientation() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLandscape) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                                    contentDescription = "Rotate Screen",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Modern Sleek Video Scrubber (Buffered Bar + Active Progress + Subtle Thumb + Scrub Tooltip)
                    var isScrubbing by remember { mutableStateOf(false) }
                    var scrubProgress by remember { mutableFloatStateOf(0f) }
                    var scrubberWidthPx by remember { mutableFloatStateOf(1f) }

                    val effectiveFraction = if (isScrubbing) scrubProgress else {
                        if (duration > 0) (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
                    }
                    val bufferedFraction = if (duration > 0) (bufferedPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
                    val scrubTimeMs = (effectiveFraction * duration).toLong()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Scrubbing Time Preview Tooltip (Shown when user touches or drags)
                        if (isScrubbing && duration > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .padding(bottom = 6.dp)
                                    .shadow(6.dp, RoundedCornerShape(8.dp))
                            ) {
                                Text(
                                    text = formatDuration(scrubTimeMs),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Custom Interactive Track Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .onSizeChanged { scrubberWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                                .pointerInput(duration) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        isScrubbing = true
                                        scrubProgress = (down.position.x / scrubberWidthPx).coerceIn(0f, 1f)
                                        val initialTarget = (scrubProgress * duration).toLong()
                                        currentPos = initialTarget
                                        lastSeekTime = System.currentTimeMillis()

                                        val pointerId = down.id
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                            if (change.pressed) {
                                                scrubProgress = (change.position.x / scrubberWidthPx).coerceIn(0f, 1f)
                                                val liveTarget = (scrubProgress * duration).toLong()
                                                currentPos = liveTarget
                                                lastSeekTime = System.currentTimeMillis()
                                                change.consume()
                                            } else {
                                                // Released!
                                                val finalTarget = (scrubProgress * duration).toLong()
                                                currentPos = finalTarget
                                                lastSeekTime = System.currentTimeMillis()
                                                exoPlayer.seekTo(finalTarget)
                                                isScrubbing = false
                                                break
                                            }
                                        }
                                    }
                                }
                                .testTag("video_seekbar"),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val trackHeight = if (isScrubbing) 6.dp else 4.dp

                            // Background Track
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(trackHeight)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White.copy(alpha = 0.22f))
                            )

                            // Buffered Stream Progress Track (Light Grey)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = bufferedFraction)
                                    .height(trackHeight)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White.copy(alpha = 0.40f))
                            )

                            // Played Active Progress Track (Vibrant Accent)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = effectiveFraction)
                                    .height(trackHeight)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(LocalAccentColor.current)
                            )

                            // Sleek Scrubbing Thumb (Expands smoothly when interacting)
                            val thumbDiameter = if (isScrubbing) 16.dp else 10.dp
                            val thumbRadiusPx = with(LocalDensity.current) { (thumbDiameter / 2).toPx() }
                            Box(
                                modifier = Modifier
                                    .offset {
                                        androidx.compose.ui.unit.IntOffset(
                                            x = ((scrubberWidthPx * effectiveFraction) - thumbRadiusPx).toInt().coerceIn(0, (scrubberWidthPx - thumbRadiusPx * 2).toInt().coerceAtLeast(0)),
                                            y = 0
                                        )
                                    }
                                    .size(thumbDiameter)
                                    .shadow(4.dp, CircleShape)
                                    .background(Color.White, CircleShape)
                                    .clip(CircleShape)
                            )
                        }
                    }
                }
            }
        }

        // 5. Native Modal Bottom Sheet for Quality, Subtitles & Info (Unifies multiple popups)
        if (showBottomSheetMenu) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheetMenu = false },
                containerColor = Color(0xFF1E1E24),
                contentColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // BottomSheet Tab Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = bottomSheetTab == 0,
                            onClick = { bottomSheetTab = 0 },
                            label = { Text("Quality") },
                            leadingIcon = { Icon(Icons.Outlined.HighQuality, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LocalAccentColor.current,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )

                        if (subtitles.isNotEmpty()) {
                            FilterChip(
                                selected = bottomSheetTab == 1,
                                onClick = { bottomSheetTab = 1 },
                                label = { Text("Subtitles") },
                                leadingIcon = { Icon(Icons.Outlined.Subtitles, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LocalAccentColor.current,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                )
                            )
                        }

                        FilterChip(
                            selected = bottomSheetTab == 2,
                            onClick = { bottomSheetTab = 2 },
                            label = { Text("Diagnostics") },
                            leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LocalAccentColor.current,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    when (bottomSheetTab) {
                        0 -> {
                            // Quality Options
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Stream Quality",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                qualities.forEach { q ->
                                    val isSelected = q == selectedQuality
                                    Surface(
                                        onClick = {
                                            selectedQuality = q
                                            showBottomSheetMenu = false
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) LocalAccentColor.current.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, LocalAccentColor.current) else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${q.quality} (${q.format})",
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) LocalAccentColor.current else Color.White
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = LocalAccentColor.current,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Subtitles Options
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Subtitles / Captions",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Surface(
                                    onClick = {
                                        selectedSubtitle = null
                                        showBottomSheetMenu = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedSubtitle == null) LocalAccentColor.current.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                    border = if (selectedSubtitle == null) androidx.compose.foundation.BorderStroke(1.5.dp, LocalAccentColor.current) else null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Off", color = if (selectedSubtitle == null) LocalAccentColor.current else Color.White)
                                        if (selectedSubtitle == null) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = LocalAccentColor.current, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                subtitles.forEach { sub ->
                                    val isSelected = selectedSubtitle == sub
                                    Surface(
                                        onClick = {
                                            selectedSubtitle = sub
                                            showBottomSheetMenu = false
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) LocalAccentColor.current.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, LocalAccentColor.current) else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(sub.label, color = if (isSelected) LocalAccentColor.current else Color.White)
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = LocalAccentColor.current, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Diagnostics Info
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DiagnosticsItem("Title", title)
                                DiagnosticsItem("Selected Quality", selectedQuality?.quality ?: "Unknown")
                                DiagnosticsItem("Stream Format", selectedQuality?.format ?: "Unknown")
                                DiagnosticsItem("Resolution", videoResolution)
                                DiagnosticsItem("Position", "${formatDuration(currentPos)} / ${formatDuration(duration)}")
                                DiagnosticsItem("Buffered", "${formatDuration(bufferedPos)} (${if (duration > 0) (bufferedPos * 100 / duration) else 0}%)")
                                DiagnosticsItem("Playback State", if (isBuffering) "BUFFERING" else if (isPlaying) "PLAYING" else "PAUSED")
                                DiagnosticsItem("Orientation", if (isLandscape) "Landscape Mode" else "Portrait Mode")
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun DiagnosticsItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF))
        Text(text = value, fontSize = 12.sp, color = Color.White, fontFamily = FontFamily.Monospace)
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

/**
 * Embedded 16:9 Inline Video Player for Card Covers
 * - Displays in 16:9 aspect ratio with square 90-degree corners (no rounded clipping)
 * - Uses the exact same ExoPlayer streaming engine and architecture as the main player
 * - Center 3-button controls (Rewind 10s, Play/Pause, Forward 10s) with double-tap support
 * - Exact same timeline scrubber bar, buffered indicator, active progress bar, and scrub tooltip
 * - Top-right 'X' button to dismiss and revert to cover
 * - External player button (VLC, MX Player, MPV)
 * - Fullscreen button to rotate into landscape fullscreen mode with seamless timestamp resume
 */
@OptIn(UnstableApi::class)
@Composable
fun InlineCardPlayer(
    title: String,
    qualities: List<StreamQuality>,
    subtitles: List<SubtitleTrack> = emptyList(),
    defaultHeaders: Map<String, String> = emptyMap(),
    exoPlayer: ExoPlayer? = null,
    onClose: () -> Unit,
    onFullscreen: (currentPositionMs: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedQuality by remember(qualities) {
        mutableStateOf(qualities.firstOrNull { it.isDefault } ?: qualities.firstOrNull())
    }
    var selectedSubtitle by remember(subtitles) { mutableStateOf<SubtitleTrack?>(null) }

    val fallbackPlayer = remember(context) {
        if (exoPlayer == null) {
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(10_000, 45_000, 500, 1_000)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
            val renderersFactory = DefaultRenderersFactory(context).setEnableDecoderFallback(true)
            ExoPlayer.Builder(context, renderersFactory)
                .setLoadControl(loadControl)
                .setSeekBackIncrementMs(10_000)
                .setSeekForwardIncrementMs(10_000)
                .build().apply { playWhenReady = true }
        } else null
    }
    val activeExoPlayer = exoPlayer ?: fallbackPlayer!!

    DisposableEffect(fallbackPlayer) {
        onDispose {
            fallbackPlayer?.release()
        }
    }

    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPos by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var bufferedPos by remember { mutableLongStateOf(0L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var errorDetails by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    var isScrubbing by remember { mutableStateOf(false) }
    var lastSeekTime by remember { mutableLongStateOf(0L) }
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxAudioVolume = remember(audioManager) { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 }
    val activity = remember(context) { context.findActivity() }

    var gestureVolumePercent by remember { mutableStateOf<Int?>(null) }
    var gestureBrightnessPercent by remember { mutableStateOf<Int?>(null) }
    var volumeHideJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var brightnessHideJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // Auto-hide controls after 4 seconds of playback
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    val activeHeaders = remember(selectedQuality, defaultHeaders) {
        val streamHeaders = selectedQuality?.headers ?: emptyMap()
        defaultHeaders + streamHeaders
    }

    fun openInExternalPlayer() {
        val streamUrl = selectedQuality?.url?.trim() ?: qualities.firstOrNull()?.url?.trim()
        if (!streamUrl.isNullOrBlank()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(streamUrl), "video/*")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(Intent.createChooser(intent, "Play with..."))
            } catch (e: Exception) {
                Toast.makeText(context, "No external player found: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "No video stream URL available", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(activeExoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> isBuffering = true
                    Player.STATE_READY -> {
                        isBuffering = false
                        errorMessage = null
                        errorDetails = null
                        duration = activeExoPlayer.duration.coerceAtLeast(0L)
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        isPlaying = false
                    }
                    Player.STATE_IDLE -> isBuffering = false
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                errorMessage = "Playback Error (${error.errorCodeName})"
                errorDetails = error.message ?: "Failed to stream media content."
            }
        }

        activeExoPlayer.addListener(listener)
        onDispose {
            activeExoPlayer.removeListener(listener)
        }
    }

    // Periodic position updates
    LaunchedEffect(activeExoPlayer, selectedQuality) {
        while (true) {
            if (!isScrubbing && (System.currentTimeMillis() - lastSeekTime > 500L)) {
                if (activeExoPlayer.playbackState == Player.STATE_READY || activeExoPlayer.isPlaying) {
                    currentPos = activeExoPlayer.currentPosition.coerceAtLeast(0L)
                    bufferedPos = activeExoPlayer.bufferedPosition.coerceAtLeast(0L)
                    duration = activeExoPlayer.duration.coerceAtLeast(0L)
                }
            } else {
                duration = activeExoPlayer.duration.coerceAtLeast(0L)
                bufferedPos = activeExoPlayer.bufferedPosition.coerceAtLeast(0L)
            }
            delay(200)
        }
    }

    // Setup and stream media source
    LaunchedEffect(selectedQuality, selectedSubtitle, activeExoPlayer) {
        val quality = selectedQuality ?: return@LaunchedEffect
        val url = quality.url.trim()
        if (url.isBlank()) {
            errorMessage = "Invalid Stream URL"
            errorDetails = "The provided media link is empty."
            return@LaunchedEffect
        }

        val currentUri = activeExoPlayer.currentMediaItem?.localConfiguration?.uri?.toString()
        if (currentUri == url && activeExoPlayer.playbackState != Player.STATE_IDLE) {
            // Stream is already loaded and playing seamlessly!
            errorMessage = null
            errorDetails = null
            isBuffering = activeExoPlayer.playbackState == Player.STATE_BUFFERING
            duration = activeExoPlayer.duration.coerceAtLeast(0L)
            if (!activeExoPlayer.isPlaying && activeExoPlayer.playbackState == Player.STATE_READY) {
                activeExoPlayer.play()
            }
            return@LaunchedEffect
        }

        errorMessage = null
        errorDetails = null
        isBuffering = true

        try {
            val httpFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(activeHeaders["User-Agent"] ?: BROWSER_UA)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(20000)

            val customHeaders = HashMap<String, String>()
            activeHeaders.forEach { (k, v) ->
                if (!k.equals("User-Agent", ignoreCase = true)) {
                    customHeaders[k] = v
                }
            }
            if (customHeaders.isNotEmpty()) {
                httpFactory.setDefaultRequestProperties(customHeaders)
            }

            val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            val mediaItemBuilder = MediaItem.Builder().setUri(url)

            when {
                url.contains(".mpd", ignoreCase = true) || quality.format.contains("dash", ignoreCase = true) -> {
                    mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
                }
                url.contains(".m3u8", ignoreCase = true) || quality.format.contains("hls", ignoreCase = true) -> {
                    mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
                }
                url.contains(".mkv", ignoreCase = true) || quality.format.contains("mkv", ignoreCase = true) -> {
                    mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MATROSKA)
                }
                url.contains(".webm", ignoreCase = true) -> {
                    mediaItemBuilder.setMimeType(MimeTypes.VIDEO_WEBM)
                }
                url.contains(".ts", ignoreCase = true) -> {
                    mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP2T)
                }
                url.contains(".mp4", ignoreCase = true) || quality.format.contains("mp4", ignoreCase = true) -> {
                    mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP4)
                }
                else -> {
                    // Let ExoPlayer infer automatically
                }
            }

            selectedSubtitle?.let { sub ->
                val subConfig = MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(sub.url))
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage(sub.language)
                    .setLabel(sub.label)
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
                mediaItemBuilder.setSubtitleConfigurations(listOf(subConfig))
            }

            val mediaSource = mediaSourceFactory.createMediaSource(mediaItemBuilder.build())
            val resumePosition = currentPos
            activeExoPlayer.setMediaSource(mediaSource)
            activeExoPlayer.prepare()
            if (resumePosition > 0) {
                activeExoPlayer.seekTo(resumePosition)
            }
            activeExoPlayer.play()
        } catch (e: Exception) {
            errorMessage = "Playback Setup Failed"
            errorDetails = e.message ?: "Could not build media source."
            isBuffering = false
        }
    }

    // Square 90-degree corners: NO rounded shape clipping
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(duration) {
                var lastTapTime = 0L
                var lastTapPos = androidx.compose.ui.geometry.Offset.Zero

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPos = down.position
                    val startX = startPos.x
                    val startY = startPos.y
                    val screenWidth = size.width.toFloat().coerceAtLeast(1f)
                    val screenHeight = size.height.toFloat().coerceAtLeast(1f)
                    val isLeftSide = startX < screenWidth * 0.5f

                    val initialVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                    val winAttributes = activity?.window?.attributes
                    val initialBrightness = if ((winAttributes?.screenBrightness ?: -1f) < 0f) 0.5f else winAttributes!!.screenBrightness
                    val initialPosition = activeExoPlayer.currentPosition.coerceAtLeast(0L)

                    var gestureType = 0 // 0: None yet, 1: Seek (horizontal), 2: Volume (vert left), 3: Brightness (vert right)
                    var hasMoved = false
                    var currentSeekTargetMs = initialPosition
                    val pointerId = down.id

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                        if (change.pressed) {
                            val totalDx = change.position.x - startX
                            val totalDy = change.position.y - startY
                            val absDx = kotlin.math.abs(totalDx)
                            val absDy = kotlin.math.abs(totalDy)

                            if (gestureType == 0) {
                                val touchSlop = 12f
                                if (absDx > touchSlop || absDy > 32f) {
                                    hasMoved = true
                                    gestureType = if (absDx > absDy) {
                                        1 // Horizontal Seek
                                    } else if (absDy > absDx * 1.3f) {
                                        if (isLeftSide) 2 else 3 // 2: Volume, 3: Brightness
                                    } else {
                                        0
                                    }
                                    if (gestureType == 2 || gestureType == 3) {
                                        showControls = false
                                    }
                                }
                            }

                            if (gestureType == 1) {
                                // Horizontal Seek: High Sensitivity
                                change.consume()
                                showControls = true
                                val seekRangeMs = maxOf(duration * 0.45f, 240_000f).toLong().coerceAtLeast(60_000L)
                                val deltaMs = ((totalDx / (screenWidth * 0.45f)) * seekRangeMs).toLong()
                                val targetMs = (initialPosition + deltaMs).coerceIn(0L, duration.coerceAtLeast(1L))
                                currentSeekTargetMs = targetMs
                                currentPos = targetMs
                                lastSeekTime = System.currentTimeMillis()
                            } else if (gestureType == 2) {
                                // Volume Gesture
                                change.consume()
                                showControls = false
                                val fractionChange = -totalDy / (screenHeight * 0.45f)
                                val newVolume = (initialVolume + (fractionChange * maxAudioVolume)).toInt().coerceIn(0, maxAudioVolume)
                                try {
                                    audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                                } catch (_: Exception) {}
                                val percent = if (maxAudioVolume > 0) ((newVolume.toFloat() / maxAudioVolume) * 100).toInt() else 0
                                gestureVolumePercent = percent
                                volumeHideJob?.cancel()
                            } else if (gestureType == 3) {
                                // Brightness Gesture
                                change.consume()
                                showControls = false
                                val fractionChange = -totalDy / (screenHeight * 0.45f)
                                val newBrightness = (initialBrightness + fractionChange).coerceIn(0.01f, 1.0f)
                                try {
                                    activity?.window?.let { win ->
                                        val lp = win.attributes
                                        lp.screenBrightness = newBrightness
                                        win.attributes = lp
                                    }
                                } catch (_: Exception) {}
                                val percent = (newBrightness * 100).toInt()
                                gestureBrightnessPercent = percent
                                brightnessHideJob?.cancel()
                            }
                        } else {
                            // Released!
                            if (gestureType == 1) {
                                activeExoPlayer.seekTo(currentSeekTargetMs)
                                currentPos = currentSeekTargetMs
                                lastSeekTime = System.currentTimeMillis()
                            } else if (gestureType == 2) {
                                volumeHideJob = coroutineScope.launch {
                                    kotlinx.coroutines.delay(800)
                                    gestureVolumePercent = null
                                }
                            } else if (gestureType == 3) {
                                brightnessHideJob = coroutineScope.launch {
                                    kotlinx.coroutines.delay(800)
                                    gestureBrightnessPercent = null
                                }
                            } else if (gestureType == 0 && !hasMoved) {
                                val now = System.currentTimeMillis()
                                val distFromLastTap = (change.position - lastTapPos).getDistance()
                                if (now - lastTapTime < 320L && distFromLastTap < 100f) {
                                    if (startX < screenWidth * 0.5f) {
                                        val target = (activeExoPlayer.currentPosition - 10000).coerceAtLeast(0L)
                                        activeExoPlayer.seekTo(target)
                                        currentPos = target
                                        lastSeekTime = System.currentTimeMillis()
                                    } else {
                                        val target = (activeExoPlayer.currentPosition + 10000).coerceAtMost(duration)
                                        activeExoPlayer.seekTo(target)
                                        currentPos = target
                                        lastSeekTime = System.currentTimeMillis()
                                    }
                                    lastTapTime = 0L
                                } else {
                                    showControls = !showControls
                                    lastTapTime = now
                                    lastTapPos = change.position
                                }
                            }
                            break
                        }
                    }
                }
            }
            .testTag("inline_video_player")
    ) {
        // 1. AndroidView ExoPlayer surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = activeExoPlayer
                    useController = false
                    keepScreenOn = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                if (playerView.player != activeExoPlayer) {
                    playerView.player = activeExoPlayer
                }
                playerView.keepScreenOn = true
            },
            onReset = { playerView ->
                playerView.player = null
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Buffering Progress Indicator
        if (isBuffering && errorMessage == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                SmoothProgressIndicator(
                    modifier = Modifier.size(44.dp),
                    color = LocalAccentColor.current,
                    strokeWidth = 3.5.dp
                )
            }
        }

        // 3. Playback Error Indicator
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.90f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(34.dp)
                    )
                    Text(
                        text = errorMessage ?: "Playback Error",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        text = errorDetails ?: "Unable to stream media content.",
                        color = Color(0xFFD1D5DB),
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 2
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                errorMessage = null
                                errorDetails = null
                                isBuffering = true
                                activeExoPlayer.prepare()
                                activeExoPlayer.play()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LocalAccentColor.current),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Retry", fontSize = 12.sp)
                        }
                        FilledTonalButton(
                            onClick = { openInExternalPlayer() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("External Player", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Sleek Native Side Vertical Indicators (Volume & Brightness) - Outer Edge Alignment
        VerticalSideBarIndicator(
            visible = gestureVolumePercent != null,
            percent = gestureVolumePercent ?: 0,
            icon = when {
                (gestureVolumePercent ?: 0) == 0 -> Icons.Default.VolumeOff
                (gestureVolumePercent ?: 0) < 50 -> Icons.Default.VolumeDown
                else -> Icons.Default.VolumeUp
            },
            alignment = Alignment.CenterStart,
            sidePadding = 16.dp,
            modifier = Modifier.align(Alignment.CenterStart)
        )

        VerticalSideBarIndicator(
            visible = gestureBrightnessPercent != null,
            percent = gestureBrightnessPercent ?: 0,
            icon = if ((gestureBrightnessPercent ?: 0) < 50) Icons.Default.BrightnessLow else Icons.Default.BrightnessHigh,
            alignment = Alignment.CenterEnd,
            sidePadding = 16.dp,
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        // 4. Interactive Overlay Controls (Identical to main player)
        AnimatedVisibility(
            visible = showControls && errorMessage == null,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.70f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                // Top-Right Back Button (Enlarged circle, matching Full Screen Back button size 38.dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(38.dp)
                        .background(Color.White.copy(alpha = 0.20f), CircleShape)
                        .clip(CircleShape)
                        .clickable(onClick = onClose)
                        .testTag("back_inline_player"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Center 3-Button Controls (Rewind 10s, Play/Pause, Forward 10s)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { activeExoPlayer.seekTo((activeExoPlayer.currentPosition - 10000).coerceAtLeast(0L)) },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (activeExoPlayer.isPlaying) {
                                activeExoPlayer.pause()
                            } else {
                                activeExoPlayer.play()
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("inline_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    IconButton(
                        onClick = { activeExoPlayer.seekTo((activeExoPlayer.currentPosition + 10000).coerceAtMost(activeExoPlayer.duration)) },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Bottom Timeline & Orientation Actions (Identical to Main Player Scrubber)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    // Time and info row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatDuration(currentPos),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = " / ${formatDuration(duration)}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Pop-Up Window / Picture in Picture Button
                            IconButton(
                                onClick = {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && activity != null) {
                                        try {
                                            showControls = false
                                            val format = activeExoPlayer.videoFormat
                                            val rational = if (format != null && format.width > 0 && format.height > 0) {
                                                val w = format.width
                                                val h = format.height
                                                val ratio = w.toFloat() / h.toFloat()
                                                if (ratio in 0.42f..2.38f) {
                                                    android.util.Rational(w, h)
                                                } else {
                                                    android.util.Rational(16, 9)
                                                }
                                            } else {
                                                android.util.Rational(16, 9)
                                            }
                                            val params = android.app.PictureInPictureParams.Builder()
                                                .setAspectRatio(rational)
                                                .build()
                                            activity.enterPictureInPictureMode(params)
                                        } catch (_: Exception) {
                                            openInExternalPlayer()
                                        }
                                    } else {
                                        openInExternalPlayer()
                                    }
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("popup_inline_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PictureInPictureAlt,
                                    contentDescription = "Pop Up Window / Picture in Picture",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Watch in External Player (VLC / MX Player)
                            IconButton(
                                onClick = { openInExternalPlayer() },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("external_player_inline_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.OpenInNew,
                                    contentDescription = "Watch in External Player",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Fullscreen Button (Rotates to Landscape)
                            IconButton(
                                onClick = { onFullscreen(currentPos) },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("fullscreen_inline_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen Landscape Mode",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Modern Sleek Video Scrubber (Buffered Bar + Active Progress + Subtle Thumb + Scrub Tooltip)
                    var isScrubbing by remember { mutableStateOf(false) }
                    var scrubProgress by remember { mutableFloatStateOf(0f) }
                    var scrubberWidthPx by remember { mutableFloatStateOf(1f) }

                    val effectiveFraction = if (isScrubbing) scrubProgress else {
                        if (duration > 0) (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
                    }
                    val bufferedFraction = if (duration > 0) (bufferedPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
                    val scrubTimeMs = (effectiveFraction * duration).toLong()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Scrubbing Time Preview Tooltip
                        if (isScrubbing && duration > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .padding(bottom = 4.dp)
                                    .shadow(4.dp, RoundedCornerShape(6.dp))
                            ) {
                                Text(
                                    text = formatDuration(scrubTimeMs),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Custom Interactive Track Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .onSizeChanged { scrubberWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                                .pointerInput(duration) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        isScrubbing = true
                                        scrubProgress = (down.position.x / scrubberWidthPx).coerceIn(0f, 1f)
                                        val initialTarget = (scrubProgress * duration).toLong()
                                        currentPos = initialTarget
                                        lastSeekTime = System.currentTimeMillis()

                                        val pointerId = down.id
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                            if (change.pressed) {
                                                scrubProgress = (change.position.x / scrubberWidthPx).coerceIn(0f, 1f)
                                                val liveTarget = (scrubProgress * duration).toLong()
                                                currentPos = liveTarget
                                                lastSeekTime = System.currentTimeMillis()
                                                change.consume()
                                            } else {
                                                // Released!
                                                val finalTarget = (scrubProgress * duration).toLong()
                                                currentPos = finalTarget
                                                lastSeekTime = System.currentTimeMillis()
                                                activeExoPlayer.seekTo(finalTarget)
                                                isScrubbing = false
                                                break
                                            }
                                        }
                                    }
                                }
                                .testTag("video_seekbar_inline"),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val trackHeight = if (isScrubbing) 5.dp else 4.dp

                            // Background Track
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(trackHeight)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.22f))
                            )

                            // Buffered Stream Progress Track (Light Grey)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = bufferedFraction)
                                    .height(trackHeight)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.40f))
                            )

                            // Played Active Progress Track (Vibrant Accent)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = effectiveFraction)
                                    .height(trackHeight)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(LocalAccentColor.current)
                            )

                            // Sleek Scrubbing Thumb
                            val thumbDiameter = if (isScrubbing) 14.dp else 10.dp
                            val thumbRadiusPx = with(LocalDensity.current) { (thumbDiameter / 2).toPx() }
                            Box(
                                modifier = Modifier
                                    .offset {
                                        androidx.compose.ui.unit.IntOffset(
                                            x = ((scrubberWidthPx * effectiveFraction) - thumbRadiusPx).toInt().coerceIn(0, (scrubberWidthPx - thumbRadiusPx * 2).toInt().coerceAtLeast(0)),
                                            y = 0
                                        )
                                    }
                                    .size(thumbDiameter)
                                    .shadow(4.dp, CircleShape)
                                    .background(Color.White, CircleShape)
                                    .clip(CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

