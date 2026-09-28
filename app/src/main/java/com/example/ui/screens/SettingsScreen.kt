package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.SettingsEntity
import com.example.ui.MainViewModel
import com.example.ui.components.ColorPalettePicker
import com.example.ui.components.DataBackupSection
import com.example.ui.components.IntegrationsDropdownDebridSection
import com.example.ui.components.NativeThemeSelector
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalVaultPalette
import com.example.ui.theme.parseHexColor
import kotlinx.coroutines.launch

private enum class SettingsSection {
    MAIN_MENU,
    DISPLAY,
    INTEGRATIONS,
    DATA_BACKUP,
    SAMPLE_DATA
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current
    val coroutineScope = rememberCoroutineScope()

    val currentSettingsRaw by viewModel.settings.collectAsStateWithLifecycle()
    val currentSettings = currentSettingsRaw ?: SettingsEntity()

    var themeName by remember(currentSettings) { mutableStateOf(currentSettings.currentTheme) }
    var accentHex by remember(currentSettings) { mutableStateOf(currentSettings.accentColorHex) }
    var torboxKey by remember(currentSettings) { mutableStateOf(currentSettings.torboxApiKey) }
    var rdKey by remember(currentSettings) { mutableStateOf(currentSettings.realDebridApiKey) }

    var sampleDataStatus by remember { mutableStateOf("") }
    var currentSection by remember { mutableStateOf(SettingsSection.MAIN_MENU) }

    // Intercept hardware/gesture back press when inside a sub-category
    BackHandler(enabled = currentSection != SettingsSection.MAIN_MENU) {
        currentSection = SettingsSection.MAIN_MENU
    }

    val screenTitle = when (currentSection) {
        SettingsSection.MAIN_MENU -> "Settings"
        SettingsSection.DISPLAY -> "Display"
        SettingsSection.INTEGRATIONS -> "Integrations"
        SettingsSection.DATA_BACKUP -> "Data & Backup"
        SettingsSection.SAMPLE_DATA -> "Sample Data"
    }

    fun saveAllSettings() {
        viewModel.updateSettings(
            currentSettings.copy(
                currentTheme = themeName,
                accentColorHex = accentHex,
                torboxApiKey = torboxKey.trim(),
                realDebridApiKey = rdKey.trim()
            )
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        screenTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentSection != SettingsSection.MAIN_MENU) {
                            currentSection = SettingsSection.MAIN_MENU
                        } else {
                            viewModel.navigateBack()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        AnimatedContent(
            targetState = currentSection,
            transitionSpec = {
                if (targetState != SettingsSection.MAIN_MENU) {
                    (fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) +
                            slideInHorizontally { width -> width / 3 })
                        .togetherWith(
                            fadeOut(animationSpec = androidx.compose.animation.core.tween(150))
                        )
                } else {
                    fadeIn(animationSpec = androidx.compose.animation.core.tween(200))
                        .togetherWith(
                            fadeOut(animationSpec = androidx.compose.animation.core.tween(150)) +
                                    slideOutHorizontally { width -> width / 3 }
                        )
                }
            },
            label = "settings_navigation"
        ) { section ->
            when (section) {
                SettingsSection.MAIN_MENU -> {
                    SettingsMainMenu(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState()),
                        themeName = themeName,
                        rdKeyConfigured = rdKey.isNotBlank() || torboxKey.isNotBlank(),
                        onNavigateTo = { currentSection = it }
                    )
                }
                SettingsSection.DISPLAY -> {
                    SettingsDisplaySection(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        themeName = themeName,
                        onThemeChange = {
                            themeName = it
                            saveAllSettings()
                        },
                        accentHex = accentHex,
                        onAccentChange = {
                            accentHex = it
                            saveAllSettings()
                        },
                        showCards = currentSettings.showManagementCards,
                        onShowCardsChange = {
                            viewModel.updateSettings(currentSettings.copy(showManagementCards = it))
                        },
                        appIconStyle = currentSettings.appIconStyle,
                        onAppIconStyleChange = {
                            viewModel.updateSettings(currentSettings.copy(appIconStyle = it))
                        }
                    )
                }
                SettingsSection.INTEGRATIONS -> {
                    IntegrationsDropdownDebridSection(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        realDebridKey = rdKey,
                        onRealDebridKeyChange = {
                            rdKey = it
                            viewModel.updateSettings(currentSettings.copy(realDebridApiKey = it.trim()))
                        },
                        torboxKey = torboxKey,
                        onTorboxKeyChange = {
                            torboxKey = it
                            viewModel.updateSettings(currentSettings.copy(torboxApiKey = it.trim()))
                        }
                    )
                }
                SettingsSection.DATA_BACKUP -> {
                    DataBackupSection(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        onExportJson = { viewModel.exportDataJson() },
                        onImportJson = { jsonStr -> viewModel.importJsonData(jsonStr) }
                    )
                }
                SettingsSection.SAMPLE_DATA -> {
                    SettingsSampleDataSection(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        sampleDataStatus = sampleDataStatus,
                        accentColor = accent,
                        onLoadSample = {
                            viewModel.importSampleDataset { count ->
                                sampleDataStatus = "Loaded $count sample scenes successfully!"
                            }
                        },
                        onClearSample = {
                            viewModel.clearSampleDataset { count ->
                                sampleDataStatus = "Cleared $count sample scenes!"
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsMainMenu(
    modifier: Modifier = Modifier,
    themeName: String,
    rdKeyConfigured: Boolean,
    onNavigateTo: (SettingsSection) -> Unit
) {
    Column(modifier = modifier) {
        // Native Android Preferences style items with Icons
        SettingsPreferenceItem(
            icon = Icons.Outlined.Tv,
            title = "Display",
            summary = "Theme ($themeName), Color Palette",
            onClick = { onNavigateTo(SettingsSection.DISPLAY) }
        )

        HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        SettingsPreferenceItem(
            icon = Icons.Outlined.CloudQueue,
            title = "Integrations",
            summary = if (rdKeyConfigured) "Real-Debrid / Torbox (Active)" else "Real-Debrid, Torbox Debrid Services",
            onClick = { onNavigateTo(SettingsSection.INTEGRATIONS) }
        )

        HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        SettingsPreferenceItem(
            icon = Icons.Outlined.Backup,
            title = "Data & Backup",
            summary = "Export & Import JSON database backups",
            onClick = { onNavigateTo(SettingsSection.DATA_BACKUP) }
        )

        HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        SettingsPreferenceItem(
            icon = Icons.Outlined.Dataset,
            title = "Sample dataset",
            summary = "Load or clean removable demo data",
            onClick = { onNavigateTo(SettingsSection.SAMPLE_DATA) }
        )
    }
}

@Composable
private fun SettingsPreferenceItem(
    icon: ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(24.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (summary.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun SettingsDisplaySection(
    modifier: Modifier = Modifier,
    themeName: String,
    onThemeChange: (String) -> Unit,
    accentHex: String,
    onAccentChange: (String) -> Unit,
    showCards: Boolean,
    onShowCardsChange: (Boolean) -> Unit,
    appIconStyle: Int,
    onAppIconStyleChange: (Int) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Theme selection (Native UI with Dark, Amoled, Light)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Theme",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                NativeThemeSelector(
                    selectedTheme = themeName,
                    onSelectTheme = onThemeChange
                )
            }
        }

        // Cards layout toggle for Actors and Studios management
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Cards",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Display cards for Actors and Studios management",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = showCards,
                    onCheckedChange = onShowCardsChange,
                    modifier = Modifier.testTag("cards_management_switch")
                )
            }
        }

        // App Icon Style Picker
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                IconStylePicker(
                    selectedIndex = appIconStyle,
                    onSelectIconStyle = onAppIconStyleChange
                )
            }
        }

        // Color Palette (Material You 3-split circular palette picker)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                ColorPalettePicker(
                    selectedId = accentHex,
                    onSelectPalette = onAccentChange
                )
            }
        }
    }
}

fun switchAppIcon(context: android.content.Context, styleIndex: Int) {
    val packageManager = context.packageManager
    val packageName = context.packageName

    val aliases = listOf(
        "com.example.MainActivityAliasDefault",
        "com.example.MainActivityAliasBlue",
        "com.example.MainActivityAliasOrange",
        "com.example.MainActivityAliasDark",
        "com.example.MainActivityAliasInverted"
    )

    for ((index, alias) in aliases.withIndex()) {
        val componentName = android.content.ComponentName(packageName, alias)
        val newState = if (index == styleIndex) {
            android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        try {
            packageManager.setComponentEnabledSetting(
                componentName,
                newState,
                android.content.pm.PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Programmatically restart the application via the newly enabled alias explicitly to apply the icon change immediately
    try {
        val targetAlias = aliases[styleIndex]
        val intent = android.content.Intent().apply {
            setClassName(packageName, targetAlias)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
        if (context is android.app.Activity) {
            context.finish()
        }
        // Force close and kill to let Android OS apply the launcher icon immediately!
        android.os.Process.killProcess(android.os.Process.myPid())
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
private fun IconStylePicker(
    selectedIndex: Int,
    onSelectIconStyle: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val accent = LocalAccentColor.current
    val options = listOf(
        Triple("Default", Color(0xFF58595e), Color.White),
        Triple("Blue", Color(0xFF3B82F6), Color.White),
        Triple("Orange", Color(0xFFD97706), Color.White),
        Triple("Dark", Color(0xFF1F2937), Color.White),
        Triple("Inverted", Color(0xFFF3F4F6), Color.Black)
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Icons",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, (label, bgColor, fgColor) ->
                val isSelected = selectedIndex == index

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) accent.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable {
                                onSelectIconStyle(index)
                                switchAppIcon(context, index)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(bgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(fgColor, CircleShape)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSampleDataSection(
    modifier: Modifier = Modifier,
    sampleDataStatus: String,
    accentColor: Color,
    onLoadSample: () -> Unit,
    onClearSample: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Sample dataset management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Load realistic sample data (studios, actors, scenes with magnets) or clean them completely from the database",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onLoadSample,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Load Sample")
                    }

                    OutlinedButton(
                        onClick = onClearSample,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All")
                    }
                }

                if (sampleDataStatus.isNotEmpty()) {
                    Text(
                        sampleDataStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF10B981)
                    )
                }
            }
        }
    }
}
