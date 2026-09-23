package com.mesada.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Preferencia persistida: si la app intenta usar la balanza Bluetooth o solo el software. */
class HardwareSettings(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _scaleEnabled = MutableStateFlow(prefs.getBoolean(KEY_SCALE_ENABLED, false))
    val scaleEnabled: StateFlow<Boolean> = _scaleEnabled

    fun setScaleEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCALE_ENABLED, enabled).apply()
        _scaleEnabled.value = enabled
    }

    companion object {
        private const val PREFS_NAME = "mesada_settings"
        private const val KEY_SCALE_ENABLED = "hardware_scale_enabled"
    }
}
