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

package com.focuslock.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkSlateBackground = Color(0xFF0D0F14)
val DarkCardSurface = Color(0xFF161B22)
val NeonCyanPrimary = Color(0xFF00F0FF)
val IndigoAccent = Color(0xFF6366F1)
val EmeraldSuccess = Color(0xFF10B981)
val TextHighEmphasis = Color(0xFFF3F4F6)
val TextMediumEmphasis = Color(0xFF9CA3AF)

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyanPrimary,
    secondary = IndigoAccent,
    background = DarkSlateBackground,
    surface = DarkCardSurface,
    onPrimary = Color.Black,
    onBackground = TextHighEmphasis,
    onSurface = TextHighEmphasis
)

@Composable
fun FocusLockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
