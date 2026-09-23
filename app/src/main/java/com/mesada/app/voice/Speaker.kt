package com.mesada.app.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class Speaker(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false
    var enabled = true

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val ar = tts.setLanguage(Locale("es", "AR"))
        if (ar == TextToSpeech.LANG_MISSING_DATA || ar == TextToSpeech.LANG_NOT_SUPPORTED) tts.setLanguage(Locale("es"))
        ready = true
    }

    fun speak(text: String) {
        if (enabled && ready) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mesada-${System.nanoTime()}")
    }

    fun stop() { if (ready) tts.stop() }

    fun shutdown() = tts.shutdown()
}
