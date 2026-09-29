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
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.example.data.local.entity.StudioEntity
import com.example.ui.MainViewModel
import com.example.ui.ScreenState
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalBetaTestPrivacy
import com.example.ui.theme.LocalVaultPalette
import com.example.ui.theme.privacyImageBlur
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioManagementScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current

    val studios by viewModel.allStudios.collectAsStateWithLifecycle()
    val links by viewModel.allLinks.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val showCards = settings.showManagementCards

    var showAddDialog by remember { mutableStateOf(false) }
    var studioToEdit by remember { mutableStateOf<StudioEntity?>(null) }
    var sortOption by remember { mutableStateOf(ManagementSortOption.NAME_AZ) }
    var showSortMenu by remember { mutableStateOf(false) }

    val sortedStudios = remember(studios, links, sortOption) {
        when (sortOption) {
            ManagementSortOption.NAME_AZ -> studios.sortedBy { it.name.lowercase() }
            ManagementSortOption.NAME_ZA -> studios.sortedByDescending { it.name.lowercase() }
            ManagementSortOption.NEWEST -> studios.sortedByDescending { it.createdAt }
            ManagementSortOption.OLDEST -> studios.sortedBy { it.createdAt }
        }
    }

    Scaffold(
        containerColor = palette.bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Studios (${studios.size})", color = palette.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("sort_studios_button")
                        ) {
                            Icon(Icons.Default.SwapVert, contentDescription = "Sort", tint = palette.textPrimary)
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = palette.surface,
                            modifier = Modifier.background(palette.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("A - Z", color = if (sortOption == ManagementSortOption.NAME_AZ) accent else palette.textPrimary) },
                                leadingIcon = {
                                    if (sortOption == ManagementSortOption.NAME_AZ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = accent)
                                    }
                                },
                                onClick = {
                                    sortOption = ManagementSortOption.NAME_AZ
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Z - A", color = if (sortOption == ManagementSortOption.NAME_ZA) accent else palette.textPrimary) },
                                leadingIcon = {
                                    if (sortOption == ManagementSortOption.NAME_ZA) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = accent)
                                    }
                                },
                                onClick = {
                                    sortOption = ManagementSortOption.NAME_ZA
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("New", color = if (sortOption == ManagementSortOption.NEWEST) accent else palette.textPrimary) },
                                leadingIcon = {
                                    if (sortOption == ManagementSortOption.NEWEST) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = accent)
                                    }
                                },
                                onClick = {
                                    sortOption = ManagementSortOption.NEWEST
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Old", color = if (sortOption == ManagementSortOption.OLDEST) accent else palette.textPrimary) },
                                leadingIcon = {
                                    if (sortOption == ManagementSortOption.OLDEST) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = accent)
                                    }
                                },
                                onClick = {
                                    sortOption = ManagementSortOption.OLDEST
                                    showSortMenu = false
                                }
                            )
                        }
                    }

                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.testTag("add_studio_button")) {
                        Icon(Icons.Default.AddBusiness, contentDescription = "Add Studio", tint = accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.surface)
            )
        }
    ) { padding ->
        if (sortedStudios.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding()),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Business, contentDescription = null, tint = palette.textMuted, modifier = Modifier.size(54.dp))
                    Text("No studios in vault", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                    Button(onClick = { showAddDialog = true }) {
                        Text("Add Studio")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom = 16.dp,
                    start = 12.dp,
                    end = 12.dp
                ),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sortedStudios, key = { it.id }) { studio ->
                    val sceneCount = links.count { it.studioIds.contains(studio.id) }
                    val itemContent = @Composable {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val isBetaTest = LocalBetaTestPrivacy.current

                            if (!studio.logoUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = studio.logoUrl,
                                    contentDescription = studio.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, if (showCards) Color.White else accent.copy(alpha = 0.5f), CircleShape)
                                        .privacyImageBlur(isBetaTest)
                                )
                                if (isBetaTest) {
                                    Box(
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.75f))
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clip(CircleShape)
                                        .background(palette.surface)
                                        .border(2.dp, if (showCards) Color.White else accent.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Business, contentDescription = null, tint = palette.textMuted, modifier = Modifier.size(36.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = studio.name,
                                color = palette.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = "$sceneCount scenes",
                                color = palette.textMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (showCards) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.navigateTo(ScreenState.StudioScenes(studio.id))
                                },
                            colors = CardDefaults.cardColors(containerColor = palette.cardBg)
                        ) {
                            itemContent()
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.navigateTo(ScreenState.StudioScenes(studio.id))
                                }
                        ) {
                            itemContent()
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Studio Dialog
    if (showAddDialog || studioToEdit != null) {
        val editing = studioToEdit
        var name by remember { mutableStateOf(editing?.name ?: "") }
        var logoUrl by remember { mutableStateOf(editing?.logoUrl ?: "") }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                studioToEdit = null
            },
            title = { Text(if (editing != null) "Edit Studio" else "Add Studio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Studio Name *") },
                        shape = RoundedCornerShape(32.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = logoUrl,
                        onValueChange = { logoUrl = it },
                        label = { Text("Logo Image URL") },
                        shape = RoundedCornerShape(32.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Live Circular Preview Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = palette.surface,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, accent.copy(alpha = 0.6f), CircleShape)
                        ) {
                            if (logoUrl.trim().isNotEmpty()) {
                                AsyncImage(
                                    model = logoUrl.trim(),
                                    contentDescription = "Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = Icons.Default.MovieCreation,
                                        contentDescription = null,
                                        tint = palette.textMuted,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }
                        Column {
                            Text(
                                text = "Preview",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = palette.textPrimary
                            )
                            Text(
                                text = if (logoUrl.trim().isNotEmpty()) "Live studio logo preview" else "No logo URL",
                                fontSize = 11.5.sp,
                                color = palette.textMuted
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val studio = StudioEntity(
                                id = editing?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                logoUrl = logoUrl.trim().ifEmpty { null }
                            )
                            viewModel.saveStudio(studio)
                            showAddDialog = false
                            studioToEdit = null
                        }
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    studioToEdit = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
