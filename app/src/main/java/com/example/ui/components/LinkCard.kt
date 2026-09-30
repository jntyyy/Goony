package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.ActorEntity
import com.example.data.local.entity.LinkEntity
import com.example.data.local.entity.StudioEntity
import com.example.ui.ActiveInlineVideoPlayback
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalBetaTestPrivacy
import com.example.ui.theme.LocalVaultPalette
import com.example.ui.theme.privacyImageBlur
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class CardActionMenuState {
    CLOSED,
    MAIN_MENU,
    QUALITY_MENU,
    DELETE_CONFIRM,
    ACTORS_MENU
}

@Composable
fun LinkCard(
    link: LinkEntity,
    actorsMap: Map<String, String> = emptyMap(),
    studiosMap: Map<String, String> = emptyMap(),
    fullActorsMap: Map<String, ActorEntity> = emptyMap(),
    isBookmarked: Boolean = false,
    isActiveCard: Boolean = false,
    onActivate: () -> Unit = {},
    onDismissActive: () -> Unit = {},
    onToggleBookmark: () -> Unit = {},
    onPlay: (url: String) -> Unit,
    onOpenGallery: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onActorClick: (actorId: String) -> Unit = {},
    onStudioClick: (studioId: String) -> Unit = {},
    resolvingStatus: String? = null,
    isResolvingThisCard: Boolean = false,
    resolutionError: String? = null,
    onDismissResolutionError: () -> Unit = {},
    inlinePlayback: ActiveInlineVideoPlayback? = null,
    onCloseInlineVideo: () -> Unit = {},
    onFullscreenInlineVideo: (positionMs: Long) -> Unit = {},
    exoPlayer: androidx.media3.exoplayer.ExoPlayer? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current
    val haptic = LocalHapticFeedback.current

    // Internal Submenu state while active
    var subMenuState by remember { mutableStateOf<CardActionMenuState?>(null) }
    var selectedSource by remember { mutableStateOf<Source?>(null) }
    var showAllActorsPopup by remember { mutableStateOf(false) }

    val currentMenuState = when {
        !isActiveCard -> CardActionMenuState.CLOSED
        subMenuState != null -> subMenuState!!
        else -> CardActionMenuState.MAIN_MENU
    }
    val isOverlayActive = currentMenuState != CardActionMenuState.CLOSED

    var lastOpenMenuState by remember { mutableStateOf(CardActionMenuState.MAIN_MENU) }
    if (currentMenuState != CardActionMenuState.CLOSED) {
        lastOpenMenuState = currentMenuState
    }

    val menuProgress by animateFloatAsState(
        targetValue = if (isOverlayActive) 1f else 0f,
        animationSpec = if (isOverlayActive) spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow)
                        else tween(150, easing = FastOutLinearInEasing),
        label = "menu_progress"
    )

    val isOverlayVisible by remember {
        derivedStateOf { menuProgress > 0.001f }
    }

    // Close when dismissed from outside
    LaunchedEffect(isActiveCard) {
        if (!isActiveCard) {
            subMenuState = null
            selectedSource = null
        }
    }

    fun handleCoverTap() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

        if (isOverlayActive) {
            // Dismiss instantly on second click, even if clicked extremely fast!
            subMenuState = null
            onDismissActive()
        } else {
            // Open on first click!
            subMenuState = CardActionMenuState.MAIN_MENU
            onActivate()
        }
    }

    fun handleScrimTap() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        
        if (currentMenuState == CardActionMenuState.ACTORS_MENU) {
            subMenuState = null
            onDismissActive()
        } else if (currentMenuState != CardActionMenuState.MAIN_MENU) {
            subMenuState = CardActionMenuState.MAIN_MENU
        } else {
            // Close instantly when clicking on the scrim background
            subMenuState = null
            onDismissActive()
        }
    }

    // Handle System Back button when overlay is open (returns to MAIN_MENU from submenus, or closes)
    BackHandler(enabled = isOverlayActive) {
        if (currentMenuState == CardActionMenuState.ACTORS_MENU) {
            subMenuState = null
            onDismissActive()
        } else if (currentMenuState != CardActionMenuState.MAIN_MENU && currentMenuState != CardActionMenuState.CLOSED) {
            subMenuState = CardActionMenuState.MAIN_MENU
        } else {
            subMenuState = null
            onDismissActive()
        }
    }

    val imageBlur by animateDpAsState(
        targetValue = if (isOverlayActive) 8.dp else 0.dp,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "cover_blur"
    )

    // O(1) Instant Lookup for Actors and Studio Names (No list iteration inside composition)
    val actorsDisplayName = remember(link.actorIds, actorsMap) {
        if (link.actorIds.isEmpty()) {
            ""
        } else {
            val names = link.actorIds.map { id -> actorsMap[id] ?: id }
            names.joinToString(", ")
        }
    }

    val studioName = remember(link.studioIds, studiosMap) {
        if (link.studioIds.isEmpty()) {
            ""
        } else {
            val firstId = link.studioIds.first()
            studiosMap[firstId] ?: firstId
        }
    }

    // Formatted date
    val displayDate = remember(link.createdAt, link.assignedDate) {
        val ts = link.assignedDate ?: link.createdAt
        formatDisplayDate(ts)
    }

    // Unified URL Handler: Play in App player if video streamable, otherwise launch browser
    fun handleUrlSelection(url: String?) {
        if (!url.isNullOrBlank()) {
            val trimmed = url.trim()
            if (trimmed.endsWith(".mp4", ignoreCase = true) ||
                trimmed.endsWith(".m3u8", ignoreCase = true) ||
                trimmed.endsWith(".mkv", ignoreCase = true) ||
                trimmed.contains("/dash/", ignoreCase = true) ||
                trimmed.contains(".mpd", ignoreCase = true)
            ) {
                onPlay(trimmed)
            } else {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trimmed))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not open URL: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "No URL specified for this quality", Toast.LENGTH_SHORT).show()
        }
    }

    // Magnet handler (Stream / Download in app)
    fun handleMagnet(magnetUri: String?) {
        if (!magnetUri.isNullOrBlank()) {
            onPlay(magnetUri)
        } else {
            Toast.makeText(context, "No Magnet link specified for this quality", Toast.LENGTH_SHORT).show()
        }
    }

    val hasUrlHD = !link.urlHD.isNullOrBlank()
    val hasUrl4K = !link.url4K.isNullOrBlank()
    val hasAnyUrl = hasUrlHD || hasUrl4K

    val hasMagnetHD = !link.magnet.isNullOrBlank()
    val hasMagnet4K = !link.magnet4K.isNullOrBlank()
    val hasAnyMagnet = hasMagnetHD || hasMagnet4K

    val isPhotoset = link.galleryUrls.isNotEmpty() && !hasAnyUrl && !hasAnyMagnet

    // Native Smooth Cover Reveal Animation State
    var isImageLoaded by remember(link.coverImage) { mutableStateOf(false) }

    val coverAlpha by animateFloatAsState(
        targetValue = if (isImageLoaded) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "cover_reveal_alpha"
    )
    val coverScale by animateFloatAsState(
        targetValue = if (isOverlayActive) 1.15f else 1.0f,
        animationSpec = tween(
            durationMillis = 420,
            easing = FastOutSlowInEasing
        ),
        label = "cover_scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("scene_card_${link.id}")
    ) {
        // ========================================================
        // 1. Edge-to-Edge 16:9 Thumbnail or Inline Video Player
        // ========================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(palette.cardBg)
                .clipToBounds()
                .then(
                    if (inlinePlayback == null) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            handleCoverTap()
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            if (inlinePlayback != null) {
                // Embedded 16:9 Native Video Player
                InlineCardPlayer(
                    title = inlinePlayback.title,
                    qualities = inlinePlayback.qualities,
                    subtitles = inlinePlayback.subtitles,
                    defaultHeaders = inlinePlayback.headers,
                    exoPlayer = exoPlayer,
                    onClose = onCloseInlineVideo,
                    onFullscreen = onFullscreenInlineVideo,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Background Image with hardware layer acceleration and native reveal transition
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (imageBlur > 0.dp) {
                                Modifier.blur(imageBlur)
                            } else {
                                Modifier
                            }
                        )
                ) {
                // Native Clean Skeleton Loading Shimmer while image is loading or before it appears
                if (!isImageLoaded && link.coverImage.isNotEmpty()) {
                    val shimmerBrush = ShimmerBrush(targetValue = 900f)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(palette.cardBg)
                            .background(shimmerBrush)
                    )
                }

                val isBetaTest = LocalBetaTestPrivacy.current

                if (link.coverImage.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(link.coverImage)
                            .crossfade(true)
                            .crossfade(280)
                            .build(),
                        contentDescription = link.title,
                        contentScale = ContentScale.Crop,
                        onSuccess = { isImageLoaded = true },
                        onError = { isImageLoaded = true },
                        modifier = Modifier
                            .fillMaxSize()
                            .privacyImageBlur(isBetaTest)
                            .graphicsLayer {
                                alpha = coverAlpha
                                scaleX = coverScale
                                scaleY = coverScale
                            }
                    )
                    if (isBetaTest) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.28f))
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = "No Cover Image",
                            tint = palette.textSecondary.copy(alpha = 0.35f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            // Smooth Scrim Layer with simple fade in/out
            val scrimAlpha by animateFloatAsState(
                targetValue = if (isOverlayActive) 1f else 0f,
                animationSpec = tween(durationMillis = 150),
                label = "scrim_alpha"
            )

            if (scrimAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = scrimAlpha }
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(
                            enabled = isOverlayActive,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            handleScrimTap()
                        }
                )
            }

            // Smooth Native Zoom & Enhanced Bouncy Pop-up transition (Interruption-safe overlay)
            if (isOverlayVisible) {
                CompositionLocalProvider(LocalActionsInteractive provides isOverlayActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .graphicsLayer {
                                val p = menuProgress
                                alpha = (p * 1.5f).coerceIn(0f, 1f)
                                val s = lerp(0.65f, 1f, p)
                                scaleX = s
                                scaleY = s
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = if (isOverlayActive) currentMenuState else lastOpenMenuState,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
                                        scaleIn(
                                            initialScale = 0.65f,
                                            animationSpec = spring(
                                                dampingRatio = 0.65f,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        ))
                                    .togetherWith(
                                        fadeOut(animationSpec = tween(150, easing = FastOutLinearInEasing)) +
                                                scaleOut(
                                                    targetScale = 0.85f,
                                                    animationSpec = spring(
                                                        dampingRatio = 0.65f,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                    )
                                    .using(
                                        SizeTransform(clip = false) { _, _ ->
                                            tween(durationMillis = 180, easing = FastOutSlowInEasing)
                                        }
                                    )
                            },
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth(),
                            label = "center_spread_content"
                        ) { state ->
                            when (state) {
                                CardActionMenuState.CLOSED -> {
                                    Spacer(modifier = Modifier.size(0.dp))
                                }

                                CardActionMenuState.MAIN_MENU -> {
                        MainActionMenu(
                            onMagnetClick = {
                                selectedSource = Source.MAGNET
                                subMenuState = CardActionMenuState.QUALITY_MENU
                            },
                            onUrlClick = {
                                selectedSource = Source.URL
                                subMenuState = CardActionMenuState.QUALITY_MENU
                            },
                            onSave = {
                                onToggleBookmark()
                            },
                            isSaved = isBookmarked,
                            onEdit = {
                                onDismissActive()
                                onEdit()
                            },
                            onDelete = {
                                subMenuState = CardActionMenuState.DELETE_CONFIRM
                            }
                        )
                    }

                    CardActionMenuState.QUALITY_MENU -> {
                        val hasHD = if (selectedSource == Source.MAGNET) {
                            !link.magnet.isNullOrBlank() || !link.torrentUrlHD.isNullOrBlank()
                        } else {
                            !link.urlHD.isNullOrBlank()
                        }
                        val has4K = if (selectedSource == Source.MAGNET) {
                            !link.magnet4K.isNullOrBlank() || !link.torrentUrl4K.isNullOrBlank()
                        } else {
                            !link.url4K.isNullOrBlank()
                        }

                        QualitySelectMenu(
                            hasHD = hasHD,
                            has4K = has4K,
                            onSelectHD = {
                                onDismissActive()
                                if (selectedSource == Source.MAGNET) {
                                    handleMagnet(link.magnet)
                                } else {
                                    handleUrlSelection(link.urlHD)
                                }
                            },
                            onSelect4K = {
                                onDismissActive()
                                if (selectedSource == Source.MAGNET) {
                                    handleMagnet(link.magnet4K)
                                } else {
                                    handleUrlSelection(link.url4K)
                                }
                            }
                        )
                    }

                    CardActionMenuState.DELETE_CONFIRM -> {
                        DeleteConfirmMenu(
                            onCancel = {
                                subMenuState = CardActionMenuState.MAIN_MENU
                            },
                            onConfirm = {
                                onDismissActive()
                                onDelete()
                            }
                        )
                    }

                    CardActionMenuState.ACTORS_MENU -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // The other actors list (excluding the first one)
                            link.actorIds.drop(1).forEach { actorId ->
                                val actorName = actorsMap[actorId] ?: actorId
                                val actorImg = fullActorsMap[actorId]?.imageUrl ?: ""
                                
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .width(72.dp)
                                        .clip(RectangleShape)
                                        .clickable(enabled = isOverlayActive) {
                                            subMenuState = null
                                            onDismissActive()
                                            onActorClick(actorId)
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(palette.surface)
                                            .border(2.dp, accent.copy(alpha = 0.35f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (actorImg.isNotEmpty()) {
                                            AsyncImage(
                                                model = actorImg,
                                                contentDescription = actorName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.AccountCircle,
                                                contentDescription = null,
                                                tint = palette.textMuted,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = actorName,
                                        color = palette.textPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

            // ========================================================
            // Inline Resolution & Progress Overlay (replaces popup dialog)
            // Cover turns into theme background (Dark / Amoled / Light) with real-time status steps
            // ========================================================
            if (isResolvingThisCard && resolvingStatus != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    palette.surface,
                                    palette.bg
                                )
                            )
                        )
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        SmoothProgressIndicator(
                            modifier = Modifier.size(42.dp),
                            color = accent,
                            strokeWidth = 3.5.dp
                        )
                        Text(
                            text = resolvingStatus,
                            color = palette.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else if (isResolvingThisCard && resolutionError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    palette.surface,
                                    palette.cardBg
                                )
                            )
                        )
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = resolutionError,
                            color = palette.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 16.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Button(
                            onClick = onDismissResolutionError,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("OK", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

        // ========================================================
        // 2. Native Material 3 UI Metadata Container
        // ========================================================
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left & Center Column: [Top: Actor | Studio] and [Bottom: Title | Date]
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Row 1: Top-Left (Actor) | Top-Right (Studio)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (link.actorIds.isEmpty()) {
                            Text(
                                text = "Scene",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        } else {
                            val firstActorId = link.actorIds[0]
                            val firstActorName = actorsMap[firstActorId] ?: firstActorId
                            
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = firstActorName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.15.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .clip(RectangleShape)
                                        .clickable {
                                            subMenuState = null
                                            onDismissActive()
                                            onActorClick(firstActorId)
                                        }
                                        .padding(horizontal = 2.dp, vertical = 2.dp)
                                        .weight(1f, fill = false)
                                )
                                
                                if (link.actorIds.size > 1) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                subMenuState = CardActionMenuState.ACTORS_MENU
                                                onActivate()
                                            }
                                            .testTag("more_actors_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "More Actors",
                                            tint = accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        val firstStudioId = link.studioIds.firstOrNull()
                        if (studioName.isNotEmpty()) {
                            Text(
                                text = studioName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Normal,
                                    letterSpacing = 0.2.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .clip(RectangleShape)
                                    .clickable(enabled = firstStudioId != null) {
                                        if (firstStudioId != null) {
                                            subMenuState = null
                                            onDismissActive()
                                            onStudioClick(firstStudioId)
                                        }
                                    }
                                    .padding(horizontal = 2.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Row 2: Bottom-Left (Title) | Bottom-Right (Date)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = link.title,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Normal,
                                lineHeight = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = displayDate,
                            style = MaterialTheme.typography.bodySmall.copy(
                                letterSpacing = 0.25.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

fun formatDisplayDate(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        "Jul 31, 2026"
    }
}
