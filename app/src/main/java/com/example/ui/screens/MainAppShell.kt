package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.ActiveVideoPlayback
import com.example.ui.MainViewModel
import com.example.ui.ScreenState
import com.example.ui.components.ExoPlayerOverlay
import com.example.ui.components.GoPlayer
import com.example.ui.components.PhotosetLightbox
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
    val activeVideo by viewModel.activeVideo.collectAsStateWithLifecycle()
    val activeLightbox by viewModel.activeLightbox.collectAsStateWithLifecycle()
    val resolvingStatus by viewModel.resolvingVideoStatus.collectAsStateWithLifecycle()
    val resolvingCardId by viewModel.resolvingCardId.collectAsStateWithLifecycle()
    val videoResolutionError by viewModel.videoResolutionError.collectAsStateWithLifecycle()

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
        gesturesEnabled = currentScreen is ScreenState.Home,
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
                                Icon(Icons.Filled.VideoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
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
                                imageVector = if (isHomeSelected) Icons.Filled.Home else Icons.Outlined.Home,
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
                                imageVector = if (isActorsSelected) Icons.Filled.RecentActors else Icons.Outlined.RecentActors,
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
                                imageVector = if (isStudiosSelected) Icons.Filled.MovieCreation else Icons.Outlined.MovieCreation,
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

                    val isHanimeSelected = currentScreen is ScreenState.Hanime || currentScreen is ScreenState.HanimeDetail
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = if (isHanimeSelected) Icons.Filled.Animation else Icons.Outlined.Animation,
                                contentDescription = "Hanime",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Hanime", fontWeight = if (isHanimeSelected) FontWeight.SemiBold else FontWeight.Normal) },
                        selected = isHanimeSelected,
                        onClick = {
                            viewModel.navigateTo(ScreenState.Hanime)
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

                    val isCoomersSelected = currentScreen is ScreenState.Coomers || currentScreen is ScreenState.CoomerDetail
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = if (isCoomersSelected) Icons.Filled.Subscriptions else Icons.Outlined.Subscriptions,
                                contentDescription = "OnlyHaven",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("OnlyHaven", fontWeight = if (isCoomersSelected) FontWeight.SemiBold else FontWeight.Normal) },
                        selected = isCoomersSelected,
                        onClick = {
                            viewModel.navigateTo(ScreenState.Coomers)
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
                                imageVector = if (isSettingsSelected) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "Settings & Sync",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Settings & Sync", fontWeight = if (isSettingsSelected) FontWeight.SemiBold else FontWeight.Normal) },
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
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Active Screen View with Smooth Motion Transitions
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            val isOpeningAddScreen = targetState is ScreenState.AddEditLink ||
                                    targetState is ScreenState.AddEditActor ||
                                    targetState is ScreenState.AddEditStudio ||
                                    targetState is ScreenState.AddEditCoomer ||
                                    targetState is ScreenState.AddEditHanime ||
                                    targetState is ScreenState.ActorScenes ||
                                    targetState is ScreenState.StudioScenes ||
                                    targetState is ScreenState.CoomerDetail ||
                                    targetState is ScreenState.HanimeDetail

                            val isLeavingAddScreen = initialState is ScreenState.AddEditLink ||
                                    initialState is ScreenState.AddEditActor ||
                                    initialState is ScreenState.AddEditStudio ||
                                    initialState is ScreenState.AddEditCoomer ||
                                    initialState is ScreenState.AddEditHanime ||
                                    initialState is ScreenState.ActorScenes ||
                                    initialState is ScreenState.StudioScenes ||
                                    initialState is ScreenState.CoomerDetail ||
                                    initialState is ScreenState.HanimeDetail

                            if (isOpeningAddScreen) {
                                (slideInVertically(animationSpec = tween(320, easing = FastOutSlowInEasing)) { fullHeight -> fullHeight / 5 } +
                                        fadeIn(animationSpec = tween(280)))
                                    .togetherWith(
                                        slideOutVertically(animationSpec = tween(260, easing = FastOutSlowInEasing)) { fullHeight -> -fullHeight / 12 } +
                                                fadeOut(animationSpec = tween(220))
                                    )
                            } else if (isLeavingAddScreen) {
                                (slideInVertically(animationSpec = tween(260, easing = FastOutSlowInEasing)) { fullHeight -> -fullHeight / 12 } +
                                        fadeIn(animationSpec = tween(240)))
                                    .togetherWith(
                                        slideOutVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) { fullHeight -> fullHeight / 5 } +
                                                fadeOut(animationSpec = tween(240))
                                    )
                            } else {
                                (fadeIn(animationSpec = tween(240)) + scaleIn(initialScale = 0.98f, animationSpec = tween(240)))
                                    .togetherWith(fadeOut(animationSpec = tween(180)))
                            }
                        },
                        label = "screen_motion_transition"
                    ) { screen ->
                        when (screen) {
                            is ScreenState.Home -> HomeScreen(
                                viewModel = viewModel,
                                onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                            )
                            is ScreenState.AddEditLink -> AddEditLinkScreen(viewModel, screen.linkId)
                            is ScreenState.Actors -> ActorManagementScreen(viewModel)
                            is ScreenState.AddEditActor -> ActorManagementScreen(viewModel)
                            is ScreenState.ActorScenes -> HomeScreen(
                                viewModel = viewModel,
                                onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                            )
                            is ScreenState.Studios -> StudioManagementScreen(viewModel)
                            is ScreenState.AddEditStudio -> StudioManagementScreen(viewModel)
                            is ScreenState.StudioScenes -> HomeScreen(
                                viewModel = viewModel,
                                onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                            )
                            is ScreenState.Coomers -> CoomerManagementScreen(viewModel)
                            is ScreenState.AddEditCoomer -> CoomerManagementScreen(viewModel)
                            is ScreenState.CoomerDetail -> CoomerDetailScreen(viewModel, screen.coomerId)
                            is ScreenState.Hanime -> HanimeManagementScreen(viewModel)
                            is ScreenState.AddEditHanime -> HanimeManagementScreen(viewModel)
                            is ScreenState.HanimeDetail -> HanimeDetailScreen(viewModel, screen.hanimeId)
                            is ScreenState.PhotosetViewer -> PhotosetViewerScreen(viewModel, screen.title, screen.images, screen.initialIndex)
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
                                CircularProgressIndicator(
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
