package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import com.example.data.local.entity.HanimeEntity
import com.example.network.MediaScrapers
import com.example.ui.components.SmoothProgressIndicator
import com.example.ui.MainViewModel
import com.example.ui.ScreenState
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalBetaTestPrivacy
import com.example.ui.theme.LocalVaultPalette
import com.example.ui.theme.privacyImageBlur
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HanimeManagementScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current
    val coroutineScope = rememberCoroutineScope()

    val seriesList by viewModel.allHanime.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var isScraping by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = palette.bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Hanime Vault (${seriesList.size})", color = palette.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.testTag("add_hanime_button")) {
                        Icon(Icons.Default.Add, contentDescription = "Add Series", tint = accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.surface)
            )
        }
    ) { padding ->
        if (seriesList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding()),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Animation, contentDescription = null, tint = palette.textMuted, modifier = Modifier.size(54.dp))
                    Text("No Hanime series in vault", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                    Text("Add series using HStream URLs to play DASH 4K/1080p", color = palette.textMuted, fontSize = 13.sp)
                    Button(onClick = { showAddDialog = true }) {
                        Text("Add Series")
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
                items(seriesList, key = { it.id }) { series ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                            .clickable {
                                viewModel.navigateTo(ScreenState.HanimeDetail(series.id))
                            },
                        colors = CardDefaults.cardColors(containerColor = palette.cardBg)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .background(palette.skeletonBg)
                            ) {
                                if (series.coverImage.isNotEmpty()) {
                                    val isBetaTest = LocalBetaTestPrivacy.current
                                    AsyncImage(
                                        model = series.coverImage,
                                        contentDescription = series.title,
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
                                }

                                // Censorship badge
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (series.censorship == "UNCENSORED") Color(0xFF10B981) else Color(0xFFEF4444),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = series.censorship,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = series.title,
                                    color = palette.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${series.episodes.size} episodes",
                                    color = palette.textMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Hanime Dialog
    if (showAddDialog) {
        var hstreamUrl by remember { mutableStateOf("") }
        var manualTitle by remember { mutableStateOf("") }
        var manualCover by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Hanime Series") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Auto-Scrape from HStream / Hanime URL:", fontSize = 12.sp, color = palette.textSecondary)
                    OutlinedTextField(
                        value = hstreamUrl,
                        onValueChange = { hstreamUrl = it },
                        label = { Text("HStream Series / Episode URL") },
                        modifier = Modifier.fillMaxWidth().testTag("hanime_url_input")
                    )

                    HorizontalDivider(color = palette.border)
                    Text("Or manual entry:", fontSize = 12.sp, color = palette.textMuted)

                    OutlinedTextField(
                        value = manualTitle,
                        onValueChange = { manualTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = manualCover,
                        onValueChange = { manualCover = it },
                        label = { Text("Cover Image URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (hstreamUrl.isNotBlank()) {
                            isScraping = true
                            coroutineScope.launch {
                                val result = MediaScrapers.scrapeHanimeSeries(hstreamUrl)
                                val series = HanimeEntity(
                                    id = UUID.randomUUID().toString(),
                                    title = result.title.ifEmpty { manualTitle.ifEmpty { "Hanime Series" } },
                                    coverImage = result.coverImage.ifEmpty { manualCover },
                                    description = result.description,
                                    censorship = result.censorship,
                                    episodes = result.episodes,
                                    secondaryCovers = result.secondaryCovers
                                )
                                viewModel.saveHanime(series)
                                isScraping = false
                                showAddDialog = false
                            }
                        } else if (manualTitle.isNotBlank()) {
                            val series = HanimeEntity(
                                id = UUID.randomUUID().toString(),
                                title = manualTitle.trim(),
                                coverImage = manualCover.trim()
                            )
                            viewModel.saveHanime(series)
                            showAddDialog = false
                        }
                    },
                    enabled = !isScraping && (hstreamUrl.isNotBlank() || manualTitle.isNotBlank())
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
fun HanimeDetailScreen(
    viewModel: MainViewModel,
    hanimeId: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current

    val seriesList by viewModel.allHanime.collectAsStateWithLifecycle()
    val series = remember(hanimeId, seriesList) {
        seriesList.firstOrNull { it.id == hanimeId }
    }

    Scaffold(
        containerColor = palette.bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(series?.title ?: "Series Details", color = palette.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
                    }
                },
                actions = {
                    if (series != null) {
                        IconButton(onClick = {
                            viewModel.deleteHanime(series.id)
                            viewModel.navigateBack()
                        }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.surface)
            )
        }
    ) { padding ->
        if (series == null) {
            Box(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()), contentAlignment = Alignment.Center) {
                Text("Series not found", color = palette.textPrimary)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom = 16.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Banner & Description
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (series.coverImage.isNotEmpty()) {
                            val isBetaTest = LocalBetaTestPrivacy.current
                            Box {
                                AsyncImage(
                                    model = series.coverImage,
                                    contentDescription = series.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(110.dp)
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .privacyImageBlur(isBetaTest)
                                )
                                if (isBetaTest) {
                                    Box(
                                        modifier = Modifier
                                            .width(110.dp)
                                            .height(160.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black.copy(alpha = 0.75f))
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = series.title,
                                color = palette.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (series.censorship == "UNCENSORED") Color(0xFF10B981) else Color(0xFFEF4444)
                            ) {
                                Text(
                                    text = series.censorship,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            if (!series.description.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = series.description,
                                    color = palette.textSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 4
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Episodes (${series.episodes.size})",
                        color = palette.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (series.episodes.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = palette.cardBg)
                        ) {
                            Text("No episodes parsed yet", color = palette.textMuted, modifier = Modifier.padding(16.dp))
                        }
                    }
                } else {
                    items(series.episodes, key = { it.id }) { ep ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.playVideo(ep.url, "${series.title} - ${ep.title ?: "Ep ${ep.episodeNumber}"}")
                                },
                            colors = CardDefaults.cardColors(containerColor = palette.cardBg)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 90.dp, height = 55.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(palette.skeletonBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (ep.coverImage.isNotEmpty()) {
                                        AsyncImage(
                                            model = ep.coverImage,
                                            contentDescription = ep.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ep.title ?: "Episode ${ep.episodeNumber}",
                                        color = palette.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "DASH 4K / 1080p 48fps AV1",
                                        color = accent,
                                        fontSize = 11.sp
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
