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
                links.filter { it.actorIds.contains(actorId) }
            }
            is ScreenState.StudioScenes -> {
                val studioId = (currentScreen as ScreenState.StudioScenes).studioId
                links.filter { it.studioIds.contains(studioId) }
            }
            else -> links
        }
    }

    // O(1) Precomputed Fast Lookup Maps - computed once at Screen level on data change
    val actorsMap = remember(actors) {
        actors.associate { it.id to it.name }
    }
    val studiosMap = remember(studios) {
        studios.associate { it.id to it.name }
    }

    var isSearchExpanded by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var activeOverlayCardId by remember { mutableStateOf<String?>(null) }
    var showEditActorDialog by remember { mutableStateOf(false) }
    var showDeleteActorConfirm by remember { mutableStateOf(false) }
    var showEditStudioDialog by remember { mutableStateOf(false) }
    var showDeleteStudioConfirm by remember { mutableStateOf(false) }

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
                    } else if (targetActor != null || targetStudio != null) {
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

                        // Edit / Delete Actor/Studio Actions in Header
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
                            IconButton(
                                onClick = { showDeleteActorConfirm = true },
                                modifier = Modifier.testTag("delete_actor_header_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Actor",
                                    tint = Color(0xFFEF4444)
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
                            IconButton(
                                onClick = { showDeleteStudioConfirm = true },
                                modifier = Modifier.testTag("delete_studio_header_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Studio",
                                    tint = Color(0xFFEF4444)
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
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = WindowInsets.navigationBars.asPaddingValues()
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
                            resolvingStatus = resolvingStatus,
                            isResolvingThisCard = resolvingCardId == link.id,
                            resolutionError = if (resolvingCardId == link.id) videoResolutionError else null,
                            onDismissResolutionError = { viewModel.dismissVideoError() },
                            inlinePlayback = if (activeInlineVideo?.cardId == link.id) activeInlineVideo else null,
                            onCloseInlineVideo = { viewModel.closeInlineVideo(link.id) },
                            onFullscreenInlineVideo = { currentPos ->
                                viewModel.openFullscreenFromInline(link.id, currentPos)
                            }
                        )
                    }
                }
            }
        }
    }

    // Edit Actor Dialog
    if (showEditActorDialog && targetActor != null) {
        var name by remember(targetActor) { mutableStateOf(targetActor.name) }
        var imageUrl by remember(targetActor) { mutableStateOf(targetActor.imageUrl ?: "") }

        AlertDialog(
            onDismissRequest = { showEditActorDialog = false },
            title = { Text("Edit Actor") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Actor Name *") },
                        shape = RoundedCornerShape(32.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_actor_name_input")
                    )
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("Profile Image URL") },
                        shape = RoundedCornerShape(32.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.saveActor(
                                targetActor.copy(
                                    name = name.trim(),
                                    imageUrl = imageUrl.trim().ifEmpty { "" }
                                )
                            )
                            showEditActorDialog = false
                        }
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditActorDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Actor Confirmation Dialog
    if (showDeleteActorConfirm && targetActor != null) {
        AlertDialog(
            onDismissRequest = { showDeleteActorConfirm = false },
            title = { Text("Delete Actor") },
            text = { Text("Are you sure you want to delete '${targetActor.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteActorConfirm = false
                        viewModel.deleteActor(targetActor.id)
                        viewModel.navigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteActorConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Studio Dialog
    if (showEditStudioDialog && targetStudio != null) {
        var name by remember(targetStudio) { mutableStateOf(targetStudio.name) }
        var logoUrl by remember(targetStudio) { mutableStateOf(targetStudio.logoUrl ?: "") }

        AlertDialog(
            onDismissRequest = { showEditStudioDialog = false },
            title = { Text("Edit Studio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Studio Name *") },
                        shape = RoundedCornerShape(32.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_studio_name_input")
                    )
                    OutlinedTextField(
                        value = logoUrl,
                        onValueChange = { logoUrl = it },
                        label = { Text("Logo Image URL") },
                        shape = RoundedCornerShape(32.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.saveStudio(
                                targetStudio.copy(
                                    name = name.trim(),
                                    logoUrl = logoUrl.trim().ifEmpty { null }
                                )
                            )
                            showEditStudioDialog = false
                        }
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditStudioDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Studio Confirmation Dialog
    if (showDeleteStudioConfirm && targetStudio != null) {
        AlertDialog(
            onDismissRequest = { showDeleteStudioConfirm = false },
            title = { Text("Delete Studio") },
            text = { Text("Are you sure you want to delete '${targetStudio.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteStudioConfirm = false
                        viewModel.deleteStudio(targetStudio.id)
                        viewModel.navigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteStudioConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
