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

package com.focuslock.app.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.focuslock.app.FocusLockApp
import com.focuslock.app.R
import com.focuslock.app.database.FocusSessionDao
import com.focuslock.app.database.FocusSessionEntity
import com.focuslock.app.overlay.OverlayManager
import com.focuslock.app.presentation.MainActivity
import com.focuslock.app.timer.CountdownEngine
import com.focuslock.app.timer.TimerState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FocusForegroundService : Service() {

    @Inject lateinit var countdownEngine: CountdownEngine
    @Inject lateinit var overlayManager: OverlayManager
    @Inject lateinit var sessionDao: FocusSessionDao
    @Inject lateinit var vibrator: Vibrator

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var currentSessionId: Long = 0L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SESSION -> {
                val durationSec = intent.getLongExtra(EXTRA_DURATION_SECONDS, 0L)
                val tag = intent.getStringExtra(EXTRA_TAG) ?: "Deep Work"
                val note = intent.getStringExtra(EXTRA_NOTE) ?: ""
                val phrase = intent.getStringExtra(EXTRA_PHRASE) ?: "Stay focused."
                startFocusSession(durationSec, tag, note, phrase)
            }
            ACTION_STOP_SESSION -> {
                stopFocusSession(completed = false)
            }
        }
        return START_STICKY
    }

    private fun startFocusSession(durationSeconds: Long, tag: String, note: String, phrase: String) {
        val notification = createForegroundNotification("Focus Lock Active", "00:00:00 remaining")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Save session in Room DB
        serviceScope.launch(Dispatchers.IO) {
            val entity = FocusSessionEntity(
                durationSeconds = durationSeconds,
                remainingSeconds = durationSeconds,
                startTimeMillis = System.currentTimeMillis(),
                tag = tag,
                note = note,
                isCompleted = false
            )
            currentSessionId = sessionDao.insertSession(entity)
        }

        val remainingFlow = MutableStateFlow(durationSeconds)

        // Show window overlay above all apps
        overlayManager.showOverlay(remainingFlow.asStateFlow(), durationSeconds, phrase)

        // Start countdown engine
        countdownEngine.startTimer(durationSeconds) {
            onSessionCompleted()
        }

        // Observe countdown state
        serviceScope.launch {
            countdownEngine.timerState.collect { state ->
                when (state) {
                    is TimerState.Running -> {
                        remainingFlow.value = state.remainingSeconds
                        updateNotification(state.remainingSeconds)
                    }
                    TimerState.Completed -> {
                        onSessionCompleted()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun updateNotification(remainingSec: Long) {
        val hours = remainingSec / 3600
        val minutes = (remainingSec % 3600) / 60
        val seconds = remainingSec % 60
        val formatted = String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)

        val notification = createForegroundNotification("Focus Lock Engaged", "$formatted remaining")
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun onSessionCompleted() {
        // Haptic feedback & vibration
        vibrator.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 300), -1))

        // Mark Room DB completed
        serviceScope.launch(Dispatchers.IO) {
            if (currentSessionId != 0L) {
                val session = sessionDao.getSessionById(currentSessionId)
                session?.let {
                    sessionDao.updateSession(it.copy(isCompleted = true, remainingSeconds = 0))
                }
            }
        }

        stopFocusSession(completed = true)
    }

    private fun stopFocusSession(completed: Boolean) {
        countdownEngine.stopTimer()
        overlayManager.hideOverlay()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        // Launch MainActivity to show completion screen if completed
        if (completed) {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(EXTRA_SHOW_COMPLETION, true)
            }
            startActivity(intent)
        }
    }

    private fun createForegroundNotification(title: String, contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, FocusLockApp.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_SESSION = "com.focuslock.ACTION_START"
        const val ACTION_STOP_SESSION = "com.focuslock.ACTION_STOP"
        const val EXTRA_DURATION_SECONDS = "extra_duration_seconds"
        const val EXTRA_TAG = "extra_tag"
        const val EXTRA_NOTE = "extra_note"
        const val EXTRA_PHRASE = "extra_phrase"
        const val EXTRA_SHOW_COMPLETION = "extra_show_completion"
    }
}
