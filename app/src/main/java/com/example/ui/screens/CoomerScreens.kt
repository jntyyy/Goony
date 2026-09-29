package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Camera
import compose.icons.feathericons.Plus
import com.example.data.local.entity.CoomerEntity
import com.example.network.MediaScrapers
import com.example.ui.MainViewModel
import com.example.ui.ScreenState
import com.example.ui.components.SmoothProgressIndicator
import com.example.ui.components.StaggeredEntranceItem
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalBetaTestPrivacy
import com.example.ui.theme.LocalVaultPalette
import com.example.ui.theme.privacyImageBlur
import com.example.ui.theme.rememberDominantColor
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.ui.graphics.Brush
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoomerManagementScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current
    val coroutineScope = rememberCoroutineScope()

    val coomers by viewModel.allCoomers.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var isScraping by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = palette.bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("OnlyHaven (${coomers.size})", color = palette.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = "Back", tint = palette.textPrimary, modifier = Modifier.size(20.dp))
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.testTag("add_creator_button")) {
                        Icon(FeatherIcons.Plus, contentDescription = "Add Creator", tint = accent, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.surface)
            )
        }
    ) { padding ->
        if (coomers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding()),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(FeatherIcons.Camera, contentDescription = null, tint = palette.textMuted, modifier = Modifier.size(54.dp))
                    Text("No creators in vault", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                    Text("Add creators to browse OnlyFans/Fansly posts", color = palette.textMuted, fontSize = 13.sp)
                    Button(onClick = { showAddDialog = true }) {
                        Text("Add Creator")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom = 16.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(coomers, key = { _, it -> it.id }) { index, creator ->
                    StaggeredEntranceItem(index = index) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.navigateTo(ScreenState.CoomerDetail(creator.id))
                                },
                            colors = CardDefaults.cardColors(containerColor = palette.cardBg)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (creator.imageUrl.isNotEmpty()) {
                                    val isBetaTest = LocalBetaTestPrivacy.current
                                    Box {
                                        AsyncImage(
                                            model = creator.imageUrl,
                                            contentDescription = creator.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, accent, CircleShape)
                                                .privacyImageBlur(isBetaTest)
                                        )
                                        if (isBetaTest) {
                                            Box(
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.75f))
                                            )
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape)
                                            .background(palette.surface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = palette.textMuted, modifier = Modifier.size(44.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = creator.name,
                                    color = palette.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )

                                Text(
                                    text = "${creator.posts.size} posts",
                                    color = palette.textMuted,
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                IconButton(
                                    onClick = { viewModel.deleteCoomer(creator.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Creator Dialog
    if (showAddDialog) {
        var profileUrl by remember { mutableStateOf("") }
        var manualName by remember { mutableStateOf("") }
        var manualAvatar by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Creator") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Auto-Scrape from URL (e.g. Coomer/OnlyFans/Fansly):", fontSize = 12.sp, color = palette.textSecondary)
                    OutlinedTextField(
                        value = profileUrl,
                        onValueChange = { profileUrl = it },
                        label = { Text("Profile URL") },
                        modifier = Modifier.fillMaxWidth().testTag("creator_url_input")
                    )

                    HorizontalDivider(color = palette.border)
                    Text("Or manual:", fontSize = 12.sp, color = palette.textMuted)

                    OutlinedTextField(
                        value = manualName,
                        onValueChange = { manualName = it },
                        label = { Text("Creator Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = manualAvatar,
                        onValueChange = { manualAvatar = it },
                        label = { Text("Avatar Image URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (profileUrl.isNotBlank()) {
                            isScraping = true
                            coroutineScope.launch {
                                val result = MediaScrapers.scrapeCreatorProfile(profileUrl)
                                val coomer = CoomerEntity(
                                    id = UUID.randomUUID().toString(),
                                    name = result.name.ifEmpty { manualName.ifEmpty { "Creator" } },
                                    imageUrl = result.avatarUrl.ifEmpty { manualAvatar },
                                    posts = result.posts,
                                    sourceUrl = profileUrl
                                )
                                viewModel.saveCoomer(coomer)
                                isScraping = false
                                showAddDialog = false
                            }
                        } else if (manualName.isNotBlank()) {
                            val coomer = CoomerEntity(
                                id = UUID.randomUUID().toString(),
                                name = manualName.trim(),
                                imageUrl = manualAvatar.trim()
                            )
                            viewModel.saveCoomer(coomer)
                            showAddDialog = false
                        }
                    },
                    enabled = !isScraping && (profileUrl.isNotBlank() || manualName.isNotBlank())
                ) {
                    if (isScraping) {
                        SmoothProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Add")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoomerDetailScreen(
    viewModel: MainViewModel,
    coomerId: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current

    val coomers by viewModel.allCoomers.collectAsStateWithLifecycle()
    val creator = remember(coomerId, coomers) {
        coomers.firstOrNull { it.id == coomerId }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Images, 2: Videos

    Scaffold(
        containerColor = palette.bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(creator?.name ?: "Creator Posts", color = palette.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.surface)
            )
        }
    ) { padding ->
        if (creator == null) {
            Box(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()), contentAlignment = Alignment.Center) {
                Text("Creator not found", color = palette.textPrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
            ) {
                // Filter Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = palette.surface,
                    contentColor = accent
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("All (${creator.posts.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Photos") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Videos") }
                    )
                }

                val allMediaUrls = remember(creator, selectedTab) {
                    val list = mutableListOf<String>()
                    for (p in creator.posts) {
                        for ((idx, url) in p.urls.withIndex()) {
                            val isVideo = p.mediaTypes.getOrNull(idx) == "video" || url.endsWith(".mp4")
                            when (selectedTab) {
                                1 -> if (!isVideo) list.add(url)
                                2 -> if (isVideo) list.add(url)
                                else -> list.add(url)
                            }
                        }
                    }
                    list
                }

                if (allMediaUrls.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No media in this tab", color = palette.textMuted)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 120.dp),
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(allMediaUrls) { mediaUrl ->
                            val isVideo = mediaUrl.endsWith(".mp4") || mediaUrl.endsWith(".m4v")
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(palette.surface)
                                    .clickable {
                                        if (isVideo) {
                                            viewModel.playVideo(mediaUrl, creator.name)
                                        } else {
                                            viewModel.openLightbox(allMediaUrls.filter { !it.endsWith(".mp4") }, allMediaUrls.indexOf(mediaUrl).coerceAtLeast(0))
                                        }
                                    }
                            ) {
                                val isBetaTest = LocalBetaTestPrivacy.current
                                AsyncImage(
                                    model = mediaUrl,
                                    contentDescription = "Post Media",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .privacyImageBlur(isBetaTest)
                                )
                                if (isBetaTest) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.75f))
                                    )
                                }

                                if (isVideo) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.7f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
