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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.example.data.local.entity.ActorEntity
import com.example.ui.MainViewModel
import com.example.ui.ScreenState
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalBetaTestPrivacy
import com.example.ui.theme.LocalVaultPalette
import com.example.ui.theme.privacyImageBlur
import java.util.UUID

enum class ManagementSortOption {
    NAME_AZ,
    NAME_ZA,
    NEWEST,
    OLDEST
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActorManagementScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current

    val actors by viewModel.allActors.collectAsStateWithLifecycle()
    val links by viewModel.allLinks.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val showCards = settings.showManagementCards

    var showAddDialog by remember { mutableStateOf(false) }
    var actorToEdit by remember { mutableStateOf<ActorEntity?>(null) }
    var sortOption by remember { mutableStateOf(ManagementSortOption.NAME_AZ) }
    var showSortMenu by remember { mutableStateOf(false) }

    val sortedActors = remember(actors, links, sortOption) {
        when (sortOption) {
            ManagementSortOption.NAME_AZ -> actors.sortedBy { it.name.lowercase() }
            ManagementSortOption.NAME_ZA -> actors.sortedByDescending { it.name.lowercase() }
            ManagementSortOption.NEWEST -> actors.sortedByDescending { it.createdAt }
            ManagementSortOption.OLDEST -> actors.sortedBy { it.createdAt }
        }
    }

    Scaffold(
        containerColor = palette.bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Actors (${actors.size})", color = palette.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("sort_actors_button")
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

                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.testTag("add_actor_button")) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Actor", tint = accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.surface)
            )
        }
    ) { padding ->
        if (sortedActors.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding()),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = palette.textMuted, modifier = Modifier.size(54.dp))
                    Text("No actors in vault", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                    Button(onClick = { showAddDialog = true }) {
                        Text("Add Actor")
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
                items(sortedActors, key = { it.id }) { actor ->
                    val sceneCount = links.count { it.actorIds.contains(actor.id) }
                    val itemContent = @Composable {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val isBetaTest = LocalBetaTestPrivacy.current

                            Surface(
                                shape = CircleShape,
                                color = palette.surface,
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, if (showCards) Color.White else accent.copy(alpha = 0.6f), CircleShape)
                            ) {
                                if (actor.imageUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = actor.imageUrl,
                                        contentDescription = actor.name,
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
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(accent.copy(alpha = 0.25f), palette.cardBg)
                                                )
                                            ),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = palette.textSecondary.copy(alpha = 0.9f),
                                            modifier = Modifier
                                                .fillMaxSize(0.85f)
                                                .padding(bottom = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = actor.name,
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
                                    viewModel.navigateTo(ScreenState.ActorScenes(actor.id))
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
                                    viewModel.navigateTo(ScreenState.ActorScenes(actor.id))
                                }
                        ) {
                            itemContent()
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Actor Dialog
    if (showAddDialog || actorToEdit != null) {
        val editing = actorToEdit
        var name by remember { mutableStateOf(editing?.name ?: "") }
        var imageUrl by remember { mutableStateOf(editing?.imageUrl ?: "") }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                actorToEdit = null
            },
            title = { Text(if (editing != null) "Edit Actor" else "Add Actor") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Actor Name *") },
                        shape = RoundedCornerShape(32.dp),
                        modifier = Modifier.fillMaxWidth().testTag("actor_name_input")
                    )
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("Profile Image URL") },
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
                            if (imageUrl.trim().isNotEmpty()) {
                                AsyncImage(
                                    model = imageUrl.trim(),
                                    contentDescription = "Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
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
                                text = if (imageUrl.trim().isNotEmpty()) "Live actor photo preview" else "No image URL",
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
                            val actor = ActorEntity(
                                id = editing?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                imageUrl = imageUrl.trim()
                            )
                            viewModel.saveActor(actor)
                            showAddDialog = false
                            actorToEdit = null
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
                    actorToEdit = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
