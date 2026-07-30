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

package com.focuslock.app

import com.focuslock.app.timer.CountdownEngine
import com.focuslock.app.timer.TimerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CountdownEngineTest {

    private lateinit var countdownEngine: CountdownEngine

    @Before
    fun setUp() {
        countdownEngine = CountdownEngine()
    }

    @Test
    fun timerStartsInIdleState() {
        val state = countdownEngine.timerState.value
        assertTrue(state is TimerState.Idle)
    }

    @Test
    fun stopTimerResetsStateToIdle() = runTest {
        countdownEngine.startTimer(300) {}
        countdownEngine.stopTimer()
        assertTrue(countdownEngine.timerState.value is TimerState.Idle)
    }
}
