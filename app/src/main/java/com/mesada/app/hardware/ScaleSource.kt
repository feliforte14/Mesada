package com.mesada.app.hardware

import kotlinx.coroutines.flow.StateFlow

enum class ScaleConnectionState { DISABLED, DISCONNECTED, CONNECTING, CONNECTED }

/**
 * Fuente de peso medido por hardware externo (la balanza Bluetooth). Es opcional: el resto de
 * la app (registro manual, asistente de voz con gramos estimados, etc.) funciona sin que nada
 * implemente esta interfaz. [ScaleConnectionState.DISABLED] es el estado por defecto y el único
 * válido cuando el usuario no activó la balanza — en ese estado no se toca ninguna API de
 * Bluetooth.
 */
interface ScaleSource {
    val connectionState: StateFlow<ScaleConnectionState>
    val grams: StateFlow<Double?>

    fun start()
    fun stop()
}
