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
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.ActorEntity
import com.example.data.local.entity.LinkEntity
import com.example.data.local.entity.StudioEntity
import com.example.ui.ActiveInlineVideoPlayback
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalVaultPalette
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlinx.coroutines.delay

private enum class CardActionMenuState {
    CLOSED,
    MAIN_MENU,   // Action Buttons: Delete (Red), Edit (Soft Sand), Bookmark (Pink Bookmark), URL (Blue), Magnet (Emerald Green)
    URL_SUBMENU, // Back, HD (Blue), 4K (Gold)
    MAGNET_SUBMENU // Back, HD (Blue), 4K (Gold)
}

/**
 * Authentic Horseshoe Magnet Icon Vector matching the reference design:
 * Clean rounded U-magnet with silver pole tips.
 */
@Composable
fun HorseshoeMagnetIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.22f

        // Draw U-shape Horseshoe
        val uPath = Path().apply {
            moveTo(w * 0.23f, h * 0.22f)
            lineTo(w * 0.23f, h * 0.58f)
            arcTo(
                rect = Rect(
                    left = w * 0.23f,
                    top = h * 0.26f,
                    right = w * 0.77f,
                    bottom = h * 0.88f
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            lineTo(w * 0.77f, h * 0.22f)
        }

        drawPath(
            path = uPath,
            color = tint,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round
            )
        )

        // Draw Left & Right Metal Pole Caps (White/Silver accent tips)
        val capHeight = h * 0.13f
        val capWidth = strokeWidth * 0.95f

        // Left Cap
        drawRect(
            color = Color.White.copy(alpha = 0.90f),
            topLeft = Offset(w * 0.23f - capWidth / 2f, h * 0.15f),
            size = Size(capWidth, capHeight)
        )

        // Right Cap
        drawRect(
            color = Color.White.copy(alpha = 0.90f),
            topLeft = Offset(w * 0.77f - capWidth / 2f, h * 0.15f),
            size = Size(capWidth, capHeight)
        )
    }
}

@Composable
fun LinkCard(
    link: LinkEntity,
    actorsMap: Map<String, String> = emptyMap(),
    studiosMap: Map<String, String> = emptyMap(),
    isBookmarked: Boolean = false,
    isActiveCard: Boolean = false,
    onActivate: () -> Unit = {},
    onDismissActive: () -> Unit = {},
    onToggleBookmark: () -> Unit = {},
    onPlay: (url: String) -> Unit,
    onOpenGallery: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    resolvingStatus: String? = null,
    isResolvingThisCard: Boolean = false,
    resolutionError: String? = null,
    onDismissResolutionError: () -> Unit = {},
    inlinePlayback: ActiveInlineVideoPlayback? = null,
    onCloseInlineVideo: () -> Unit = {},
    onFullscreenInlineVideo: (positionMs: Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current

    // Delete Confirmation Dialog State
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Internal Submenu state while active
    var subMenuState by remember { mutableStateOf<CardActionMenuState?>(null) }
    
    val currentMenuState = when {
        !isActiveCard -> CardActionMenuState.CLOSED
        subMenuState != null -> subMenuState!!
        else -> CardActionMenuState.MAIN_MENU
    }
    val isOverlayActive = currentMenuState != CardActionMenuState.CLOSED

    // Close when dismissed from outside
    LaunchedEffect(isActiveCard) {
        if (!isActiveCard) {
            subMenuState = null
        }
    }

    // Handle System Back button when overlay is open
    BackHandler(enabled = isOverlayActive) {
        if (currentMenuState == CardActionMenuState.URL_SUBMENU || currentMenuState == CardActionMenuState.MAGNET_SUBMENU) {
            subMenuState = CardActionMenuState.MAIN_MENU
        } else {
            onDismissActive()
        }
    }

    // Smooth GPU Zoom & Blur on the cover image (only animated when active)
    val imageScale by animateFloatAsState(
        targetValue = if (isOverlayActive) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cover_scale"
    )
    val imageBlur by animateDpAsState(
        targetValue = if (isOverlayActive) 8.dp else 0.dp,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
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
        targetValue = if (isImageLoaded) 1f else 1.04f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "cover_reveal_scale"
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
                            if (isOverlayActive) {
                                onDismissActive()
                            } else {
                                onActivate()
                            }
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
                            if (isOverlayActive) {
                                Modifier
                                    .graphicsLayer {
                                        scaleX = imageScale
                                        scaleY = imageScale
                                    }
                                    .blur(imageBlur)
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
                            .graphicsLayer {
                                alpha = coverAlpha
                                if (!isOverlayActive) {
                                    scaleX = coverScale
                                    scaleY = coverScale
                                }
                            }
                    )
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

            // Smooth Scrim Layer
            if (isOverlayActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onDismissActive()
                        }
                )
            }

            // AnimatedContent transition for menu states with clip = false to prevent clipping during scale overshoot
            AnimatedContent(
                targetState = currentMenuState,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(120, easing = LinearOutSlowInEasing)) +
                            scaleIn(initialScale = 0.85f, animationSpec = tween(120)))
                        .togetherWith(
                            fadeOut(animationSpec = tween(80, easing = FastOutLinearInEasing)) +
                                    scaleOut(targetScale = 0.90f, animationSpec = tween(80))
                        )
                        .using(SizeTransform(clip = false))
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(vertical = 4.dp),
                label = "center_spread_content"
            ) { state ->
                when (state) {
                    CardActionMenuState.CLOSED -> {
                        Spacer(modifier = Modifier.size(0.dp))
                    }

                    CardActionMenuState.MAIN_MENU -> {
                        if (isPhotoset) {
                            val totalButtons = 4
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                // 0. Delete (Coral Red)
                                StaggeredCircularButton(
                                    delayIndex = 0,
                                    totalCount = totalButtons,
                                    icon = Icons.Outlined.Delete,
                                    label = "Delete",
                                    containerColor = Color(0xFFE54B4B),
                                    contentColor = Color.White,
                                    onClick = { showDeleteConfirmDialog = true }
                                )

                                // 1. Edit (Muted Warm Sand Grey)
                                StaggeredCircularButton(
                                    delayIndex = 1,
                                    totalCount = totalButtons,
                                    icon = Icons.Outlined.Edit,
                                    label = "Edit",
                                    containerColor = Color(0xFFC7C5B8),
                                    contentColor = Color(0xFF2C2C28),
                                    onClick = {
                                        onDismissActive()
                                        onEdit()
                                    }
                                )

                                // 2. Bookmark (Bookmark Ribbon: Unsaved = Outlined ring, Saved = Filled Rose Pink)
                                val bookmarkContainer = if (isBookmarked) Color(0xFFD64A71) else Color.Transparent
                                val bookmarkContent = if (isBookmarked) Color.White else Color(0xFFD64A71)
                                val bookmarkBorder = if (isBookmarked) null else BorderStroke(2.dp, Color(0xFFD64A71))

                                StaggeredCircularButton(
                                    delayIndex = 2,
                                    totalCount = totalButtons,
                                    icon = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    label = if (isBookmarked) "Saved" else "Save",
                                    containerColor = bookmarkContainer,
                                    contentColor = bookmarkContent,
                                    border = bookmarkBorder,
                                    onClick = { onToggleBookmark() }
                                )

                                // 3. Photos Gallery
                                StaggeredCircularButton(
                                    delayIndex = 3,
                                    totalCount = totalButtons,
                                    icon = Icons.Outlined.PhotoLibrary,
                                    label = "Photos",
                                    containerColor = accent,
                                    contentColor = Color.White,
                                    onClick = {
                                        onDismissActive()
                                        onOpenGallery()
                                    }
                                )
                            }
                        } else {
                            // Reversed Order (5 buttons):
                            // 1. Magnet (Vibrant Emerald Green: #1EA87A, horseshoe magnet icon) -> "Magnet"
                            // 2. URL (Vibrant Sky Blue: #2F80ED, linked rings icon) -> "URL"
                            // 3. Bookmark (Rose Pink: #D64A71, Ribbon icon. Unsaved: outlined with ring, Saved: filled) -> "Save" / "Saved"
                            // 4. Edit (Warm Sand Grey: #C7C5B8, dark icon) -> "Edit"
                            // 5. Delete (Coral Red: #E54B4B) -> "Delete"
                            val totalButtons = 3 + (if (hasAnyUrl) 1 else 0) + (if (hasAnyMagnet) 1 else 0)
                            var runningIndex = 0

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                // 1. Magnet (Vibrant Emerald Green: 0xFF1EA87A) - Only shown if at least one Magnet exists
                                if (hasAnyMagnet) {
                                    StaggeredCircularButton(
                                        delayIndex = runningIndex++,
                                        totalCount = totalButtons,
                                        customIcon = { tint ->
                                            HorseshoeMagnetIcon(
                                                tint = tint,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        },
                                        label = "Magnet",
                                        containerColor = Color(0xFF1EA87A),
                                        contentColor = Color.White,
                                        onClick = {
                                            subMenuState = CardActionMenuState.MAGNET_SUBMENU
                                        }
                                    )
                                }

                                // 2. URL (Vibrant Sky Blue: 0xFF2F80ED) - Only shown if at least one URL exists
                                if (hasAnyUrl) {
                                    StaggeredCircularButton(
                                        delayIndex = runningIndex++,
                                        totalCount = totalButtons,
                                        icon = Icons.Outlined.Link,
                                        iconRotation = 45f,
                                        label = "URL",
                                        containerColor = Color(0xFF2F80ED),
                                        contentColor = Color.White,
                                        onClick = {
                                            subMenuState = CardActionMenuState.URL_SUBMENU
                                        }
                                    )
                                }

                                // 3. Bookmark (Rose Pink Ribbon: 0xFFD64A71)
                                // Unsaved: Dark circle with rose pink outline ring and icon
                                // Saved: Solid filled rose pink circle with white icon
                                val bookmarkContainer = if (isBookmarked) Color(0xFFD64A71) else Color.Transparent
                                val bookmarkContent = if (isBookmarked) Color.White else Color(0xFFD64A71)
                                val bookmarkBorder = if (isBookmarked) null else BorderStroke(2.dp, Color(0xFFD64A71))

                                StaggeredCircularButton(
                                    delayIndex = runningIndex++,
                                    totalCount = totalButtons,
                                    icon = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    label = if (isBookmarked) "Saved" else "Save",
                                    containerColor = bookmarkContainer,
                                    contentColor = bookmarkContent,
                                    border = bookmarkBorder,
                                    onClick = {
                                        onToggleBookmark()
                                    }
                                )

                                // 4. Edit (Warm Sand Grey: 0xFFC7C5B8, Dark Slate Icon)
                                StaggeredCircularButton(
                                    delayIndex = runningIndex++,
                                    totalCount = totalButtons,
                                    icon = Icons.Outlined.Edit,
                                    label = "Edit",
                                    containerColor = Color(0xFFC7C5B8),
                                    contentColor = Color(0xFF2C2C28),
                                    onClick = {
                                        onDismissActive()
                                        onEdit()
                                    }
                                )

                                // 5. Delete (Coral Red: 0xFFE54B4B)
                                StaggeredCircularButton(
                                    delayIndex = runningIndex++,
                                    totalCount = totalButtons,
                                    icon = Icons.Outlined.Delete,
                                    label = "Delete",
                                    containerColor = Color(0xFFE54B4B),
                                    contentColor = Color.White,
                                    onClick = {
                                        showDeleteConfirmDialog = true
                                    }
                                )
                            }
                        }
                    }

                    CardActionMenuState.URL_SUBMENU -> {
                        val totalSubButtons = 1 + (if (hasUrlHD) 1 else 0) + (if (hasUrl4K) 1 else 0)
                        var subIndex = 0

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            // Back Button
                            StaggeredCircularButton(
                                delayIndex = subIndex++,
                                totalCount = totalSubButtons,
                                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                                label = "Back",
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = { subMenuState = CardActionMenuState.MAIN_MENU }
                            )

                            // HD URL (0xFF2F80ED Blue)
                            if (hasUrlHD) {
                                StaggeredQualityButton(
                                    delayIndex = subIndex++,
                                    totalCount = totalSubButtons,
                                    title = "HD",
                                    label = "HD",
                                    containerColor = Color(0xFF2F80ED),
                                    contentColor = Color.White,
                                    onClick = {
                                        onDismissActive()
                                        handleUrlSelection(link.urlHD)
                                    }
                                )
                            }

                            // 4K URL (0xFFEAB308 Gold/Yellow)
                            if (hasUrl4K) {
                                StaggeredQualityButton(
                                    delayIndex = subIndex++,
                                    totalCount = totalSubButtons,
                                    title = "4K",
                                    label = "4K",
                                    containerColor = Color(0xFFEAB308),
                                    contentColor = Color.Black,
                                    onClick = {
                                        onDismissActive()
                                        handleUrlSelection(link.url4K)
                                    }
                                )
                            }
                        }
                    }

                    CardActionMenuState.MAGNET_SUBMENU -> {
                        val totalSubButtons = 1 + (if (hasMagnetHD) 1 else 0) + (if (hasMagnet4K) 1 else 0)
                        var subIndex = 0

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            // Back Button
                            StaggeredCircularButton(
                                delayIndex = subIndex++,
                                totalCount = totalSubButtons,
                                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                                label = "Back",
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = { subMenuState = CardActionMenuState.MAIN_MENU }
                            )

                            // HD Magnet (0xFF2F80ED Blue)
                            if (hasMagnetHD) {
                                StaggeredQualityButton(
                                    delayIndex = subIndex++,
                                    totalCount = totalSubButtons,
                                    title = "HD",
                                    label = "HD",
                                    containerColor = Color(0xFF2F80ED),
                                    contentColor = Color.White,
                                    onClick = {
                                        onDismissActive()
                                        handleMagnet(link.magnet)
                                    }
                                )
                            }

                            // 4K Magnet (0xFFEAB308 Gold/Yellow)
                            if (hasMagnet4K) {
                                StaggeredQualityButton(
                                    delayIndex = subIndex++,
                                    totalCount = totalSubButtons,
                                    title = "4K",
                                    label = "4K",
                                    containerColor = Color(0xFFEAB308),
                                    contentColor = Color.Black,
                                    onClick = {
                                        onDismissActive()
                                        handleMagnet(link.magnet4K)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ========================================================
            // Inline Resolution & Progress Overlay (replaces popup dialog)
            // Cover turns into theme palette color (Dark / Amoled / Light) with real-time status steps
            // ========================================================
            if (isResolvingThisCard && resolvingStatus != null) {
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
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(42.dp),
                            color = MaterialTheme.colorScheme.primary,
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
                        val actorGradient = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                        Text(
                            text = actorsDisplayName.ifEmpty { "Scene" },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                brush = actorGradient,
                                letterSpacing = 0.15.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = studioName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 0.2.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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

    // ========================================================
    // 3. Delete Confirmation Dialog with Rounded Corners
    // ========================================================
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            shape = RoundedCornerShape(24.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFFEE2E2), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Delete Scene?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${link.title}\"? This action cannot be undone.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDismissActive()
                        onDelete()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    )
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * StaggeredCircularButton:
 * Center-outward animation delay based on distanceFromCenter.
 * Scale spring animation: dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium.
 * Alpha tween animation: duration = 220ms, easing = LinearOutSlowInEasing.
 */
@Composable
private fun StaggeredCircularButton(
    delayIndex: Int,
    totalCount: Int,
    icon: ImageVector? = null,
    customIcon: (@Composable (Color) -> Unit)? = null,
    iconRotation: Float = 0f,
    label: String,
    containerColor: Color,
    contentColor: Color,
    border: BorderStroke? = null,
    onClick: () -> Unit
) {
    val centerIndex = (totalCount - 1) / 2f
    val distanceFromCenter = abs(delayIndex - centerIndex)

    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay((distanceFromCenter * 45).toLong())
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "btn_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(220, easing = LinearOutSlowInEasing),
        label = "btn_alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.width(48.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = containerColor,
            border = border,
            shadowElevation = if (border != null) 0.dp else 4.dp,
            modifier = Modifier
                .size(46.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (customIcon != null) {
                    customIcon(contentColor)
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = contentColor,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(iconRotation)
                    )
                }
            }
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.graphicsLayer {
                this.alpha = alpha
            }
        )
    }
}

/**
 * StaggeredQualityButton:
 * Center-outward animation delay based on distanceFromCenter.
 * Scale spring animation: dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium.
 * Alpha tween animation: duration = 220ms, easing = LinearOutSlowInEasing.
 */
@Composable
private fun StaggeredQualityButton(
    delayIndex: Int,
    totalCount: Int,
    title: String,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    val centerIndex = (totalCount - 1) / 2f
    val distanceFromCenter = abs(delayIndex - centerIndex)

    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay((distanceFromCenter * 45).toLong())
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "btn_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(220, easing = LinearOutSlowInEasing),
        label = "btn_alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.width(52.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = containerColor,
            shadowElevation = 4.dp,
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = contentColor
                )
            }
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.graphicsLayer {
                this.alpha = alpha
            }
        )
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
