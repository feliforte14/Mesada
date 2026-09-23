package com.mesada.app

import android.app.Application
import android.content.Context
import com.mesada.app.assistant.ClaudeClient
import com.mesada.app.data.HardwareSettings
import com.mesada.app.data.MesadaRepository
import com.mesada.app.data.db.MesadaDatabase
import com.mesada.app.hardware.BleScaleSource
import com.mesada.app.hardware.ScaleSource

/** Contenedor de dependencias manual (suficiente para un prototipo; migrable a Hilt). */
class AppContainer(context: Context) {
    private val db = MesadaDatabase.build(context)
    val repository = MesadaRepository(db.dao())
    val claude = ClaudeClient(BuildConfig.ASSISTANT_BASE_URL, BuildConfig.ANTHROPIC_API_KEY, BuildConfig.CLAUDE_MODEL)

    // Módulo de hardware opcional: sin celda de carga + ESP32, hardwareSettings.scaleEnabled
    // queda en false y scaleSource nunca toca ninguna API de Bluetooth (ver BleScaleSource.start).
    val hardwareSettings = HardwareSettings(context)
    val scaleSource: ScaleSource = BleScaleSource(context, hardwareSettings)
}

class MesadaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
