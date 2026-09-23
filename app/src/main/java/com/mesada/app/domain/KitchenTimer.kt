package com.mesada.app.domain

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TimerState(val totalSec: Int = 300, val leftSec: Int = 300, val running: Boolean = false) {
    val finished: Boolean get() = leftSec == 0 && !running
    val label: String get() = "%02d:%02d".format(leftSec / 60, leftSec % 60)
}

class KitchenTimer(private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()
    private var job: Job? = null

    fun set(minutes: Double, autoStart: Boolean = false) {
        pause()
        val s = (minutes * 60).toInt().coerceAtLeast(1)
        _state.value = TimerState(s, s, false)
        if (autoStart) start()
    }

    fun toggle() = if (_state.value.running) pause() else start()

    fun start() {
        if (_state.value.leftSec <= 0) _state.update { it.copy(leftSec = it.totalSec) }
        _state.update { it.copy(running = true) }
        job?.cancel()
        job = scope.launch {
            while (isActive && _state.value.leftSec > 0) {
                delay(1000)
                _state.update { it.copy(leftSec = it.leftSec - 1) }
            }
            _state.update { it.copy(running = false) }
            if (_state.value.leftSec == 0) alarm()
        }
    }

    fun pause() {
        job?.cancel(); job = null
        _state.update { it.copy(running = false) }
    }

    fun reset() {
        pause()
        _state.update { it.copy(leftSec = it.totalSec) }
    }

    private fun alarm() = runCatching {
        val tone = ToneGenerator(AudioManager.STREAM_ALARM, 90)
        scope.launch {
            try {
                repeat(3) { tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 400); delay(700) }
            } finally {
                tone.release()
            }
        }
    }
}
