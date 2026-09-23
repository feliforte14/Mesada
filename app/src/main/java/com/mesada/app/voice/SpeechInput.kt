package com.mesada.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed interface SpeechEvent {
    data class Partial(val text: String) : SpeechEvent
    data class Final(val text: String) : SpeechEvent
    data class Error(val message: String) : SpeechEvent
}

/** Envuelve SpeechRecognizer en un Flow. Recolectar en el hilo principal (viewModelScope ya lo es). */
class SpeechInput(private val context: Context) {

    val isAvailable: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun listen(language: String = "es-AR"): Flow<SpeechEvent> = callbackFlow {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onPartialResults(partial: Bundle?) {
                partial.firstResult()?.let { trySend(SpeechEvent.Partial(it)) }
            }
            override fun onResults(results: Bundle?) {
                val text = results.firstResult().orEmpty()
                trySend(if (text.isBlank()) SpeechEvent.Error("No te escuché. Probá de nuevo.") else SpeechEvent.Final(text))
                close()
            }
            override fun onError(error: Int) {
                trySend(SpeechEvent.Error(messageFor(error)))
                close()
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer.startListening(intent)
        awaitClose { recognizer.destroy() }
    }

    private fun Bundle?.firstResult(): String? =
        this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

    private fun messageFor(code: Int) = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No te escuché. Probá de nuevo."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Falta el permiso de micrófono."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Sin conexión para reconocer la voz."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "El micrófono está ocupado. Probá en un segundo."
        else -> "No pude reconocer la voz (código $code)."
    }
}
