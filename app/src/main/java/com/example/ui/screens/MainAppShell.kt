package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.ActiveVideoPlayback
import com.example.ui.MainViewModel
import com.example.ui.ScreenState
import com.example.ui.components.ExoPlayerOverlay
import com.example.ui.components.GoPlayer
import com.example.ui.components.PhotosetLightbox
import com.example.ui.components.SmoothProgressIndicator
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalVaultPalette
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppShell(viewModel: MainViewModel) {
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val currentScreen by viewModel.screenState.collectAsStateWithLifecycle()
    val navDirection by viewModel.navDirection.collectAsStateWithLifecycle()
    val activeVideo by viewModel.activeVideo.collectAsStateWithLifecycle()
    val activeLightbox by viewModel.activeLightbox.collectAsStateWithLifecycle()
    val resolvingStatus by viewModel.resolvingVideoStatus.collectAsStateWithLifecycle()
    val resolvingCardId by viewModel.resolvingCardId.collectAsStateWithLifecycle()
    val videoResolutionError by viewModel.videoResolutionError.collectAsStateWithLifecycle()
    val currentSettings by viewModel.settings.collectAsStateWithLifecycle()

    // Smooth App Launch Entrance Animation (Matches Add Scene motion)
    var appEntranceVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        appEntranceVisible = true
    }

    // Handle back button press
    BackHandler(enabled = true) {
        if (activeLightbox != null) {
            viewModel.closeLightbox()
        } else if (activeVideo != null) {
            viewModel.closeVideo()
        } else if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else {
            val handled = viewModel.navigateBack()
            if (!handled) {
                // At root, let system handle exit
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = palette.surface,
                drawerContentColor = palette.textPrimary,
                modifier = Modifier.width(280.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = accent,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                    contentDescription = "Goony Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Text("Goony", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = palette.textPrimary)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = palette.border)
                    Spacer(modifier = Modifier.height(12.dp))

                    val isHomeSelected = currentScreen is ScreenState.Home
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_nav_home),
                                contentDescription = "Home",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Home", fontWeight = if (isHomeSelected) FontWeight.SemiBold else FontWeight.Normal) },
                        selected = isHomeSelected,
                        onClick = {
                            viewModel.navigateTo(ScreenState.Home)
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = accent.copy(alpha = 0.18f),
                            selectedTextColor = accent,
                            selectedIconColor = accent,
                            unselectedTextColor = palette.textPrimary,
                            unselectedIconColor = palette.textSecondary
                        ),
                        shape = CircleShape,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    val isActorsSelected = currentScreen is ScreenState.Actors || currentScreen is ScreenState.ActorScenes
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_nav_actor),
                                contentDescription = "Actors",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Actors", fontWeight = if (isActorsSelected) FontWeight.SemiBold else FontWeight.Normal) },
                        selected = isActorsSelected,
                        onClick = {
                            viewModel.navigateTo(ScreenState.Actors)
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = accent.copy(alpha = 0.18f),
                            selectedTextColor = accent,
                            selectedIconColor = accent,
                            unselectedTextColor = palette.textPrimary,
                            unselectedIconColor = palette.textSecondary
                        ),
                        shape = CircleShape,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    val isStudiosSelected = currentScreen is ScreenState.Studios || currentScreen is ScreenState.StudioScenes
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_nav_studio),
                                contentDescription = "Studios",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Studios", fontWeight = if (isStudiosSelected) FontWeight.SemiBold else FontWeight.Normal) },
                        selected = isStudiosSelected,
                        onClick = {
                            viewModel.navigateTo(ScreenState.Studios)
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = accent.copy(alpha = 0.18f),
                            selectedTextColor = accent,
                            selectedIconColor = accent,
                            unselectedTextColor = palette.textPrimary,
                            unselectedIconColor = palette.textSecondary
                        ),
                        shape = CircleShape,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    val isBookmarksSelected = currentScreen is ScreenState.Bookmarks
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_nav_bookmark),
                                contentDescription = "Bookmarks",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Bookmarks", fontWeight = if (isBookmarksSelected) FontWeight.SemiBold else FontWeight.Normal) },
                        selected = isBookmarksSelected,
                        onClick = {
                            viewModel.navigateTo(ScreenState.Bookmarks)
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = accent.copy(alpha = 0.18f),
                            selectedTextColor = accent,
                            selectedIconColor = accent,
                            unselectedTextColor = palette.textPrimary,
                            unselectedIconColor = palette.textSecondary
                        ),
                        shape = CircleShape,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    val isStashDbSelected = currentScreen is ScreenState.StashDb
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_nav_stashdb),
                                contentDescription = "StashDB",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("StashDB", fontWeight = if (isStashDbSelected) FontWeight.SemiBold else FontWeight.Normal) },
                        selected = isStashDbSelected,
                        onClick = {
                            viewModel.navigateTo(ScreenState.StashDb)
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = accent.copy(alpha = 0.18f),
                            selectedTextColor = accent,
                            selectedIconColor = accent,
                            unselectedTextColor = palette.textPrimary,
                            unselectedIconColor = palette.textSecondary
                        ),
                        shape = CircleShape,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))
                    HorizontalDivider(color = palette.border)
                    Spacer(modifier = Modifier.height(12.dp))

                    val isSettingsSelected = currentScreen is ScreenState.Settings
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_nav_settings),
                                contentDescription = "Settings",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Settings", fontWeight = if (isSettingsSelected) FontWeight.SemiBold else FontWeight.Normal) },
                        selected = isSettingsSelected,
                        onClick = {
                            viewModel.navigateTo(ScreenState.Settings)
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = accent.copy(alpha = 0.18f),
                            selectedTextColor = accent,
                            selectedIconColor = accent,
                            unselectedTextColor = palette.textPrimary,
                            unselectedIconColor = palette.textSecondary
                        ),
                        shape = CircleShape,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    ) {
        AnimatedVisibility(
            visible = appEntranceVisible,
            enter = slideInVertically(
                animationSpec = tween(340, easing = FastOutSlowInEasing)
            ) { fullHeight -> fullHeight / 5 } + fadeIn(animationSpec = tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Active Screen View with Smooth Motion Transitions
                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                when (currentSettings.transitionStyle) {
                                    1 -> {
                                        // Lateral / Horizontal Slide (Bidirectional Side Motion)
                                        if (navDirection == MainViewModel.NavigationDirection.BACK) {
                                            (slideInHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> -width / 4 } +
                                                    fadeIn(animationSpec = tween(200, easing = LinearOutSlowInEasing)))
                                                .togetherWith(
                                                    slideOutHorizontally(animationSpec = tween(200, easing = FastOutSlowInEasing)) { width -> width / 4 } +
                                                            fadeOut(animationSpec = tween(170))
                                                )
                                        } else {
                                            (slideInHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> width / 4 } +
                                                    fadeIn(animationSpec = tween(200, easing = LinearOutSlowInEasing)))
                                                .togetherWith(
                                                    slideOutHorizontally(animationSpec = tween(200, easing = FastOutSlowInEasing)) { width -> -width / 4 } +
                                                            fadeOut(animationSpec = tween(170))
                                                )
                                        }
                                    }
                                    2 -> {
                                        // Ultra Smooth & Lightweight Fade + Subtle Scale (Minimum CPU/GPU overhead)
                                        if (navDirection == MainViewModel.NavigationDirection.BACK) {
                                            (fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
                                                    scaleIn(initialScale = 1.03f, animationSpec = tween(220, easing = LinearOutSlowInEasing)))
                                                .togetherWith(
                                                    fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing)) +
                                                            scaleOut(targetScale = 0.97f, animationSpec = tween(180, easing = FastOutLinearInEasing))
                                                )
                                        } else {
                                            (fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
                                                    scaleIn(initialScale = 0.97f, animationSpec = tween(220, easing = LinearOutSlowInEasing)))
                                                .togetherWith(
                                                    fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing)) +
                                                            scaleOut(targetScale = 1.03f, animationSpec = tween(180, easing = FastOutLinearInEasing))
                                                )
                                        }
                                    }
                                    3 -> {
                                        // Link Transition: Static TopBar/Head (No slide motion), natural in-place transition of header elements
                                        if (navDirection == MainViewModel.NavigationDirection.BACK) {
                                            fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing))
                                                .togetherWith(
                                                    fadeOut(animationSpec = tween(160, easing = FastOutLinearInEasing))
                                                )
                                        } else {
                                            fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing))
                                                .togetherWith(
                                                    fadeOut(animationSpec = tween(160, easing = FastOutLinearInEasing))
                                                )
                                        }
                                    }
                                    else -> {
                                        // Default: Dynamic Vertical Motion
                                        if (navDirection == MainViewModel.NavigationDirection.BACK) {
                                            (slideInVertically(animationSpec = tween(260, easing = FastOutSlowInEasing)) { fullHeight -> -fullHeight / 12 } +
                                                    fadeIn(animationSpec = tween(240)))
                                                .togetherWith(
                                                    slideOutVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) { fullHeight -> fullHeight / 5 } +
                                                            fadeOut(animationSpec = tween(240))
                                                )
                                        } else {
                                            (slideInVertically(animationSpec = tween(320, easing = FastOutSlowInEasing)) { fullHeight -> fullHeight / 5 } +
                                                    fadeIn(animationSpec = tween(280)))
                                                .togetherWith(
                                                    slideOutVertically(animationSpec = tween(260, easing = FastOutSlowInEasing)) { fullHeight -> -fullHeight / 12 } +
                                                            fadeOut(animationSpec = tween(220))
                                                )
                                        }
                                    }
                                }
                            },
                            label = "screen_motion_transition"
                        ) { screen ->
                        when (screen) {
                            is ScreenState.Home -> HomeScreen(
                                viewModel = viewModel,
                                onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                            )
                            is ScreenState.Bookmarks -> BookmarksScreen(
                                viewModel = viewModel,
                                onOpenDrawer = {}
                            )
                            is ScreenState.AddEditLink -> AddEditLinkScreen(viewModel, screen.linkId)
                            is ScreenState.Actors -> ActorManagementScreen(viewModel)
                            is ScreenState.AddEditActor -> ActorManagementScreen(viewModel)
                            is ScreenState.ActorScenes -> HomeScreen(
                                viewModel = viewModel,
                                onOpenDrawer = {}
                            )
                            is ScreenState.Studios -> StudioManagementScreen(viewModel)
                            is ScreenState.AddEditStudio -> StudioManagementScreen(viewModel)
                            is ScreenState.StudioScenes -> HomeScreen(
                                viewModel = viewModel,
                                onOpenDrawer = {}
                            )
                            is ScreenState.PhotosetViewer -> PhotosetViewerScreen(viewModel, screen.title, screen.images, screen.initialIndex)
                            is ScreenState.StashDb -> StashDbScreen(
                                viewModel = viewModel,
                                onOpenDrawer = {}
                            )
                            is ScreenState.Settings -> SettingsScreen(viewModel)
                        }
                    }
                }
            }

            // GoPlayer / ExoPlayer Video Player Overlay
            activeVideo?.let { video ->
                GoPlayer(
                    title = video.title,
                    qualities = video.qualities,
                    subtitles = video.subtitles,
                    defaultHeaders = video.headers,
                    initialPositionMs = video.initialPositionMs,
                    startInLandscape = video.startInLandscape,
                    exoPlayer = viewModel.sharedPlayerManager.getPlayer(),
                    onClose = { viewModel.closeVideo() }
                )
            }

            // High-Res Photoset Lightbox Overlay
            activeLightbox?.let { (images, startIndex) ->
                PhotosetLightbox(
                    images = images,
                    initialIndex = startIndex,
                    onClose = { viewModel.closeLightbox() }
                )
            }

            // Video Resolving / Debrid Progress Overlay (Only for non-card actions, cards handle inline)
            if (resolvingCardId == null) {
                resolvingStatus?.let { statusText ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .widthIn(max = 320.dp)
                                .padding(20.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = palette.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                SmoothProgressIndicator(
                                    modifier = Modifier.size(44.dp),
                                    color = accent,
                                    strokeWidth = 3.5.dp
                                )
                                Text(
                                    text = statusText,
                                    color = palette.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Video Resolution / Debrid Error Dialog (Only shown globally if not triggered by an inline card)
            if (resolvingCardId == null) {
                videoResolutionError?.let { errText ->
                    AlertDialog(
                        onDismissRequest = { viewModel.dismissVideoError() },
                        icon = {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(36.dp)
                            )
                        },
                        title = {
                            Text(
                                text = "Stream Playback Error",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        },
                        text = {
                            Text(
                                text = errText,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = palette.textSecondary
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = { viewModel.dismissVideoError() },
                                colors = ButtonDefaults.buttonColors(containerColor = accent)
                            ) {
                                Text("OK")
                            }
                        }
                    )
                }
            }
        }
    }
}
}
