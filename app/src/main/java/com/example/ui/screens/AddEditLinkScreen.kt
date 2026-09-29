package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.entity.LinkEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalVaultPalette
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLinkScreen(
    viewModel: MainViewModel,
    linkId: String?,
    modifier: Modifier = Modifier
) {
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val links by viewModel.allLinks.collectAsStateWithLifecycle()
    val actors by viewModel.allActors.collectAsStateWithLifecycle()
    val studios by viewModel.allStudios.collectAsStateWithLifecycle()

    val existingLink = remember(linkId, links) {
        links.firstOrNull { it.id == linkId }
    }

    var title by remember { mutableStateOf(existingLink?.title ?: "") }
    var coverImage by remember { mutableStateOf(existingLink?.coverImage ?: "") }
    val coverOffset = existingLink?.coverOffset ?: 50f
    val aspectRatio = existingLink?.aspectRatio ?: "16:9"
    var urlHD by remember { mutableStateOf(existingLink?.urlHD ?: "") }
    var url4K by remember { mutableStateOf(existingLink?.url4K ?: "") }
    var magnetHD by remember { mutableStateOf(existingLink?.magnet ?: "") }
    var magnet4K by remember { mutableStateOf(existingLink?.magnet4K ?: "") }
    val galleryUrls = existingLink?.galleryUrls ?: emptyList()
    var selectedActorIds by remember { mutableStateOf(existingLink?.actorIds ?: emptyList()) }
    var selectedStudioIds by remember { mutableStateOf(existingLink?.studioIds ?: emptyList()) }

    // Dialog pickers for Studios and Actors (Space-Saving Native UI)
    var showStudioPicker by remember { mutableStateOf(false) }
    var showActorPicker by remember { mutableStateOf(false) }

    // Date state & DatePicker dialog
    var assignedDate by remember {
        mutableStateOf(existingLink?.assignedDate ?: existingLink?.createdAt ?: System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val formattedAssignedDate = remember(assignedDate) {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        sdf.format(Date(assignedDate))
    }

    // Material 3 DatePickerDialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = assignedDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selected ->
                            assignedDate = selected
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Calculate usage / reference frequency across all scenes for sorting
    val studioUsageCounts = remember(links) {
        val counts = mutableMapOf<String, Int>()
        links.forEach { link ->
            link.studioIds.forEach { sId ->
                counts[sId] = (counts[sId] ?: 0) + 1
            }
        }
        counts
    }

    val actorUsageCounts = remember(links) {
        val counts = mutableMapOf<String, Int>()
        links.forEach { link ->
            link.actorIds.forEach { aId ->
                counts[aId] = (counts[aId] ?: 0) + 1
            }
        }
        counts
    }

    val sortedStudioItems = remember(studios, studioUsageCounts) {
        studios.map { studio ->
            TagPickerItem(
                id = studio.id,
                name = studio.name,
                usageCount = studioUsageCounts[studio.id] ?: 0
            )
        }.sortedWith(
            compareByDescending<TagPickerItem> { it.usageCount }
                .thenBy { it.name.lowercase() }
        )
    }

    val sortedActorItems = remember(actors, actorUsageCounts) {
        actors.map { actor ->
            TagPickerItem(
                id = actor.id,
                name = actor.name,
                usageCount = actorUsageCounts[actor.id] ?: 0
            )
        }.sortedWith(
            compareByDescending<TagPickerItem> { it.usageCount }
                .thenBy { it.name.lowercase() }
        )
    }

    // Studio Single-Select Dialog (Enforce max 1 studio, sorted by usage)
    if (showStudioPicker) {
        TagMultiSelectDialog(
            title = "Tag Studio",
            items = sortedStudioItems,
            selectedIds = selectedStudioIds.take(1).toSet(),
            singleSelection = true,
            onConfirm = { updated ->
                selectedStudioIds = updated.take(1).toList()
                showStudioPicker = false
            },
            onDismiss = { showStudioPicker = false }
        )
    }

    // Actor Multi-Select Dialog (Sorted by usage)
    if (showActorPicker) {
        TagMultiSelectDialog(
            title = "Tag Actors",
            items = sortedActorItems,
            selectedIds = selectedActorIds.toSet(),
            singleSelection = false,
            onConfirm = { updated ->
                selectedActorIds = updated.toList()
                showActorPicker = false
            },
            onDismiss = { showActorPicker = false }
        )
    }

    // Maximum fully-rounded pill shape for all inputs & preview
    val fieldShape = RoundedCornerShape(32.dp)
    val cardShape = RoundedCornerShape(20.dp)
    val chipShape = RoundedCornerShape(24.dp)

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(if (existingLink != null) "Edit Scene" else "Add Scene", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                val newLink = LinkEntity(
                                    id = existingLink?.id ?: UUID.randomUUID().toString(),
                                    title = title.trim(),
                                    coverImage = coverImage.trim(),
                                    coverOffset = coverOffset,
                                    aspectRatio = aspectRatio,
                                    urlHD = urlHD.trim().ifEmpty { null },
                                    url4K = url4K.trim().ifEmpty { null },
                                    magnet = magnetHD.trim().ifEmpty { null },
                                    magnet4K = magnet4K.trim().ifEmpty { null },
                                    galleryScraperUrl = existingLink?.galleryScraperUrl,
                                    galleryUrls = galleryUrls,
                                    actorIds = selectedActorIds,
                                    studioIds = selectedStudioIds,
                                    assignedDate = assignedDate
                                )
                                viewModel.saveLink(newLink)
                                viewModel.navigateBack()
                            }
                        },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.testTag("save_scene_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save",
                            tint = if (title.isNotBlank()) accent else palette.textMuted
                        )
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    )
                }
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Title Input (Comfortable Natural Height, with Paste Button)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title *") },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                trailingIcon = {
                    PasteTrailingIcon(onPaste = { title = it })
                },
                shape = fieldShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scene_title_input")
            )

            // 2. Date & Cover on the Same Row (Comfortable Natural Height)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Date Input (DatePicker icon, No Paste)
                val dateInteractionSource = remember { MutableInteractionSource() }
                LaunchedEffect(dateInteractionSource) {
                    dateInteractionSource.interactions.collect { interaction ->
                        if (interaction is PressInteraction.Release) {
                            showDatePicker = true
                        }
                    }
                }

                OutlinedTextField(
                    value = formattedAssignedDate,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Date") },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    interactionSource = dateInteractionSource,
                    trailingIcon = {
                        IconButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Select Date",
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    shape = fieldShape,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("scene_date_input")
                )

                // Cover Input (Paste Button, No X button)
                OutlinedTextField(
                    value = coverImage,
                    onValueChange = { coverImage = it },
                    label = { Text("Cover") },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    trailingIcon = {
                        PasteTrailingIcon(onPaste = { coverImage = it })
                    },
                    shape = fieldShape,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cover_image_input")
                )
            }

            // 3. Preview Card (16:9 Ratio with Rounded Corners)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Preview",
                    color = palette.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = palette.cardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
                ) {
                    if (coverImage.isNotBlank()) {
                        AsyncImage(
                            model = coverImage,
                            contentDescription = "Cover Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = null,
                                    tint = palette.textMuted,
                                    modifier = Modifier.size(34.dp)
                                )
                                Text(
                                    text = "No cover image",
                                    color = palette.textMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // 4. Stream Section (HD/4K Magnets in 1 row, HD/4K URLs in 1 row) - Clean Visual Spacing
            Text(
                text = "Stream",
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            // HD Magnet & 4K Magnet in the same row (Both with Paste Buttons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = magnetHD,
                    onValueChange = { magnetHD = it },
                    label = { Text("HD Magnet") },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    trailingIcon = {
                        PasteTrailingIcon(onPaste = { magnetHD = it })
                    },
                    shape = fieldShape,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hd_magnet_input")
                )
                OutlinedTextField(
                    value = magnet4K,
                    onValueChange = { magnet4K = it },
                    label = { Text("4K Magnet") },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    trailingIcon = {
                        PasteTrailingIcon(onPaste = { magnet4K = it })
                    },
                    shape = fieldShape,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("4k_magnet_input")
                )
            }

            // HD URL & 4K URL in the same row (Both with Paste Buttons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = urlHD,
                    onValueChange = { urlHD = it },
                    label = { Text("HD URL") },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    trailingIcon = {
                        PasteTrailingIcon(onPaste = { urlHD = it })
                    },
                    shape = fieldShape,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hd_url_input")
                )
                OutlinedTextField(
                    value = url4K,
                    onValueChange = { url4K = it },
                    label = { Text("4K URL") },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    trailingIcon = {
                        PasteTrailingIcon(onPaste = { url4K = it })
                    },
                    shape = fieldShape,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("4k_url_input")
                )
            }

            // 5. Space-Saving Native Tag Actors Section (Placed First, Clean Modern Design)
            val selectedActorsList = remember(selectedActorIds, actors) {
                actors.filter { selectedActorIds.contains(it.id) }
            }

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
                        text = "Tag Actors ${if (selectedActorsList.isNotEmpty()) "(${selectedActorsList.size})" else ""}",
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Surface(
                        shape = CircleShape,
                        color = accent.copy(alpha = 0.14f),
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable { showActorPicker = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Actor",
                                tint = accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (selectedActorsList.isEmpty()) {
                    Text(
                        text = "No actors tagged. Tap + to choose from your actors.",
                        color = palette.textMuted,
                        fontSize = 12.sp
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(selectedActorsList, key = { it.id }) { actor ->
                            InputChip(
                                selected = true,
                                onClick = {
                                    selectedActorIds = selectedActorIds - actor.id
                                },
                                label = { Text(actor.name, fontSize = 12.sp) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                shape = chipShape,
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = accent.copy(alpha = 0.18f),
                                    selectedLabelColor = palette.textPrimary
                                )
                            )
                        }
                    }
                }
            }

            // 6. Space-Saving Native Tag Studio Section (Single-Line Horizontal Scrollable, Clean Modern Design)
            val selectedStudiosList = remember(selectedStudioIds, studios) {
                studios.filter { selectedStudioIds.contains(it.id) }
            }

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
                        text = "Tag Studio ${if (selectedStudiosList.isNotEmpty()) "(${selectedStudiosList.size})" else ""}",
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Surface(
                        shape = CircleShape,
                        color = accent.copy(alpha = 0.14f),
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable { showStudioPicker = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Studio",
                                tint = accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (selectedStudiosList.isEmpty()) {
                    Text(
                        text = "No studios tagged. Tap + to choose from your studios.",
                        color = palette.textMuted,
                        fontSize = 12.sp
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(selectedStudiosList, key = { it.id }) { studio ->
                            InputChip(
                                selected = true,
                                onClick = {
                                    selectedStudioIds = selectedStudioIds - studio.id
                                },
                                label = { Text(studio.name, fontSize = 12.sp) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                shape = chipShape,
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = accent.copy(alpha = 0.18f),
                                    selectedLabelColor = palette.textPrimary
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Data model for items in the tag picker with usage frequency
 */
data class TagPickerItem(
    val id: String,
    val name: String,
    val usageCount: Int = 0
)

/**
 * Compact Space-Saving Native Tag Dialog with Rounded Search, Circular Checkers, and Pill Buttons
 */
@Composable
private fun TagMultiSelectDialog(
    title: String,
    items: List<TagPickerItem>,
    selectedIds: Set<String>,
    singleSelection: Boolean = false,
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current
    var currentSelected by remember { mutableStateOf(selectedIds) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) items
        else items.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, fontWeight = FontWeight.Bold, color = palette.textPrimary, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Sleek Compact Fully-Rounded Search Input with full visible placeholder & text
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        color = palette.textPrimary,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(palette.bg)
                        .border(1.dp, palette.border, RoundedCornerShape(22.dp)),
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search...",
                                        color = palette.textMuted,
                                        fontSize = 14.sp
                                    )
                                }
                                innerTextField()
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = palette.textMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                )

                // 2. List of items with Circular Selection Indicators & Usage Badges
                if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No items found", color = palette.textMuted, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            val isChecked = currentSelected.contains(item.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        currentSelected = if (singleSelection) {
                                            if (isChecked) emptySet() else setOf(item.id)
                                        } else {
                                            if (isChecked) currentSelected - item.id else currentSelected + item.id
                                        }
                                    }
                                    .background(if (isChecked) accent.copy(alpha = 0.08f) else Color.Transparent)
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 3. Circular Selection Indicator (Not square)
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(if (isChecked) accent else Color.Transparent)
                                        .border(
                                            width = if (isChecked) 0.dp else 1.8.dp,
                                            color = if (isChecked) accent else palette.border,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isChecked) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = item.name,
                                    color = if (isChecked) accent else palette.textPrimary,
                                    fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(currentSelected) },
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = if (singleSelection) "Done" else if (currentSelected.isNotEmpty()) "Done (${currentSelected.size})" else "Done",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
            ) {
                Text("Cancel", color = palette.textPrimary)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = palette.surface
    )
}

/**
 * Compact Native Paste Icon Button (Clean icon only)
 */
@Composable
private fun PasteTrailingIcon(
    onPaste: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val accent = LocalAccentColor.current
    IconButton(
        onClick = {
            clipboardManager.getText()?.text?.let { clipText ->
                if (clipText.isNotBlank()) {
                    onPaste(clipText.trim())
                }
            }
        },
        modifier = Modifier
            .padding(end = 6.dp)
            .size(38.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.ContentPaste,
            contentDescription = "Paste from Clipboard",
            tint = accent,
            modifier = Modifier.size(19.dp)
        )
    }
}

