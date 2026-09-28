package com.example.ui.screens

import androidx.compose.animation.*
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
import com.example.data.local.entity.LinkEntity
import com.example.ui.MainViewModel
import com.example.ui.ScreenState
import com.example.ui.SortMode
import com.example.ui.components.LinkCard
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalVaultPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current

    val allLinks by viewModel.allLinks.collectAsStateWithLifecycle()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsStateWithLifecycle()
    val actors by viewModel.allActors.collectAsStateWithLifecycle()
    val studios by viewModel.allStudios.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentSort by viewModel.sortMode.collectAsStateWithLifecycle()
    val resolvingStatus by viewModel.resolvingVideoStatus.collectAsStateWithLifecycle()
    val resolvingCardId by viewModel.resolvingCardId.collectAsStateWithLifecycle()
    val videoResolutionError by viewModel.videoResolutionError.collectAsStateWithLifecycle()
    val activeInlineVideo by viewModel.activeInlineVideo.collectAsStateWithLifecycle()

    val actorsMap = remember(actors) { actors.associate { it.id to it.name } }
    val studiosMap = remember(studios) { studios.associate { it.id to it.name } }

    var isSearchExpanded by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var activeOverlayCardId by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    var isInitialComposition by remember { mutableStateOf(true) }
    var previousSort by remember { mutableStateOf(currentSort) }
    var previousQuery by remember { mutableStateOf(searchQuery) }

    val bookmarkedLinks = remember(allLinks, bookmarkedIds, searchQuery, currentSort) {
        val bookmarked = allLinks.filter { bookmarkedIds.contains(it.id) }
        val searched = if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            val matchingActorIds = actors.filter { it.name.lowercase().contains(q) }.map { it.id }.toSet()
            val matchingStudioIds = studios.filter { it.name.lowercase().contains(q) }.map { it.id }.toSet()
            bookmarked.filter { link ->
                link.title.lowercase().contains(q) ||
                link.actorIds.any { matchingActorIds.contains(it) } ||
                link.studioIds.any { matchingStudioIds.contains(it) }
            }
        } else {
            bookmarked
        }

        when (currentSort) {
            SortMode.NEWEST -> searched.sortedByDescending { it.assignedDate ?: it.createdAt }
            SortMode.OLDEST -> searched.sortedBy { it.assignedDate ?: it.createdAt }
            SortMode.TITLE_AZ -> searched.sortedBy { it.title.lowercase() }
            SortMode.TITLE_ZA -> searched.sortedByDescending { it.title.lowercase() }
        }
    }

    LaunchedEffect(currentSort, searchQuery) {
        if (isInitialComposition) {
            isInitialComposition = false
        } else if (previousSort != currentSort || previousQuery != searchQuery) {
            previousSort = currentSort
            previousQuery = searchQuery
            if (bookmarkedLinks.isNotEmpty()) {
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
                            placeholder = { Text("Search bookmarks...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            singleLine = true,
                            maxLines = 1,
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
                                .testTag("search_bookmarks_input"),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            }
                        )
                    } else {
                        Text(
                            text = "Bookmarks (${bookmarkedLinks.size})",
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
                        IconButton(
                            onClick = { isSearchExpanded = true },
                            modifier = Modifier.testTag("search_action_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }

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
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (bookmarkedLinks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No bookmarked scenes yet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap the Save button on any link card to bookmark it for quick access.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = WindowInsets.navigationBars.asPaddingValues(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(bookmarkedLinks, key = { it.id }) { link ->
                    Box(
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        LinkCard(
                            link = link,
                            actorsMap = actorsMap,
                            studiosMap = studiosMap,
                            isBookmarked = true,
                            isActiveCard = activeOverlayCardId == link.id,
                            onActivate = { activeOverlayCardId = link.id },
                            onDismissActive = {
                                if (activeOverlayCardId == link.id) activeOverlayCardId = null
                            },
                            onToggleBookmark = { viewModel.toggleBookmark(link.id) },
                            onPlay = { url -> viewModel.playVideo(url, link.title, link.id) },
                            onOpenGallery = {
                                if (link.galleryUrls.isNotEmpty()) {
                                    viewModel.openLightbox(link.galleryUrls, 0)
                                }
                            },
                            onEdit = { viewModel.navigateTo(ScreenState.AddEditLink(link.id)) },
                            onDelete = { viewModel.deleteLink(link.id) },
                            resolvingStatus = resolvingStatus,
                            isResolvingThisCard = resolvingCardId == link.id,
                            resolutionError = if (resolvingCardId == link.id) videoResolutionError else null,
                            onDismissResolutionError = { viewModel.dismissVideoError() },
                            inlinePlayback = if (activeInlineVideo?.cardId == link.id) activeInlineVideo else null,
                            onCloseInlineVideo = { viewModel.closeInlineVideo(link.id) },
                            onFullscreenInlineVideo = { pos -> viewModel.openFullscreenFromInline(link.id, pos) },
                            exoPlayer = if (activeInlineVideo?.cardId == link.id) viewModel.sharedPlayerManager.getPlayer() else null
                        )
                    }
                }
            }
        }
    }
}
