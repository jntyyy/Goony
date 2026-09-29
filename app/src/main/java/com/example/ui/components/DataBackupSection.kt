package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DataBackupSection(
    onExportJson: suspend () -> String,
    onImportJson: suspend (String) -> Result<Int>,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var pendingExportData by remember { mutableStateOf("") }
    var exportStatusMessage by remember { mutableStateOf<String?>(null) }
    var isExportError by remember { mutableStateOf(false) }

    var importStatusMessage by remember { mutableStateOf<String?>(null) }
    var isImportError by remember { mutableStateOf(false) }

    // SAF CreateDocument Launcher for Export (Direct JSON file download/save)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { fileUri ->
            coroutineScope.launch {
                try {
                    context.contentResolver.openOutputStream(fileUri)?.use { stream ->
                        stream.write(pendingExportData.toByteArray(Charsets.UTF_8))
                    }
                    exportStatusMessage = "JSON backup file exported and downloaded successfully!"
                    isExportError = false
                } catch (e: Exception) {
                    exportStatusMessage = "Failed to export file: ${e.localizedMessage ?: "Unknown error"}"
                    isExportError = true
                }
            }
        }
    }

    // SAF OpenDocument Launcher for Import (Direct JSON file upload/selection)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { fileUri ->
            coroutineScope.launch {
                try {
                    val jsonContent = context.contentResolver.openInputStream(fileUri)?.use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).readText()
                    }
                    if (!jsonContent.isNullOrBlank()) {
                        val result = onImportJson(jsonContent)
                        if (result.isSuccess) {
                            val count = result.getOrDefault(0)
                            importStatusMessage = "Successfully imported $count items from JSON file!"
                            isImportError = false
                        } else {
                            importStatusMessage = "Failed to import JSON file: Invalid database format"
                            isImportError = true
                        }
                    } else {
                        importStatusMessage = "The selected JSON file is empty"
                        isImportError = true
                    }
                } catch (e: Exception) {
                    importStatusMessage = "Failed to read file: ${e.localizedMessage ?: "Unknown error"}"
                    isImportError = true
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Export Card (JSON File Download)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.FileDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Export Data",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Save database backup directly as a downloadable JSON file",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalButton(
                    onClick = {
                        coroutineScope.launch {
                            val json = onExportJson()
                            pendingExportData = json
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            exportLauncher.launch("goony_backup_$timestamp.json")
                        }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export & Download JSON", fontWeight = FontWeight.SemiBold)
                }

                exportStatusMessage?.let { status ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isExportError) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = if (isExportError) MaterialTheme.colorScheme.error else Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isExportError) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                        )
                    }
                }
            }
        }

        // 2. Import Card (JSON File Upload)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.FileUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Import Data",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Restore or add records to database by uploading a JSON file",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = {
                        importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload & Import JSON File", fontWeight = FontWeight.SemiBold)
                }

                importStatusMessage?.let { status ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isImportError) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = if (isImportError) MaterialTheme.colorScheme.error else Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isImportError) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                        )
                    }
                }
            }
        }
    }
}
