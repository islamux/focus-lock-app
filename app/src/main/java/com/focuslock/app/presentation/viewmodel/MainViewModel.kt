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

package com.focuslock.app.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.focuslock.app.database.FocusSessionDao
import com.focuslock.app.database.FocusSessionEntity
import com.focuslock.app.services.FocusForegroundService
import com.focuslock.app.timer.CountdownEngine
import com.focuslock.app.timer.TimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UiPermissionState(
    val hasOverlayPermission: Boolean = false,
    val hasAccessibilityPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val countdownEngine: CountdownEngine,
    private val sessionDao: FocusSessionDao
) : AndroidViewModel(application) {

    private val _permissions = MutableStateFlow(UiPermissionState())
    val permissions: StateFlow<UiPermissionState> = _permissions.asStateFlow()

    private val _customPhrase = MutableStateFlow("Stay focused.")
    val customPhrase: StateFlow<String> = _customPhrase.asStateFlow()

    private val prefs = getApplication<Application>()
        .getSharedPreferences("focus_lock_prefs", Context.MODE_PRIVATE)

    init {
        _customPhrase.value = prefs.getString("custom_phrase", "Stay focused.") ?: "Stay focused."
    }

    val timerState: StateFlow<TimerState> = countdownEngine.timerState

    val allSessions: StateFlow<List<FocusSessionEntity>> = sessionDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalFocusTimeSeconds: StateFlow<Long> = sessionDao.getTotalFocusTimeSeconds()
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val completedSessionCount: StateFlow<Int> = sessionDao.getCompletedSessionCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun refreshPermissions() {
        val context = getApplication<Application>()
        val hasOverlay = Settings.canDrawOverlays(context)
        val hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true

        _permissions.value = UiPermissionState(
            hasOverlayPermission = hasOverlay,
            hasAccessibilityPermission = true, // Detected via accessibility service
            hasNotificationPermission = hasNotification
        )
    }

    fun saveCustomPhrase(phrase: String) {
        _customPhrase.value = phrase
        prefs.edit().putString("custom_phrase", phrase).apply()
    }

    fun startFocusSession(hours: Int, minutes: Int, tag: String, note: String) {
        val totalSec = (hours * 3600L) + (minutes * 60L)
        if (totalSec <= 0) return

        val context = getApplication<Application>()
        val intent = Intent(context, FocusForegroundService::class.java).apply {
            action = FocusForegroundService.ACTION_START_SESSION
            putExtra(FocusForegroundService.EXTRA_DURATION_SECONDS, totalSec)
            putExtra(FocusForegroundService.EXTRA_TAG, tag)
            putExtra(FocusForegroundService.EXTRA_NOTE, note)
            putExtra(FocusForegroundService.EXTRA_PHRASE, _customPhrase.value)
        }

        context.startForegroundService(intent)
    }

    fun stopSession() {
        val context = getApplication<Application>()
        val intent = Intent(context, FocusForegroundService::class.java).apply {
            action = FocusForegroundService.ACTION_STOP_SESSION
        }
        context.startService(intent)
    }
}
