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

package com.focuslock.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.focuslock.app.presentation.screens.FocusOverlayContent
import com.focuslock.app.presentation.theme.FocusLockTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverlayManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var overlayView: View? = null
    private var isShowing = false
    var currentTotalSeconds: Long = 0L
    var currentPhrase: String = "Stay focused."

    fun showOverlay(
        remainingSecondsFlow: kotlinx.coroutines.flow.StateFlow<Long>,
        totalSeconds: Long,
        phrase: String? = null
    ) {
        currentTotalSeconds = totalSeconds
        if (phrase != null) currentPhrase = phrase
        if (isShowing) return

        // FLAG_FULLSCREEN / FLAG_SHOW_WHEN_LOCKED / FLAG_DISMISS_KEYGUARD have no modern
        // equivalents for WindowManager overlay windows (their replacements are Activity-scoped).
        @Suppress("DEPRECATION")
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.FILL
        }

        val lifecycleOwner = OverlayLifecycleOwner()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                FocusLockTheme {
                    FocusOverlayContent(
                        remainingSecondsFlow = remainingSecondsFlow,
                        totalSeconds = currentTotalSeconds,
                        phrase = currentPhrase
                    )
                }
            }
        }

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        try {
            windowManager.addView(composeView, layoutParams)
            overlayView = composeView
            isShowing = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hideOverlay() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        overlayView = null
        isShowing = false
    }

    fun isOverlayShowing(): Boolean = isShowing
}

/**
 * Custom ViewTreeLifecycleOwner for injecting Jetpack Compose into WindowManager.
 */
private class OverlayLifecycleOwner : ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = androidx.lifecycle.LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    fun performRestore(savedState: android.os.Bundle?) {
        savedStateRegistryController.performRestore(savedState)
    }
}
