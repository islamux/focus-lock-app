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

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.focuslock.app.overlay.OverlayManager
import com.focuslock.app.timer.CountdownEngine
import com.focuslock.app.timer.TimerState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FocusAccessibilityService : AccessibilityService() {

    @Inject lateinit var overlayManager: OverlayManager
    @Inject lateinit var countdownEngine: CountdownEngine

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Check if countdown is currently running
        if (countdownEngine.timerState.value is TimerState.Running) {
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    val packageName = event.packageName?.toString() ?: ""
                    // If user attempts to navigate away to another app launcher/settings
                    if (packageName != applicationContext.packageName) {
                        // Force overlay visible and bring Focus Lock back to foreground
                        if (!overlayManager.isOverlayShowing()) {
                            overlayManager.showOverlay(
                                kotlinx.coroutines.flow.MutableStateFlow(countdownEngine.getRemainingSeconds()),
                                countdownEngine.getRemainingSeconds()
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        // Service interrupted
    }
}
