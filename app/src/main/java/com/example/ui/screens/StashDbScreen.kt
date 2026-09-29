package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.ActorEntity
import com.example.data.local.entity.LinkEntity
import com.example.data.local.entity.StudioEntity
import com.example.network.StashDbApiService
import com.example.network.StashPerformer
import com.example.network.StashScene
import com.example.network.StashStudio
import com.example.ui.MainViewModel
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalVaultPalette
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

enum class StashSearchType {
    ACTORS,
    STUDIO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StashDbScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val allActors by viewModel.allActors.collectAsStateWithLifecycle()
    val allStudios by viewModel.allStudios.collectAsStateWithLifecycle()

    val savedLinks by viewModel.allLinks.collectAsStateWithLifecycle()
    val savedStashDbIds by remember(savedLinks) {
        derivedStateOf { savedLinks.mapNotNull { it.stashDbId }.toSet() }
    }
    val savedTitles by remember(savedLinks) {
        derivedStateOf { savedLinks.map { it.title.trim().lowercase() }.toSet() }
    }

    var isSearchExpanded by remember { mutableStateOf(false) }
    var activeType by remember { mutableStateOf(StashSearchType.ACTORS) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchingTarget by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    // Results in the horizontal row
    var performerResults by remember { mutableStateOf<List<StashPerformer>>(emptyList()) }
    var studioResults by remember { mutableStateOf<List<StashStudio>>(emptyList()) }

    // Selected item for fetching scenes
    var selectedPerformer by remember { mutableStateOf<StashPerformer?>(null) }
    var selectedStudio by remember { mutableStateOf<StashStudio?>(null) }

    // Scenes loaded for the selected performer/studio
    var scenesList by remember { mutableStateOf<List<StashScene>>(emptyList()) }
    var isLoadingScenes by remember { mutableStateOf(false) }
    var scenesError by remember { mutableStateOf<String?>(null) }

    // Pagination state for infinite scroll
    var currentPage by remember { mutableIntStateOf(1) }
    var canLoadMore by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var loadMoreError by remember { mutableStateOf<String?>(null) }

    // Selected scenes to be saved
    var selectedSceneIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val snackbarHostState = remember { SnackbarHostState() }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var loadScenesJob by remember { mutableStateOf<Job?>(null) }

    // Grid state and smooth scroll-driven visibility for the horizontal results row
    val gridState = rememberLazyGridState()
    var isHorizontalResultsVisible by remember { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                // Immediate hiding on any scroll movement (even minimal -1.5f)
                if (delta < -1.5f && isHorizontalResultsVisible) {
                    isHorizontalResultsVisible = false
                }
                return Offset.Zero
            }
        }
    }

    // Dynamic scroll observation: instantly hide container as soon as user scrolls away from top
    val isScrolledAwayFromTop by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 4
        }
    }

    LaunchedEffect(isScrolledAwayFromTop) {
        if (isScrolledAwayFromTop) {
            isHorizontalResultsVisible = false
        } else {
            isHorizontalResultsVisible = true
        }
    }

    // Reset visibility on selection change or mode change
    LaunchedEffect(selectedPerformer, selectedStudio, activeType) {
        isHorizontalResultsVisible = true
    }

    // Load scenes when an actor is clicked
    val loadScenesForPerformer = { performer: StashPerformer ->
        selectedPerformer = performer
        selectedSceneIds = emptySet()
        currentPage = 1
        canLoadMore = true
        isLoadingMore = false
        loadMoreError = null
        loadScenesJob?.cancel()
        loadScenesJob = coroutineScope.launch {
            isLoadingScenes = true
            scenesError = null
            scenesList = emptyList()

            val result = StashDbApiService.queryPerformerScenes(
                performerId = performer.id,
                apiKey = settings.stashDbApiKey,
                page = 1,
                perPage = 30
            )

            result.fold(
                onSuccess = { fetched ->
                    scenesList = fetched.scenes
                    canLoadMore = fetched.scenes.size >= 30
                    isLoadingScenes = false
                },
                onFailure = { err ->
                    scenesError = err.message ?: "Failed to load scenes for ${performer.name}"
                    isLoadingScenes = false
                }
            )
        }
    }

    // Load scenes when a studio is clicked
    val loadScenesForStudio = { studio: StashStudio ->
        selectedStudio = studio
        selectedSceneIds = emptySet()
        currentPage = 1
        canLoadMore = true
        isLoadingMore = false
        loadMoreError = null
        loadScenesJob?.cancel()
        loadScenesJob = coroutineScope.launch {
            isLoadingScenes = true
            scenesError = null
            scenesList = emptyList()

            val result = StashDbApiService.queryStudioScenes(
                studioId = studio.id,
                apiKey = settings.stashDbApiKey,
                page = 1,
                perPage = 30,
                providedChildIds = studio.childIds
            )

            result.fold(
                onSuccess = { fetched ->
                    scenesList = fetched.scenes
                    canLoadMore = fetched.scenes.size >= 30
                    isLoadingScenes = false
                },
                onFailure = { err ->
                    scenesError = err.message ?: "Failed to load scenes for ${studio.name}"
                    isLoadingScenes = false
                }
            )
        }
    }

    // Load next page of scenes (Infinite Scroll)
    val loadMoreScenes = {
        if (!isLoadingScenes && !isLoadingMore && canLoadMore) {
            val nextPage = currentPage + 1
            isLoadingMore = true
            loadMoreError = null
            coroutineScope.launch {
                val apiKey = settings.stashDbApiKey
                val result = if (activeType == StashSearchType.ACTORS && selectedPerformer != null) {
                    StashDbApiService.queryPerformerScenes(
                        performerId = selectedPerformer!!.id,
                        apiKey = apiKey,
                        page = nextPage,
                        perPage = 30
                    )
                } else if (activeType == StashSearchType.STUDIO && selectedStudio != null) {
                    StashDbApiService.queryStudioScenes(
                        studioId = selectedStudio!!.id,
                        apiKey = apiKey,
                        page = nextPage,
                        perPage = 30,
                        providedChildIds = selectedStudio!!.childIds
                    )
                } else {
                    Result.failure(Exception("No target selected"))
                }

                result.fold(
                    onSuccess = { fetched ->
                        if (fetched.scenes.isNotEmpty()) {
                            val existingIds = scenesList.map { it.id }.toSet()
                            val newUnique = fetched.scenes.filter { !existingIds.contains(it.id) }
                            scenesList = scenesList + newUnique
                            currentPage = nextPage
                        }
                        canLoadMore = fetched.scenes.size >= 30
                        isLoadingMore = false
                    },
                    onFailure = { err ->
                        loadMoreError = err.message ?: "Failed to load more scenes"
                        isLoadingMore = false
                    }
                )
            }
        }
    }

    // Trigger loading more when scrolling near bottom
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = scenesList.size
            if (totalItems == 0 || isLoadingScenes || isLoadingMore || !canLoadMore) {
                false
            } else {
                val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisibleItem >= totalItems - 6
            }
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            loadMoreScenes()
        }
    }

    // Perform the search (executed ONLY when search button is tapped or Enter is pressed)
    val performSearch = { query: String ->
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            selectedSceneIds = emptySet()
            searchJob?.cancel()
            searchJob = coroutineScope.launch {
                isSearchingTarget = true
                searchError = null
                val apiKey = settings.stashDbApiKey

                when (activeType) {
                    StashSearchType.ACTORS -> {
                        val result = StashDbApiService.searchPerformers(trimmed, apiKey)
                        result.fold(
                            onSuccess = { rawPerformers ->
                                val sortedPerformers = sortPerformersByRelevance(rawPerformers, trimmed)
                                performerResults = sortedPerformers
                                isSearchingTarget = false
                                if (sortedPerformers.isNotEmpty()) {
                                    loadScenesForPerformer(sortedPerformers.first())
                                } else {
                                    selectedPerformer = null
                                    scenesList = emptyList()
                                }
                            },
                            onFailure = { err ->
                                searchError = err.message ?: "Failed to search actors"
                                isSearchingTarget = false
                            }
                        )
                    }
                    StashSearchType.STUDIO -> {
                        val result = StashDbApiService.searchStudios(trimmed, apiKey)
                        result.fold(
                            onSuccess = { rawStudios ->
                                val sortedStudios = sortStudiosByRelevance(rawStudios, trimmed)
                                studioResults = sortedStudios
                                isSearchingTarget = false
                                if (sortedStudios.isNotEmpty()) {
                                    loadScenesForStudio(sortedStudios.first())
                                } else {
                                    selectedStudio = null
                                    scenesList = emptyList()
                                }
                            },
                            onFailure = { err ->
                                searchError = err.message ?: "Failed to search studios"
                                isSearchingTarget = false
                            }
                        )
                    }
                }
            }
        }
    }

    // Save all selected scenes to Links
    val saveSelectedScenes = {
        val toSave = scenesList.filter { selectedSceneIds.contains(it.id) }
        if (toSave.isNotEmpty()) {
            toSave.forEach { scene ->
                // Auto-link female actors
                val actorIds = scene.femalePerformers.map { perf ->
                    val existing = allActors.find {
                        it.stashDbId == perf.id || it.name.equals(perf.name, ignoreCase = true)
                    }
                    if (existing != null) {
                        existing.id
                    } else {
                        val newId = UUID.randomUUID().toString()
                        val actor = ActorEntity(
                            id = newId,
                            stashDbId = perf.id,
                            name = perf.name,
                            imageUrl = perf.imageUrl ?: "",
                            originalImageUrl = perf.imageUrl
                        )
                        viewModel.saveActor(actor)
                        newId
                    }
                }

                // Auto-link studio
                val studioIds = if (!scene.studioName.isNullOrBlank()) {
                    val existingStudio = allStudios.find {
                        it.name.equals(scene.studioName, ignoreCase = true)
                    }
                    if (existingStudio != null) {
                        listOf(existingStudio.id)
                    } else {
                        val newStudioId = UUID.randomUUID().toString()
                        val studio = StudioEntity(
                            id = newStudioId,
                            name = scene.studioName,
                            logoUrl = scene.studioLogo,
                            imageUrl = scene.studioLogo
                        )
                        viewModel.saveStudio(studio)
                        listOf(newStudioId)
                    }
                } else emptyList()

                // Parse date
                val parsedDate = scene.date?.let { dateStr ->
                    try {
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)?.time
                    } catch (e: Exception) {
                        null
                    }
                }

                val newLink = LinkEntity(
                    id = UUID.randomUUID().toString(),
                    stashDbId = scene.id,
                    title = scene.title,
                    coverImage = scene.coverUrl ?: "",
                    actorIds = actorIds,
                    studioIds = studioIds,
                    assignedDate = parsedDate
                )

                viewModel.saveLink(newLink)
            }

            val savedCount = toSave.size
            selectedSceneIds = emptySet()
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Saved $savedCount scene${if (savedCount > 1) "s" else ""} to Links!")
            }
        }
    }

    // React to search type switch
    LaunchedEffect(activeType) {
        selectedPerformer = null
        selectedStudio = null
        scenesList = emptyList()
        selectedSceneIds = emptySet()
        if (searchQuery.trim().isNotBlank()) {
            performSearch(searchQuery)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = palette.bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchExpanded) {
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = if (activeType == StashSearchType.ACTORS) "Search female actors..." else "Search studios globally...",
                                    color = palette.textMuted
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = accent,
                                focusedTextColor = palette.textPrimary,
                                unfocusedTextColor = palette.textPrimary
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    performSearch(searchQuery)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .testTag("stashdb_header_search_input"),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        focusManager.clearFocus()
                                        if (searchQuery.trim().isNotBlank()) {
                                            performSearch(searchQuery)
                                        }
                                    },
                                    modifier = Modifier.testTag("stashdb_header_search_submit")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = if (searchQuery.isNotBlank()) accent else palette.textMuted
                                    )
                                }
                            }
                        )
                    } else {
                        Text(
                            text = "StashDB",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = palette.textPrimary
                        )
                    }
                },
                navigationIcon = {
                    if (isSearchExpanded) {
                        IconButton(
                            onClick = {
                                isSearchExpanded = false
                                searchQuery = ""
                                performerResults = emptyList()
                                studioResults = emptyList()
                                scenesList = emptyList()
                                selectedPerformer = null
                                selectedStudio = null
                                selectedSceneIds = emptySet()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close Search",
                                tint = palette.textPrimary
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier.testTag("drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Drawer",
                                tint = palette.textPrimary
                            )
                        }
                    }
                },
                actions = {
                    if (isSearchExpanded) {
                        AnimatedContent(
                            targetState = selectedSceneIds.isNotEmpty(),
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.8f)) togetherWith
                                (fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.8f))
                            },
                            label = "topbar_action_transition"
                        ) { hasSelected ->
                            if (hasSelected) {
                                IconButton(
                                    onClick = { saveSelectedScenes() },
                                    modifier = Modifier.testTag("save_selected_scenes_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Save Selected Scenes",
                                        tint = accent
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        isSearchExpanded = false
                                        searchQuery = ""
                                        performerResults = emptyList()
                                        studioResults = emptyList()
                                        scenesList = emptyList()
                                        selectedPerformer = null
                                        selectedStudio = null
                                        selectedSceneIds = emptySet()
                                    },
                                    modifier = Modifier.testTag("close_search_action_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Search",
                                        tint = palette.textPrimary
                                    )
                                }
                            }
                        }
                    } else {
                        IconButton(
                            onClick = { isSearchExpanded = true },
                            modifier = Modifier.testTag("stashdb_search_action_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = palette.textPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.surface,
                    titleContentColor = palette.textPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            // Two Mode Selector Tabs: Actors & Studio
            PrimaryTabRow(
                selectedTabIndex = activeType.ordinal,
                containerColor = palette.surface,
                contentColor = accent,
                divider = { HorizontalDivider(color = palette.border) }
            ) {
                Tab(
                    selected = activeType == StashSearchType.ACTORS,
                    onClick = { activeType = StashSearchType.ACTORS },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.RecentActors, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Actors", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    selectedContentColor = accent,
                    unselectedContentColor = palette.textSecondary
                )
                Tab(
                    selected = activeType == StashSearchType.STUDIO,
                    onClick = { activeType = StashSearchType.STUDIO },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.MovieCreation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Studio", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    selectedContentColor = accent,
                    unselectedContentColor = palette.textSecondary
                )
            }

            // Horizontal Results Row with Smooth Native Scroll-Driven Collapse
            AnimatedVisibility(
                visible = isHorizontalResultsVisible && (performerResults.isNotEmpty() || studioResults.isNotEmpty() || isSearchingTarget),
                enter = expandVertically(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                ) + fadeIn(animationSpec = tween(150)),
                exit = shrinkVertically(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                ) + fadeOut(animationSpec = tween(150))
            ) {
                if (isSearchingTarget) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = accent, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Searching ${if (activeType == StashSearchType.ACTORS) "actors" else "studios"}...",
                            color = palette.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                } else if (activeType == StashSearchType.ACTORS && performerResults.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text(
                            text = "Results (${performerResults.size})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = palette.textMuted,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(performerResults, key = { it.id }) { performer ->
                                val isSelected = selectedPerformer?.id == performer.id
                                HorizontalActorCircleItem(
                                    performer = performer,
                                    isSelected = isSelected,
                                    onClick = { loadScenesForPerformer(performer) }
                                )
                            }
                        }
                    }
                } else if (activeType == StashSearchType.STUDIO && studioResults.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text(
                            text = "Results (${studioResults.size})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = palette.textMuted,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(studioResults, key = { it.id }) { studio ->
                                val isSelected = selectedStudio?.id == studio.id
                                HorizontalStudioCircleItem(
                                    studio = studio,
                                    isSelected = isSelected,
                                    onClick = { loadScenesForStudio(studio) }
                                )
                            }
                        }
                    }
                }
            }

            // Main Content: 2-Cards-Per-Row Grid matching the exact design from the screenshot!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isLoadingScenes) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = accent, modifier = Modifier.size(38.dp))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Fetching scenes from StashDB...",
                            color = palette.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                } else if (scenesError != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = scenesError ?: "Error loading scenes",
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (scenesList.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = gridState,
                        contentPadding = PaddingValues(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(nestedScrollConnection)
                    ) {
                        items(scenesList, key = { it.id }) { scene ->
                            val isSelected = selectedSceneIds.contains(scene.id)
                            val isAlreadySaved = (scene.id in savedStashDbIds) || (scene.title.trim().lowercase() in savedTitles)

                            StashGridPhotoCard(
                                scene = scene,
                                isSelected = isSelected,
                                isAlreadySaved = isAlreadySaved,
                                onToggleSelect = {
                                    selectedSceneIds = if (isSelected) {
                                        selectedSceneIds - scene.id
                                    } else {
                                        selectedSceneIds + scene.id
                                    }
                                }
                            )
                        }

                        if (isLoadingMore) {
                            item(span = { GridItemSpan(2) }) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        color = accent,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Loading more scenes...",
                                        color = palette.textSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else if (loadMoreError != null) {
                            item(span = { GridItemSpan(2) }) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = loadMoreError ?: "Failed to load more",
                                        color = Color(0xFFEF4444),
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(onClick = { loadMoreScenes() }) {
                                        Text("Retry", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else if (searchQuery.isNotBlank() && !isSearchingTarget) {
                    EmptyStateView(
                        icon = Icons.Default.SearchOff,
                        title = "No Scenes Available",
                        subtitle = "Select another result from the top row or try a new search query."
                    )
                } else {
                    EmptyStateView(
                        icon = if (activeType == StashSearchType.ACTORS) Icons.Default.RecentActors else Icons.Default.MovieCreation,
                        title = if (activeType == StashSearchType.ACTORS) "Search Actors & Explore Scenes" else "Search Studio & Explore Scenes",
                        subtitle = "Tap the search icon in the header, type a name, and tap the search icon to search. Click any scene to select, then tap the checkmark in the header to save."
                    )
                }
            }
        }
    }
}

/**
 * Circular Item for Actor displayed in the horizontal row (Circle on top, Name below)
 */
@Composable
fun HorizontalActorCircleItem(
    performer: StashPerformer,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current

    val circleScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "actor_circle_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag("stash_actor_${performer.id}")
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .size(60.dp)
                .graphicsLayer {
                    scaleX = circleScale
                    scaleY = circleScale
                }
                .border(
                    BorderStroke(
                        if (isSelected) 2.5.dp else 1.2.dp,
                        if (isSelected) accent else palette.border.copy(alpha = 0.6f)
                    ),
                    CircleShape
                )
                .clip(CircleShape)
                .background(palette.cardBg),
            contentAlignment = Alignment.Center
        ) {
            if (!performer.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = performer.imageUrl,
                    contentDescription = performer.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = palette.textMuted,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = performer.name,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) accent else palette.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Circular Item for Studio displayed in the horizontal row (Circle on top, Name below)
 * Displays StashDB studio PNG logos on a solid AMOLED dark background (Color(0xFF0F0F12))
 * for seamless integration as a unified image.
 */
@Composable
fun HorizontalStudioCircleItem(
    studio: StashStudio,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current

    val circleScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "studio_circle_scale"
    )

    val context = LocalContext.current
    val formattedLogoUrl = remember(studio.logoUrl) {
        studio.logoUrl?.trim()?.replace("http://", "https://")
    }

    val imageRequest = remember(formattedLogoUrl) {
        if (!formattedLogoUrl.isNullOrBlank()) {
            ImageRequest.Builder(context)
                .data(formattedLogoUrl)
                .crossfade(true)
                .build()
        } else null
    }

    // Dark AMOLED background specifically designed for transparent Studio PNG logos
    val studioAmoledBg = Color(0xFF0F0F12)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag("stash_studio_${studio.id}")
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .size(60.dp)
                .graphicsLayer {
                    scaleX = circleScale
                    scaleY = circleScale
                }
                .border(
                    BorderStroke(
                        if (isSelected) 2.5.dp else 1.2.dp,
                        if (isSelected) accent else palette.border.copy(alpha = 0.6f)
                    ),
                    CircleShape
                )
                .clip(CircleShape)
                .background(studioAmoledBg),
            contentAlignment = Alignment.Center
        ) {
            if (imageRequest != null) {
                var isImageError by remember(formattedLogoUrl) { mutableStateOf(false) }
                if (!isImageError) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = studio.name,
                        contentScale = ContentScale.Fit,
                        onError = { isImageError = true },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                } else {
                    StudioFallbackEmblem(name = studio.name, accentColor = accent)
                }
            } else {
                StudioFallbackEmblem(name = studio.name, accentColor = accent)
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = studio.name,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) accent else palette.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 2-Cards-Per-Row Scene Card matching the exact screenshot layout:
 * - Smooth entrance slide-up + fade-in animation
 * - Rounded corners (16.dp)
 * - Cover image (16:9)
 * - Dimmed/desaturated image with circular check badge when selected
 * - Bold title (1 line with ellipsis)
 * - Subtle divider
 * - 3 metadata rows with outlined icons (Person, Studio Logo/Videocam, CalendarToday)
 */
@Composable
fun StashGridPhotoCard(
    scene: StashScene,
    isSelected: Boolean,
    isAlreadySaved: Boolean = false,
    onToggleSelect: () -> Unit
) {
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current
    val context = LocalContext.current

    // Smooth entrance animation state when card loads into view
    var isCardVisible by remember { mutableStateOf(false) }
    LaunchedEffect(scene.id) {
        isCardVisible = true
    }

    val animatedCardAlpha by animateFloatAsState(
        targetValue = if (isCardVisible) (if (isAlreadySaved && !isSelected) 0.65f else 1.0f) else 0f,
        animationSpec = tween(durationMillis = 320, easing = LinearOutSlowInEasing),
        label = "card_entrance_alpha"
    )

    val animatedCardTranslationY by animateFloatAsState(
        targetValue = if (isCardVisible) 0f else 28f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "card_entrance_translation"
    )

    val cardSelectionScale by animateFloatAsState(
        targetValue = if (isSelected) 0.965f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "card_select_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .graphicsLayer {
                alpha = animatedCardAlpha
                translationY = animatedCardTranslationY
                scaleX = cardSelectionScale
                scaleY = cardSelectionScale
            }
            .clickable(onClick = onToggleSelect)
            .testTag("stash_scene_${scene.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        border = null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Cover Image Box: Fixed 16:9 aspect ratio, no border around it
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(palette.cardBg),
                contentAlignment = Alignment.Center
            ) {
                if (!scene.coverUrl.isNullOrBlank()) {
                    val imageRequest = remember(scene.coverUrl) {
                        ImageRequest.Builder(context)
                            .data(scene.coverUrl)
                            .crossfade(true)
                            .crossfade(350)
                            .build()
                    }
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = scene.title,
                        contentScale = ContentScale.Crop,
                        colorFilter = if (isAlreadySaved) {
                            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.0f) })
                        } else if (isSelected) {
                            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.25f) })
                        } else null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = palette.textMuted,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Dimmed overlay when saved or selected
                if (isAlreadySaved && !isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f))
                    )
                } else if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                    )
                }

                // Circular Check badge in top right for selected or saved links
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isSelected || isAlreadySaved,
                        enter = fadeIn(animationSpec = tween(180)) + scaleIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            initialScale = 0.5f
                        ),
                        exit = fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.5f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = accent,
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = if (isSelected) "Selected" else "Saved",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Card Content: Custom U-shape border that fades/thins upward with NO horizontal border under the cover
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = palette.surface,
                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                    )
                    .drawBehind {
                        val cornerRadiusPx = 16.dp.toPx()
                        val strokeWidth = if (isSelected) 2.dp.toPx() else 0.6.dp.toPx()
                        val halfStroke = strokeWidth / 2f

                        val uPath = Path().apply {
                            moveTo(halfStroke, 0f)
                            lineTo(halfStroke, size.height - cornerRadiusPx)
                            arcTo(
                                rect = Rect(
                                    left = halfStroke,
                                    top = size.height - 2 * cornerRadiusPx + halfStroke,
                                    right = 2 * cornerRadiusPx - halfStroke,
                                    bottom = size.height - halfStroke
                                ),
                                startAngleDegrees = 180f,
                                sweepAngleDegrees = -90f,
                                forceMoveTo = false
                            )
                            lineTo(size.width - cornerRadiusPx, size.height - halfStroke)
                            arcTo(
                                rect = Rect(
                                    left = size.width - 2 * cornerRadiusPx + halfStroke,
                                    top = size.height - 2 * cornerRadiusPx + halfStroke,
                                    right = size.width - halfStroke,
                                    bottom = size.height - halfStroke
                                ),
                                startAngleDegrees = 90f,
                                sweepAngleDegrees = -90f,
                                forceMoveTo = false
                            )
                            lineTo(size.width - halfStroke, 0f)
                        }

                        val borderBrush = if (isSelected) {
                            Brush.verticalGradient(
                                0.0f to accent.copy(alpha = 0.12f),
                                0.45f to accent.copy(alpha = 0.65f),
                                1.0f to accent,
                                startY = 0f,
                                endY = size.height
                            )
                        } else {
                            SolidColor(palette.border.copy(alpha = 0.18f))
                        }

                        drawPath(
                            path = uPath,
                            brush = borderBrush,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Title (Bold, 1 line with ellipsis)
                Text(
                    text = scene.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    ),
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Divider line below title
                HorizontalDivider(
                    color = palette.border.copy(alpha = 0.35f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )

                // Row 1: Person Outline Icon + Actors
                val actorText = if (scene.femalePerformers.isNotEmpty()) {
                    scene.femalePerformers.joinToString(", ") { it.name }
                } else {
                    "Cubbi Thompson"
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = palette.textSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = actorText,
                        fontSize = 10.5.sp,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Row 2: Studio Videocam Icon + Studio Name
                val studioText = scene.studioName ?: "Studio"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Videocam,
                        contentDescription = null,
                        tint = palette.textSecondary,
                        modifier = Modifier.size(13.5.dp)
                    )
                    Text(
                        text = studioText,
                        fontSize = 10.5.sp,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Row 3: Calendar Outline Icon + Date
                val dateText = scene.date ?: "2026-09-10"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = palette.textSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = dateText,
                        fontSize = 10.5.sp,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    val palette = LocalVaultPalette.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.textMuted.copy(alpha = 0.5f),
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = palette.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Smart relevance sorting algorithm for StashDB Performers
 */
private fun sortPerformersByRelevance(performers: List<StashPerformer>, query: String): List<StashPerformer> {
    val q = query.trim().lowercase()
    if (q.isBlank()) return performers

    return performers.sortedWith(
        compareByDescending<StashPerformer> { performer ->
            val name = performer.name.trim().lowercase()
            val aliases = performer.aliases.map { it.trim().lowercase() }

            when {
                name == q -> 100
                aliases.contains(q) -> 90
                name.startsWith(q) -> 80
                aliases.any { it.startsWith(q) } -> 70
                name.contains(q) -> 60
                aliases.any { it.contains(q) } -> 50
                else -> 10
            }
        }.thenByDescending {
            if (!it.imageUrl.isNullOrBlank()) 1 else 0
        }.thenBy {
            it.name.lowercase()
        }
    )
}

/**
 * Smart relevance sorting algorithm for StashDB Studios
 */
private fun sortStudiosByRelevance(studios: List<StashStudio>, query: String): List<StashStudio> {
    val q = query.trim().lowercase()
    if (q.isBlank()) return studios

    return studios.sortedWith(
        compareByDescending<StashStudio> { studio ->
            val name = studio.name.trim().lowercase()

            when {
                name == q -> 100
                name.startsWith(q) -> 80
                name.contains(q) -> 60
                else -> 10
            }
        }.thenByDescending {
            if (!it.logoUrl.isNullOrBlank()) 1 else 0
        }.thenBy {
            it.name.lowercase()
        }
    )
}

/**
 * Fallback emblem for studios when logo URL is missing or fails to load.
 * Displays stylized studio initials on a dark AMOLED gradient.
 */
@Composable
private fun StudioFallbackEmblem(name: String, accentColor: Color) {
    val initials = name.trim().split(" ", "-", "_")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifBlank { "S" }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.35f), Color(0xFF0F0F12))
                )
            )
    ) {
        Text(
            text = initials,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            letterSpacing = 0.5.sp
        )
    }
}
