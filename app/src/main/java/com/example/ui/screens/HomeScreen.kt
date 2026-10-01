package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import java.util.Locale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.example.data.local.entity.ActorEntity
import com.example.data.local.entity.LinkEntity
import com.example.data.local.entity.StudioEntity
import com.example.ui.MainViewModel
import com.example.ui.ScreenState
import com.example.ui.SortMode
import com.example.ui.components.LinkCard
import com.example.ui.theme.LocalBetaTestPrivacy
import com.example.ui.theme.privacyImageBlur

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val links by viewModel.filteredLinks.collectAsStateWithLifecycle()
    val actors by viewModel.allActors.collectAsStateWithLifecycle()
    val studios by viewModel.allStudios.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentSort by viewModel.sortMode.collectAsStateWithLifecycle()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsStateWithLifecycle()
    val viewFilter by viewModel.viewFilter.collectAsStateWithLifecycle()
    val resolvingStatus by viewModel.resolvingVideoStatus.collectAsStateWithLifecycle()
    val resolvingCardId by viewModel.resolvingCardId.collectAsStateWithLifecycle()
    val videoResolutionError by viewModel.videoResolutionError.collectAsStateWithLifecycle()
    val activeInlineVideo by viewModel.activeInlineVideo.collectAsStateWithLifecycle()
    val currentScreen by viewModel.screenState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val targetActor = remember(currentScreen, actors) {
        if (currentScreen is ScreenState.ActorScenes) {
            val id = (currentScreen as ScreenState.ActorScenes).actorId
            actors.firstOrNull { it.id == id }
        } else null
    }

    val targetStudio = remember(currentScreen, studios) {
        if (currentScreen is ScreenState.StudioScenes) {
            val id = (currentScreen as ScreenState.StudioScenes).studioId
            studios.firstOrNull { it.id == id }
        } else null
    }

    val displayedLinks = remember(links, currentScreen, targetActor, targetStudio) {
        when (currentScreen) {
            is ScreenState.ActorScenes -> {
                val actorId = (currentScreen as ScreenState.ActorScenes).actorId
                links.filter { it.actorIds.contains(actorId) || (targetActor != null && it.actorIds.contains(targetActor.name)) }
            }
            is ScreenState.StudioScenes -> {
                val studioId = (currentScreen as ScreenState.StudioScenes).studioId
                links.filter { it.studioIds.contains(studioId) || (targetStudio != null && it.studioIds.contains(targetStudio.name)) }
            }
            else -> links
        }
    }

    // O(1) Precomputed Fast Lookup Maps - computed once at Screen level on data change
    val actorsMap = remember(actors) {
        actors.associate { it.id to it.name }
    }
    val fullActorsMap = remember(actors) {
        actors.associateBy { it.id }
    }
    val studiosMap = remember(studios) {
        studios.associate { it.id to it.name }
    }

    var isSearchExpanded by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var activeOverlayCardId by remember { mutableStateOf<String?>(null) }
    var showEditActorDialog by remember { mutableStateOf(false) }
    var showEditStudioDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.homeScrollIndex,
        initialFirstVisibleItemScrollOffset = viewModel.homeScrollOffset
    )

    // Continuously remember the user's exact scroll position in ViewModel
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                viewModel.homeScrollIndex = index
                viewModel.homeScrollOffset = offset
            }
    }

    // Scroll to top only when the user deliberately modifies sort, filter, or search query
    var isInitialComposition by remember { mutableStateOf(true) }
    var previousSort by remember { mutableStateOf(currentSort) }
    var previousFilter by remember { mutableStateOf(viewFilter) }
    var previousQuery by remember { mutableStateOf(searchQuery) }

    LaunchedEffect(currentSort, viewFilter, searchQuery) {
        if (isInitialComposition) {
            isInitialComposition = false
        } else if (previousSort != currentSort || previousFilter != viewFilter || previousQuery != searchQuery) {
            previousSort = currentSort
            previousFilter = viewFilter
            previousQuery = searchQuery
            viewModel.homeScrollIndex = 0
            viewModel.homeScrollOffset = 0
            if (links.isNotEmpty()) {
                listState.scrollToItem(0)
            }
        }
        activeOverlayCardId = null
    }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchExpanded) {
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.searchQuery.value = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .testTag("search_scenes_input"),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search scenes, actors, studios...",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    } else {
                        val headerTitle = when {
                            targetActor != null -> targetActor.name
                            targetStudio != null -> targetStudio.name
                            currentScreen is ScreenState.ActorScenes -> {
                                val id = (currentScreen as ScreenState.ActorScenes).actorId
                                actorsMap[id] ?: id
                            }
                            currentScreen is ScreenState.StudioScenes -> {
                                val id = (currentScreen as ScreenState.StudioScenes).studioId
                                studiosMap[id] ?: id
                            }
                            else -> "Goony"
                        }
                        Text(
                            text = headerTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                        )
                    }
                },
                navigationIcon = {
                    if (isSearchExpanded) {
                        IconButton(
                            onClick = {
                                isSearchExpanded = false
                                viewModel.searchQuery.value = ""
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close Search"
                            )
                        }
                    } else if (targetActor != null || targetStudio != null || currentScreen is ScreenState.ActorScenes || currentScreen is ScreenState.StudioScenes) {
                        IconButton(
                            onClick = { viewModel.navigateBack() },
                            modifier = Modifier.testTag("back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier.testTag("open_drawer_button")
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_app_menu),
                                contentDescription = "Open Drawer"
                            )
                        }
                    }
                },
                actions = {
                    if (isSearchExpanded) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.searchQuery.value = "" },
                                modifier = Modifier.testTag("clear_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    isSearchExpanded = false
                                    viewModel.searchQuery.value = ""
                                },
                                modifier = Modifier.testTag("close_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Search"
                                )
                            }
                        }
                    } else {
                        // Native Search Action
                        IconButton(
                            onClick = { isSearchExpanded = true },
                            modifier = Modifier.testTag("search_action_button")
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_app_search),
                                contentDescription = "Search"
                            )
                        }

                        // Native Sort Action (A-Z, Z-A, New, Old) with Rounded Native UI
                        Box {
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier.testTag("sort_action_button")
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_app_sort),
                                    contentDescription = "Sort Mode"
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                shape = RoundedCornerShape(16.dp),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "New",
                                            fontWeight = if (currentSort == SortMode.CARD_NEWEST || currentSort == SortMode.NEWEST) FontWeight.Bold else FontWeight.Normal,
                                            color = if (currentSort == SortMode.CARD_NEWEST || currentSort == SortMode.NEWEST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    leadingIcon = {
                                        if (currentSort == SortMode.CARD_NEWEST || currentSort == SortMode.NEWEST) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.sortMode.value = SortMode.CARD_NEWEST
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Old",
                                            fontWeight = if (currentSort == SortMode.CARD_OLDEST || currentSort == SortMode.OLDEST) FontWeight.Bold else FontWeight.Normal,
                                            color = if (currentSort == SortMode.CARD_OLDEST || currentSort == SortMode.OLDEST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    leadingIcon = {
                                        if (currentSort == SortMode.CARD_OLDEST || currentSort == SortMode.OLDEST) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.sortMode.value = SortMode.CARD_OLDEST
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Recently Added",
                                            fontWeight = if (currentSort == SortMode.RECENTLY_ADDED) FontWeight.Bold else FontWeight.Normal,
                                            color = if (currentSort == SortMode.RECENTLY_ADDED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    leadingIcon = {
                                        if (currentSort == SortMode.RECENTLY_ADDED) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.sortMode.value = SortMode.RECENTLY_ADDED
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Oldest Added",
                                            fontWeight = if (currentSort == SortMode.OLDEST_ADDED) FontWeight.Bold else FontWeight.Normal,
                                            color = if (currentSort == SortMode.OLDEST_ADDED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    leadingIcon = {
                                        if (currentSort == SortMode.OLDEST_ADDED) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.sortMode.value = SortMode.OLDEST_ADDED
                                        showSortMenu = false
                                    }
                                )
                            }
                        }

                        // Edit Actor/Studio Action in Header
                        if (targetActor != null) {
                            IconButton(
                                onClick = { showEditActorDialog = true },
                                modifier = Modifier.testTag("edit_actor_header_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Actor"
                                )
                            }
                        } else if (targetStudio != null) {
                            IconButton(
                                onClick = { showEditStudioDialog = true },
                                modifier = Modifier.testTag("edit_studio_header_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Studio"
                                )
                            }
                        }

                        // Native Add Scene Action - ONLY on Main Screen (Home)
                        if (currentScreen is ScreenState.Home) {
                            IconButton(
                                onClick = { viewModel.navigateTo(ScreenState.AddEditLink()) },
                                modifier = Modifier.testTag("add_scene_button")
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_app_add),
                                    contentDescription = "Add Scene"
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        // ========================================================
        // FEED LIST OF ITEMS (MATCHING SCREENSHOT LAYOUT)
        // ========================================================
        if (displayedLinks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(60.dp)
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No results for '$searchQuery'"
                               else if (targetActor != null) "No scenes for ${targetActor.name}"
                               else if (targetStudio != null) "No scenes for ${targetStudio.name}"
                               else "Vault is Empty",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Try searching with different keywords"
                               else if (targetActor != null || targetStudio != null) "Tap the '+' icon to link scenes to this entity."
                               else "Tap the '+' icon in the top bar to add scenes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding()),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(displayedLinks, key = { it.id }) { link ->
                    val isBookmarked = remember(bookmarkedIds, link.id) {
                        bookmarkedIds.contains(link.id)
                    }
                    val isActive = activeOverlayCardId == link.id

                    Box(
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(durationMillis = 150),
                            fadeOutSpec = tween(durationMillis = 100),
                            placementSpec = tween(durationMillis = 200)
                        )
                    ) {
                        LinkCard(
                            link = link,
                            actorsMap = actorsMap,
                            studiosMap = studiosMap,
                            fullActorsMap = fullActorsMap,
                            isBookmarked = isBookmarked,
                            isActiveCard = isActive,
                            onActivate = { activeOverlayCardId = link.id },
                            onDismissActive = {
                                if (activeOverlayCardId == link.id) {
                                    activeOverlayCardId = null
                                }
                            },
                            onToggleBookmark = { viewModel.toggleBookmark(link.id) },
                            onPlay = { url -> viewModel.playVideo(url, link.title, cardId = link.id) },
                            onOpenGallery = {
                                viewModel.navigateTo(ScreenState.PhotosetViewer(link.title, link.galleryUrls))
                            },
                            onEdit = {
                                viewModel.navigateTo(ScreenState.AddEditLink(link.id))
                            },
                            onDelete = {
                                viewModel.deleteLink(link.id)
                            },
                            onActorClick = { actorId ->
                                viewModel.navigateTo(ScreenState.ActorScenes(actorId))
                            },
                            onStudioClick = { studioId ->
                                viewModel.navigateTo(ScreenState.StudioScenes(studioId))
                            },
                            resolvingStatus = resolvingStatus,
                            isResolvingThisCard = resolvingCardId == link.id,
                            resolutionError = if (resolvingCardId == link.id) videoResolutionError else null,
                            onDismissResolutionError = { viewModel.dismissVideoError() },
                            inlinePlayback = if (activeInlineVideo?.cardId == link.id) activeInlineVideo else null,
                            onCloseInlineVideo = { viewModel.closeInlineVideo(link.id) },
                            onFullscreenInlineVideo = { currentPos ->
                                viewModel.openFullscreenFromInline(link.id, currentPos)
                            },
                            exoPlayer = viewModel.sharedPlayerManager.getPlayer(),
                            enableVideoPlayerGestures = settings.enableVideoPlayerGestures
                        )
                    }
                }
            }
        }
    }

    // Actor Details & Deletion Dialog (with inline Adjustment mode in the same dialog)
    if (showEditActorDialog && targetActor != null) {
        var isAdjustMode by remember { mutableStateOf(false) }
        var confirmDeleteActor by remember { mutableStateOf(false) }

        var posX by remember(targetActor.id, targetActor.imagePositionX) { mutableFloatStateOf(targetActor.imagePositionX) }
        var posY by remember(targetActor.id, targetActor.imagePositionY) { mutableFloatStateOf(targetActor.imagePositionY) }
        var zoom by remember(targetActor.id, targetActor.imageZoom) { mutableFloatStateOf(targetActor.imageZoom.coerceIn(0.5f, 3.0f)) }

        val isBetaTest = LocalBetaTestPrivacy.current
        val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
        val circleBorderColor = if (isLight) Color.Black else Color.White

        AlertDialog(
            onDismissRequest = {
                showEditActorDialog = false
                confirmDeleteActor = false
                isAdjustMode = false
            },
            shape = RoundedCornerShape(28.dp),
            title = {
                if (!isAdjustMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Actor Details",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(
                            onClick = { isAdjustMode = true },
                            modifier = Modifier.testTag("adjust_actor_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Adjust Photo Position & Zoom",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { isAdjustMode = false },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Adjust Photo",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        TextButton(
                            onClick = {
                                posX = 50f
                                posY = 50f
                                zoom = 1.0f
                            }
                        ) {
                            Text("Reset", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            },
            text = {
                if (!isAdjustMode) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Static / Unchangeable Name Field matching Add Scene style
                            OutlinedTextField(
                                value = targetActor.name,
                                onValueChange = {},
                                readOnly = true,
                                singleLine = true,
                                label = { Text("Name") },
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                shape = RoundedCornerShape(32.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("actor_name_static_input")
                            )

                            // Static / Unchangeable Image URL Field matching Add Scene style
                            OutlinedTextField(
                                value = targetActor.imageUrl ?: "",
                                onValueChange = {},
                                readOnly = true,
                                singleLine = true,
                                label = { Text("Image URL") },
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                shape = RoundedCornerShape(32.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("actor_image_static_input")
                            )

                            // Delete Actor and Linked Scenes Section (Circular Button)
                            if (!confirmDeleteActor) {
                                Button(
                                    onClick = { confirmDeleteActor = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFEF4444).copy(alpha = 0.12f),
                                        contentColor = Color(0xFFEF4444)
                                    ),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("delete_actor_cascade_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Delete Actor Scene",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFEF4444).copy(alpha = 0.12f)
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "Delete '${targetActor.name}' and all scenes referencing solely this actor? (Scenes with multiple actors will be preserved).",
                                            fontSize = 13.sp,
                                            color = Color(0xFFDC2626),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = { confirmDeleteActor = false },
                                                shape = CircleShape
                                            ) {
                                                Text("Cancel")
                                            }
                                            Spacer(Modifier.width(6.dp))
                                            Button(
                                                onClick = {
                                                    showEditActorDialog = false
                                                    confirmDeleteActor = false
                                                    viewModel.deleteActorWithCascade(targetActor.id)
                                                    viewModel.navigateBack()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFFEF4444)
                                                ),
                                                shape = CircleShape
                                            ) {
                                                Text("Confirm Delete", color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Inline Adjust Photo View
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Big Circular Preview (Strictly clipped and layered so image NEVER bleeds outside the frame)
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .shadow(3.dp, CircleShape)
                                    .graphicsLayer {
                                        shape = CircleShape
                                        clip = true
                                    }
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!targetActor.imageUrl.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                shape = CircleShape
                                                clip = true
                                            }
                                            .clip(CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val biasX = (posX - 50f) / 50f
                                        val biasY = (posY - 50f) / 50f
                                        AsyncImage(
                                            model = targetActor.imageUrl,
                                            contentDescription = targetActor.name,
                                            contentScale = ContentScale.Crop,
                                            alignment = BiasAlignment(biasX, biasY),
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .graphicsLayer {
                                                    scaleX = zoom
                                                    scaleY = zoom
                                                }
                                                .privacyImageBlur(isBetaTest)
                                        )
                                        if (isBetaTest) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.75f))
                                            )
                                        }
                                    }
                                } else {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_nav_actor),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(64.dp)
                                        )
                                    }
                                }

                                // Top border overlay - ALWAYS on top so the circular frame line is never covered!
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(2.5.dp, circleBorderColor, CircleShape)
                                )
                            }

                            // 3 Sliders: X, Y, Zoom (Slim, clean, smooth control)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Slider X
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "X (Horizontal)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${posX.toInt()}%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    SleekSlimSlider(
                                        value = posX,
                                        onValueChange = { posX = it },
                                        valueRange = 0f..100f
                                    )
                                }

                                // Slider Y
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Y (Vertical)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${posY.toInt()}%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    SleekSlimSlider(
                                        value = posY,
                                        onValueChange = { posY = it },
                                        valueRange = 0f..100f
                                    )
                                }

                                // Slider Zoom
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Zoom",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.2fx", zoom),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    SleekSlimSlider(
                                        value = zoom,
                                        onValueChange = { zoom = it },
                                        valueRange = 0.5f..3.0f
                                    )
                                }
                            }
                        }
                    }
            },
            confirmButton = {
                if (isAdjustMode) {
                    Button(
                        onClick = {
                            val updatedActor = targetActor.copy(
                                imagePositionX = posX,
                                imagePositionY = posY,
                                imageZoom = zoom
                            )
                            viewModel.saveActor(updatedActor)
                            showEditActorDialog = false
                            confirmDeleteActor = false
                            isAdjustMode = false
                        },
                        shape = CircleShape
                    ) {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                if (!isAdjustMode) {
                    TextButton(
                        onClick = {
                            showEditActorDialog = false
                            confirmDeleteActor = false
                        },
                        shape = CircleShape
                    ) {
                        Text("Close")
                    }
                } else {
                    TextButton(
                        onClick = { isAdjustMode = false },
                        shape = CircleShape
                    ) {
                        Text("Back")
                    }
                }
            }
        )
    }

    // Studio Details & Deletion Dialog (with inline Background adjustment in the same dialog)
    if (showEditStudioDialog && targetStudio != null) {
        var isAdjustMode by remember { mutableStateOf(false) }
        var confirmDeleteStudio by remember { mutableStateOf(false) }

        val initialFraction = remember(targetStudio.id, targetStudio.logoBgColor) {
            val bg = targetStudio.logoBgColor
            if (bg != null) {
                try {
                    val parsed = android.graphics.Color.parseColor(bg)
                    val r = android.graphics.Color.red(parsed)
                    val g = android.graphics.Color.green(parsed)
                    val b = android.graphics.Color.blue(parsed)
                    ((r + g + b) / 3f) / 255f
                } catch (_: Exception) {
                    0.5f
                }
            } else {
                0.0f
            }
        }
        var gradientFraction by remember(targetStudio.id, targetStudio.logoBgColor) { mutableFloatStateOf(initialFraction) }
        var isCustomBgEnabled by remember(targetStudio.id, targetStudio.logoBgColor) { mutableStateOf(targetStudio.logoBgColor != null) }

        val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
        val circleBorderColor = if (isLight) Color.Black else Color.White
        val isBetaTestStudio = LocalBetaTestPrivacy.current

        val currentGray = (gradientFraction * 255).toInt().coerceIn(0, 255)
        val currentBgColor = if (isCustomBgEnabled) Color(currentGray, currentGray, currentGray) else MaterialTheme.colorScheme.surfaceVariant
        val hexString = String.format(Locale.US, "#%02X%02X%02X", currentGray, currentGray, currentGray)

        AlertDialog(
            onDismissRequest = {
                showEditStudioDialog = false
                confirmDeleteStudio = false
                isAdjustMode = false
            },
            shape = RoundedCornerShape(28.dp),
            title = {
                if (!isAdjustMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Studio Details",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(
                            onClick = { isAdjustMode = true },
                            modifier = Modifier.testTag("adjust_studio_bg_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Adjust Logo Background",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { isAdjustMode = false },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Logo Background",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            },
            text = {
                if (!isAdjustMode) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Static / Unchangeable Name Field matching Add Scene style
                            OutlinedTextField(
                                value = targetStudio.name,
                                onValueChange = {},
                                readOnly = true,
                                singleLine = true,
                                label = { Text("Name") },
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                shape = RoundedCornerShape(32.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("studio_name_static_input")
                            )

                            // Static / Unchangeable Image URL Field matching Add Scene style
                            OutlinedTextField(
                                value = targetStudio.logoUrl ?: "",
                                onValueChange = {},
                                readOnly = true,
                                singleLine = true,
                                label = { Text("Image URL") },
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                shape = RoundedCornerShape(32.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("studio_image_static_input")
                            )

                            // Delete Studio Section (Circular Button)
                            if (!confirmDeleteStudio) {
                                Button(
                                    onClick = { confirmDeleteStudio = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFEF4444).copy(alpha = 0.12f),
                                        contentColor = Color(0xFFEF4444)
                                    ),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("delete_studio_cascade_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Delete Studio Scene",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFEF4444).copy(alpha = 0.12f)
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "Delete '${targetStudio.name}' and all associated scenes without exception?",
                                            fontSize = 13.sp,
                                            color = Color(0xFFDC2626),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = { confirmDeleteStudio = false },
                                                shape = CircleShape
                                            ) {
                                                Text("Cancel")
                                            }
                                            Spacer(Modifier.width(6.dp))
                                            Button(
                                                onClick = {
                                                    showEditStudioDialog = false
                                                    confirmDeleteStudio = false
                                                    viewModel.deleteStudioWithCascade(targetStudio.id)
                                                    viewModel.navigateBack()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFFEF4444)
                                                ),
                                                shape = CircleShape
                                            ) {
                                                Text("Confirm Delete", color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Inline Adjust Studio Background View
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Big Circular Preview with dynamic background color (Layered so it never bleeds outside the frame)
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .shadow(3.dp, CircleShape)
                                    .graphicsLayer {
                                        shape = CircleShape
                                        clip = true
                                    }
                                    .clip(CircleShape)
                                    .background(currentBgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!targetStudio.logoUrl.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                shape = CircleShape
                                                clip = true
                                            }
                                            .clip(CircleShape)
                                            .padding(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = targetStudio.logoUrl,
                                            contentDescription = targetStudio.name,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .privacyImageBlur(isBetaTestStudio)
                                        )
                                        if (isBetaTestStudio) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.75f))
                                            )
                                        }
                                    }
                                } else {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_nav_studio),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(64.dp)
                                        )
                                    }
                                }

                                // Top border overlay - ALWAYS on top!
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(2.5.dp, circleBorderColor, CircleShape)
                                )
                            }

                            // Gradient Slider from Black to White (The slider itself is the colored gradient track)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Background Color",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isCustomBgEnabled) hexString else "Default",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                GradientSlider(
                                    value = gradientFraction,
                                    onValueChange = {
                                        gradientFraction = it
                                        isCustomBgEnabled = true
                                    },
                                    currentColor = currentBgColor
                                )
                            }
                        }
                    }
            },
            confirmButton = {
                if (isAdjustMode) {
                    Button(
                        onClick = {
                            val finalHex = if (isCustomBgEnabled) hexString else null
                            viewModel.saveStudio(targetStudio.copy(logoBgColor = finalHex))
                            showEditStudioDialog = false
                            confirmDeleteStudio = false
                            isAdjustMode = false
                        },
                        shape = CircleShape
                    ) {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                if (!isAdjustMode) {
                    TextButton(
                        onClick = {
                            showEditStudioDialog = false
                            confirmDeleteStudio = false
                        },
                        shape = CircleShape
                    ) {
                        Text("Close")
                    }
                } else {
                    TextButton(
                        onClick = { isAdjustMode = false },
                        shape = CircleShape
                    ) {
                        Text("Back")
                    }
                }
            }
        )
    }
}

@Composable
private fun SleekSlimSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    trackHeight: androidx.compose.ui.unit.Dp = 4.dp,
    thumbDiameter: androidx.compose.ui.unit.Dp = 16.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    val thumbRadius = thumbDiameter / 2

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val density = LocalDensity.current
        val thumbRadiusPx = with(density) { thumbRadius.toPx() }
        val usableWidth = (widthPx - thumbRadiusPx * 2).coerceAtLeast(1f)

        fun updateFromX(touchX: Float) {
            val clamped = (touchX - thumbRadiusPx).coerceIn(0f, usableWidth)
            val newFraction = clamped / usableWidth
            val newValue = valueRange.start + newFraction * (valueRange.endInclusive - valueRange.start)
            onValueChange(newValue)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(usableWidth, valueRange) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        updateFromX(down.position.x)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            updateFromX(change.position.x)
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // Inactive slim track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .clip(CircleShape)
                    .background(inactiveColor)
            )
            // Active slim track
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(trackHeight)
                    .clip(CircleShape)
                    .background(activeColor)
            )
            // Sleek, clean circular thumb
            val thumbOffset = with(density) { (fraction * usableWidth).toDp() }
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(thumbDiameter)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(activeColor)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

@Composable
private fun GradientSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    currentColor: Color,
    modifier: Modifier = Modifier,
    trackHeight: androidx.compose.ui.unit.Dp = 8.dp,
    thumbDiameter: androidx.compose.ui.unit.Dp = 18.dp
) {
    val fraction = value.coerceIn(0f, 1f)
    val thumbRadius = thumbDiameter / 2

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val density = LocalDensity.current
        val thumbRadiusPx = with(density) { thumbRadius.toPx() }
        val usableWidth = (widthPx - thumbRadiusPx * 2).coerceAtLeast(1f)

        fun updateFromX(touchX: Float) {
            val clamped = (touchX - thumbRadiusPx).coerceIn(0f, usableWidth)
            val newFraction = clamped / usableWidth
            onValueChange(newFraction)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(usableWidth) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        updateFromX(down.position.x)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            updateFromX(change.position.x)
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // The colored gradient track itself - slim and sleek!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Black,
                                Color(0xFF333333),
                                Color(0xFF666666),
                                Color(0xFF999999),
                                Color(0xFFCCCCCC),
                                Color.White
                            )
                        )
                    )
                    .border(0.75.dp, Color.Gray.copy(alpha = 0.4f), CircleShape)
            )

            // Dynamic Thumb with current color fill and crisp contrasting border
            val thumbOffset = with(density) { (fraction * usableWidth).toDp() }
            val thumbBorderColor = if (currentColor.luminance() > 0.5f) Color.Black else Color.White
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(thumbDiameter)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(currentColor)
                    .border(2.dp, thumbBorderColor, CircleShape)
            )
        }
    }
}
