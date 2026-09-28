package com.mesada.app

import com.mesada.app.data.db.ProfileEntity

/** Modelos de estado de UI que expone MesadaViewModel; sin lógica, solo forma de los datos. */

enum class VoiceMode { IDLE, LISTENING, THINKING }
enum class Role { USER, ASSISTANT, ACTION, ERROR }

sealed interface ProfileLoadState {
    data object Loading : ProfileLoadState
    data class Loaded(val profile: ProfileEntity?) : ProfileLoadState
}

data class ChatMessage(val id: Long, val role: Role, val text: String)

data class AssistantUi(
    val open: Boolean = false,
    val mode: VoiceMode = VoiceMode.IDLE,
    val partial: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val speak: Boolean = true,
)
