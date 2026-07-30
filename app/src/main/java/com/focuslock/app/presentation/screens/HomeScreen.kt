// Focus Lock
// Copyright (C) 2026 islamux
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package com.focuslock.app.presentation.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuslock.app.presentation.theme.DarkCardSurface
import com.focuslock.app.presentation.theme.DarkSlateBackground
import com.focuslock.app.presentation.theme.EmeraldSuccess
import com.focuslock.app.presentation.theme.NeonCyanPrimary
import com.focuslock.app.presentation.theme.TextHighEmphasis
import com.focuslock.app.presentation.theme.TextMediumEmphasis
import com.focuslock.app.presentation.viewmodel.MainViewModel
import com.focuslock.app.timer.TimerState

private val PRESET_MINUTES = listOf(15, 25, 50, 90)

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val permissions by viewModel.permissions.collectAsState()
    val totalSeconds by viewModel.totalFocusTimeSeconds.collectAsState()
    val completedCount by viewModel.completedSessionCount.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    val customPhrase by viewModel.customPhrase.collectAsState()

    val isRunning = timerState is TimerState.Running
    var selectedMinutes by remember { mutableIntStateOf(25) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshPermissions() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkSlateBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Focus Lock",
                color = TextHighEmphasis,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            PermissionStatusCard(
                hasOverlay = permissions.hasOverlayPermission,
                hasNotifications = permissions.hasNotificationPermission,
                hasAccessibility = permissions.hasAccessibilityPermission,
                onRequestOverlay = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                },
                onRequestNotification = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onRequestAccessibility = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCardSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Start a session",
                        color = TextHighEmphasis,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = customPhrase,
                        onValueChange = { viewModel.saveCustomPhrase(it) },
                        label = { Text("Your focus phrase") },
                        singleLine = true,
                        enabled = !isRunning,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = TextHighEmphasis,
                            unfocusedTextColor = TextHighEmphasis,
                            cursorColor = NeonCyanPrimary,
                            focusedLabelColor = NeonCyanPrimary,
                            unfocusedLabelColor = TextMediumEmphasis,
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PRESET_MINUTES.forEach { minutes ->
                            OutlinedButton(
                                onClick = { selectedMinutes = minutes },
                                enabled = !isRunning,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selectedMinutes == minutes) NeonCyanPrimary.copy(alpha = 0.18f) else Color.Transparent,
                                    contentColor = NeonCyanPrimary
                                )
                            ) {
                                Text(text = "${minutes}m", fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.startFocusSession(
                                hours = 0,
                                minutes = selectedMinutes,
                                tag = "Deep Work",
                                note = ""
                            )
                        },
                        enabled = !isRunning && permissions.hasOverlayPermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyanPrimary,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(text = "Lock In — ${selectedMinutes} min", fontWeight = FontWeight.Bold)
                    }

                    if (isRunning) {
                        Button(
                            onClick = { viewModel.stopSession() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text(text = "End session")
                        }
                    }
                }
            }

            StatsCard(
                completedCount = completedCount,
                totalSeconds = totalSeconds
            )
        }
    }
}

@Composable
private fun PermissionStatusCard(
    hasOverlay: Boolean,
    hasNotifications: Boolean,
    hasAccessibility: Boolean,
    onRequestOverlay: () -> Unit,
    onRequestNotification: () -> Unit,
    onRequestAccessibility: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !hasOverlay) { onRequestOverlay() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = if (hasOverlay) EmeraldSuccess else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = if (hasOverlay) "Overlay permission granted" else "Tap to grant overlay permission",
                    color = TextMediumEmphasis,
                    fontSize = 14.sp
                )
            }
            HorizontalDivider(color = TextMediumEmphasis.copy(alpha = 0.15f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !hasNotifications) { onRequestNotification() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = if (hasNotifications) EmeraldSuccess else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = if (hasNotifications) "Notifications enabled" else "Tap to enable notifications",
                    color = TextMediumEmphasis,
                    fontSize = 14.sp
                )
            }
            HorizontalDivider(color = TextMediumEmphasis.copy(alpha = 0.15f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !hasAccessibility) { onRequestAccessibility() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = if (hasAccessibility) EmeraldSuccess else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = if (hasAccessibility) "Accessibility service enabled" else "Tap to enable accessibility service",
                    color = TextMediumEmphasis,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun StatsCard(completedCount: Int, totalSeconds: Long) {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatColumn(value = "$completedCount", label = "Sessions")
            StatColumn(value = "${hours}h ${minutes}m", label = "Total focus")
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = NeonCyanPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.size(4.dp))
        Text(text = label, color = TextMediumEmphasis, fontSize = 12.sp)
    }
}
