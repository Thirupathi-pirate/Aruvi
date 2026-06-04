package com.telegramtv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.telegramtv.ui.components.TVButton
import com.telegramtv.ui.components.TVIconButton
import com.telegramtv.ui.theme.*

/**
 * Settings screen for app configuration.
 */
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(TVBackground, TVSurface, TVBackground)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp)
        ) {
            // Header
            SettingsHeader(onBackClick = onBackClick)

            Spacer(modifier = Modifier.height(48.dp))

            // Settings sections
            Column(
                modifier = Modifier 
                    .fillMaxWidth(0.6f)
                    .align(Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // User info section
                if (uiState.userName != null) {
                    SettingsSection(title = "Account") {
                        SettingsItem(
                            label = "Logged in as",
                            value = uiState.userName!!
                        )
                    }
                }

                // Playback settings
                SettingsSection(title = "Playback") {
                    SettingsToggle(
                        label = "Auto-play next file",
                        checked = uiState.autoPlayNext,
                        onCheckedChange = { viewModel.toggleAutoPlayNext() }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Logout button - raw Box to avoid Material/TV button theming issues
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(
                            color = Color(0xFF333333),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.showLogoutConfirm() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Logout",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }

        // Logout confirmation dialog
        if (uiState.showLogoutConfirm) {
            LogoutConfirmDialog(
                onConfirm = { viewModel.logout { onLogout() } },
                onDismiss = { viewModel.hideLogoutConfirm() }
            )
        }
    }
}

/**
 * Settings header with back button.
 */
@Composable
private fun SettingsHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TVIconButton(
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TVTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            },
            onClick = onBackClick,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            tint = TVPrimary,
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            color = TVTextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Settings section with title.
 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = TVPrimary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TVSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content
            )
        }
    }
}

/**
 * Read-only settings item.
 */
@Composable
private fun SettingsItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = TVTextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = TVTextPrimary
        )
    }
}

/**
 * Settings text field.
 */
@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = ""
) {
    var isFocused by remember { mutableStateOf(false) }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TVTextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(TVSurfaceVariant, RoundedCornerShape(10.dp))
                .border(
                    width = if (isFocused) 2.dp else 0.dp,
                    color = if (isFocused) TVPrimary else Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = TVTextPrimary
                ),
                singleLine = true,
                cursorBrush = SolidColor(TVPrimary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TVTextSecondary
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

/**
 * Settings toggle switch.
 */
@Composable
private fun SettingsToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = TVTextPrimary
        )

        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = TVPrimary,
                checkedTrackColor = TVPrimary.copy(alpha = 0.5f),
                uncheckedThumbColor = TVTextSecondary,
                uncheckedTrackColor = TVSurfaceVariant
            )
        )
    }
}

/**
 * Logout confirmation dialog.
 */
@Composable
private fun LogoutConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Logout",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "Are you sure you want to logout?",
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Logout", color = TVError)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = TVSurface
    )
}
