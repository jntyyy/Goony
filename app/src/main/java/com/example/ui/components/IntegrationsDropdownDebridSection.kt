package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class DebridServiceOption(
    val id: String,
    val title: String,
    val tokenUrlHint: String
) {
    REAL_DEBRID(
        id = "real_debrid",
        title = "Real-Debrid",
        tokenUrlHint = "Get API token from real-debrid.com/apitoken"
    ),
    TORBOX(
        id = "torbox",
        title = "Torbox",
        tokenUrlHint = "Get API key from torbox.app/settings"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationsDropdownDebridSection(
    realDebridKey: String,
    onRealDebridKeyChange: (String) -> Unit,
    torboxKey: String,
    onTorboxKeyChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedService by remember {
        mutableStateOf(
            if (realDebridKey.isNotBlank() || torboxKey.isBlank()) DebridServiceOption.REAL_DEBRID
            else DebridServiceOption.TORBOX
        )
    }

    var isDropdownExpanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    var showApiKey by remember { mutableStateOf(false) }

    val activeKey = when (selectedService) {
        DebridServiceOption.REAL_DEBRID -> realDebridKey
        DebridServiceOption.TORBOX -> torboxKey
    }

    val onActiveKeyChange: (String) -> Unit = { newKey ->
        when (selectedService) {
            DebridServiceOption.REAL_DEBRID -> onRealDebridKeyChange(newKey)
            DebridServiceOption.TORBOX -> onTorboxKeyChange(newKey)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Debrid Service",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 1. Native Exposed Dropdown Menu Box
                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedService.title,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Debrid Service") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Cloud,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(start = 10.dp)
                                    .size(22.dp)
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded)
                        },
                        shape = RoundedCornerShape(28.dp),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        DebridServiceOption.values().forEach { option ->
                            val isCurrentSelected = selectedService == option
                            val isOptionConfigured = when (option) {
                                DebridServiceOption.REAL_DEBRID -> realDebridKey.isNotBlank()
                                DebridServiceOption.TORBOX -> torboxKey.isNotBlank()
                            }

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = option.title,
                                            fontWeight = if (isCurrentSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCurrentSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isOptionConfigured) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF10B981).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "Active",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF10B981),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Cloud,
                                        contentDescription = null,
                                        tint = if (isCurrentSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    selectedService = option
                                    isDropdownExpanded = false
                                },
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }

                // 2. API Key Field with Paste Action & Quick Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${selectedService.title} API Key",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Native Rounded Paste Button
                    FilledTonalButton(
                        onClick = {
                            clipboardManager.getText()?.text?.let { clipboardText ->
                                if (clipboardText.isNotBlank()) {
                                    onActiveKeyChange(clipboardText.trim())
                                }
                            }
                        },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_app_paste),
                            contentDescription = "Paste",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                OutlinedTextField(
                    value = activeKey,
                    onValueChange = onActiveKeyChange,
                    placeholder = { Text("Paste ${selectedService.title} API token here...", fontSize = 14.sp) },
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, lineHeight = 20.sp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .size(22.dp)
                        )
                    },
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showApiKey) "Hide API Key" else "Show API Key"
                            )
                        }
                    },
                    singleLine = true,
                    maxLines = 1,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("debrid_api_key_input")
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = if (activeKey.isNotBlank()) "✓ Connected and saved" else selectedService.tokenUrlHint,
                        color = if (activeKey.isNotBlank()) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                if (activeKey.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { onActiveKeyChange("") },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Clear Token", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
