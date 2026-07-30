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

package com.focuslock.app.timer

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed class TimerState {
    object Idle : TimerState()
    data class Running(val remainingSeconds: Long, val totalSeconds: Long, val progress: Float) : TimerState()
    object Completed : TimerState()
}

@Singleton
class CountdownEngine @Inject constructor() {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var timerJob: Job? = null

    private var targetRealtimeMs: Long = 0L
    private var totalDurationSeconds: Long = 0L

    private val _timerState = MutableStateFlow<TimerState>(TimerState.Idle)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    /**
     * Start countdown with immunity against System.currentTimeMillis() time changes.
     * Uses SystemClock.elapsedRealtime() to track true physical hardware runtime.
     */
    fun startTimer(durationSeconds: Long, onFinish: () -> Unit) {
        timerJob?.cancel()

        totalDurationSeconds = durationSeconds
        // Calculate target realtime anchor
        targetRealtimeMs = SystemClock.elapsedRealtime() + (durationSeconds * 1000L)

        timerJob = scope.launch {
            while (true) {
                val currentRealtime = SystemClock.elapsedRealtime()
                val remainingMs = targetRealtimeMs - currentRealtime
                val remainingSec = Math.max(0L, (remainingMs + 999L) / 1000L)

                if (remainingSec <= 0) {
                    _timerState.value = TimerState.Completed
                    onFinish()
                    break
                } else {
                    val progress = 1f - (remainingSec.toFloat() / totalDurationSeconds.toFloat())
                    _timerState.value = TimerState.Running(
                        remainingSeconds = remainingSec,
                        totalSeconds = totalDurationSeconds,
                        progress = progress.coerceIn(0f, 1f)
                    )
                }

                // Precision sync sleep every 500ms
                delay(500)
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _timerState.value = TimerState.Idle
    }

    fun getRemainingSeconds(): Long {
        val currentRealtime = SystemClock.elapsedRealtime()
        val remainingMs = targetRealtimeMs - currentRealtime
        return Math.max(0L, (remainingMs + 999L) / 1000L)
    }
}
