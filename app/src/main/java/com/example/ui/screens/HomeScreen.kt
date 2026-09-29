package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.local.entity.ActorEntity
import com.example.data.local.entity.LinkEntity
import com.example.data.local.entity.StudioEntity
import com.example.ui.MainViewModel
import com.example.ui.ScreenState
import com.example.ui.SortMode
import com.example.ui.components.LinkCard

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
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.searchQuery.value = it },
                            placeholder = { Text("Search scenes, actors, studios...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .testTag("search_scenes_input"),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
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
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Drawer"
                            )
                        }
                    }
                },
                actions = {
                    if (!isSearchExpanded) {
                        // Native Search Action
                        IconButton(
                            onClick = { isSearchExpanded = true },
                            modifier = Modifier.testTag("search_action_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
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
                                    imageVector = Icons.Default.SwapVert,
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
                                            "A-Z",
                                            fontWeight = if (currentSort == SortMode.TITLE_AZ) FontWeight.Bold else FontWeight.Normal,
                                            color = if (currentSort == SortMode.TITLE_AZ) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (currentSort == SortMode.TITLE_AZ) MaterialTheme.colorScheme.primary else Color.Transparent
                                        )
                                    },
                                    onClick = {
                                        viewModel.sortMode.value = SortMode.TITLE_AZ
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Z-A",
                                            fontWeight = if (currentSort == SortMode.TITLE_ZA) FontWeight.Bold else FontWeight.Normal,
                                            color = if (currentSort == SortMode.TITLE_ZA) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (currentSort == SortMode.TITLE_ZA) MaterialTheme.colorScheme.primary else Color.Transparent
                                        )
                                    },
                                    onClick = {
                                        viewModel.sortMode.value = SortMode.TITLE_ZA
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "New",
                                            fontWeight = if (currentSort == SortMode.NEWEST) FontWeight.Bold else FontWeight.Normal,
                                            color = if (currentSort == SortMode.NEWEST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (currentSort == SortMode.NEWEST) MaterialTheme.colorScheme.primary else Color.Transparent
                                        )
                                    },
                                    onClick = {
                                        viewModel.sortMode.value = SortMode.NEWEST
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Old",
                                            fontWeight = if (currentSort == SortMode.OLDEST) FontWeight.Bold else FontWeight.Normal,
                                            color = if (currentSort == SortMode.OLDEST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (currentSort == SortMode.OLDEST) MaterialTheme.colorScheme.primary else Color.Transparent
                                        )
                                    },
                                    onClick = {
                                        viewModel.sortMode.value = SortMode.OLDEST
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
                                    imageVector = Icons.Default.Add,
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
                            exoPlayer = viewModel.sharedPlayerManager.getPlayer()
                        )
                    }
                }
            }
        }
    }

    // Actor Details & Deletion Dialog
    if (showEditActorDialog && targetActor != null) {
        var confirmDeleteActor by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showEditActorDialog = false
                confirmDeleteActor = false
            },
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "Actor Details",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
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
                                "Delete Actors / Studio Scene",
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
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditActorDialog = false
                        confirmDeleteActor = false
                    },
                    shape = CircleShape
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Studio Details & Deletion Dialog
    if (showEditStudioDialog && targetStudio != null) {
        var confirmDeleteStudio by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showEditStudioDialog = false
                confirmDeleteStudio = false
            },
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "Studio Details",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
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

                    // Delete Studio and Linked Scenes Section (Circular Button)
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
                                "Delete Actors / Studio Scene",
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
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditStudioDialog = false
                        confirmDeleteStudio = false
                    },
                    shape = CircleShape
                ) {
                    Text("Close")
                }
            }
        )
    }
}
