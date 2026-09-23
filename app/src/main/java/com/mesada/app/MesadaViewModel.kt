package com.mesada.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mesada.app.assistant.Assistant
import com.mesada.app.assistant.AssistantTools
import com.mesada.app.assistant.ClaudeException
import com.mesada.app.data.DayState
import com.mesada.app.data.Food
import com.mesada.app.data.FoodCatalog
import com.mesada.app.data.Meal
import com.mesada.app.data.db.ProfileEntity
import com.mesada.app.domain.KitchenTimer
import com.mesada.app.domain.MealIdea
import com.mesada.app.hardware.ScaleConnectionState
import com.mesada.app.voice.SpeechEvent
import com.mesada.app.voice.SpeechInput
import com.mesada.app.voice.Speaker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VoiceMode { IDLE, LISTENING, THINKING }
enum class Role { USER, ASSISTANT, ACTION, ERROR }
enum class GoalField { KCAL, PROTEIN, CARBS, FAT }

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

class MesadaViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as MesadaApp).container
    private val repo = container.repository

    val day: StateFlow<DayState> = repo.observeDay()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayState.empty())

    // Loading al principio (todavía no sabemos si hay perfil guardado); Loaded(null) =
    // sí se consultó la base y no hay perfil, ahí corresponde mostrar el onboarding.
    val profileState: StateFlow<ProfileLoadState> = repo.observeProfile()
        .map { ProfileLoadState.Loaded(it) as ProfileLoadState }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileLoadState.Loading)
    var editingProfile by mutableStateOf(false)
        private set
    fun editProfile() { editingProfile = true }
    fun cancelEditProfile() { editingProfile = false }
    fun saveProfile(profile: ProfileEntity) = viewModelScope.launch {
        repo.saveProfile(profile)
        editingProfile = false
    }

    val timer = KitchenTimer(viewModelScope)
    var selectedMeal by mutableStateOf(Meal.forNow())

    // --- Balanza Bluetooth (opcional) ---
    private val scaleSource = container.scaleSource
    val scaleEnabled: StateFlow<Boolean> = container.hardwareSettings.scaleEnabled
    val scaleConnection: StateFlow<ScaleConnectionState> = scaleSource.connectionState
    val scaleGrams: StateFlow<Double?> = scaleSource.grams
    fun setScaleEnabled(enabled: Boolean) {
        container.hardwareSettings.setScaleEnabled(enabled)
        if (enabled) scaleSource.start() else scaleSource.stop()
    }

    init {
        if (scaleEnabled.value) scaleSource.start()
    }

    private val speaker = Speaker(app)
    private val speech = SpeechInput(app)
    private val brain = Assistant(container.claude, AssistantTools(repo, timer))

    private val _assistant = MutableStateFlow(AssistantUi())
    val assistant: StateFlow<AssistantUi> = _assistant.asStateFlow()
    private var listenJob: Job? = null
    private var askJob: Job? = null
    private var msgId = 0L

    // --- Registro manual ---
    fun addFood(food: Food, qty: Double, meal: Meal = selectedMeal) = viewModelScope.launch { repo.addFood(meal, food, qty) }
    fun remove(id: Long) = viewModelScope.launch { repo.remove(id) }
    fun changeSteps(delta: Int) = viewModelScope.launch { repo.changeSteps(delta = delta) }
    fun clearDay() = viewModelScope.launch { repo.clearToday(); brain.reset() }
    fun addIdea(idea: MealIdea) = viewModelScope.launch {
        idea.items.forEach { (id, q) -> repo.addFood(Meal.DINNER, FoodCatalog.byId.getValue(id), q) }
    }
    fun changeGoal(field: GoalField, delta: Int) = viewModelScope.launch {
        repo.updateGoals { g ->
            when (field) {
                GoalField.KCAL -> g.copy(kcal = (g.kcal + delta).coerceAtLeast(0))
                GoalField.PROTEIN -> g.copy(protein = (g.protein + delta).coerceAtLeast(0))
                GoalField.CARBS -> g.copy(carbs = (g.carbs + delta).coerceAtLeast(0))
                GoalField.FAT -> g.copy(fat = (g.fat + delta).coerceAtLeast(0))
            }
        }
    }

    // --- Asistente de voz ---
    fun openAssistant() = _assistant.update { it.copy(open = true) }
    fun closeAssistant() { stopListening(send = false); _assistant.update { it.copy(open = false) } }
    fun toggleSpeak() {
        val on = !_assistant.value.speak
        speaker.enabled = on
        if (!on) speaker.stop()
        _assistant.update { it.copy(speak = on) }
    }
    fun micPermissionDenied() = post(Role.ERROR, "Sin permiso de micrófono. Podés escribir el pedido abajo.")

    /** Un solo botón: escuchar → terminar y enviar; mientras piensa, detener. */
    fun onMicTapped() {
        when (_assistant.value.mode) {
            VoiceMode.LISTENING -> stopListening(send = true)
            VoiceMode.THINKING -> { askJob?.cancel(); _assistant.update { it.copy(mode = VoiceMode.IDLE) } }
            VoiceMode.IDLE -> startListening()
        }
    }

    private fun startListening() {
        if (!speech.isAvailable) {
            post(Role.ERROR, "Este dispositivo no tiene reconocimiento de voz. Escribí el pedido abajo.")
            return
        }
        speaker.stop()
        _assistant.update { it.copy(mode = VoiceMode.LISTENING, partial = "") }
        listenJob = viewModelScope.launch {
            speech.listen().collect { ev ->
                when (ev) {
                    is SpeechEvent.Partial -> _assistant.update { it.copy(partial = ev.text) }
                    is SpeechEvent.Final -> { _assistant.update { it.copy(mode = VoiceMode.IDLE, partial = "") }; send(ev.text) }
                    is SpeechEvent.Error -> { _assistant.update { it.copy(mode = VoiceMode.IDLE, partial = "") }; post(Role.ERROR, ev.message) }
                }
            }
        }
    }

    private fun stopListening(send: Boolean) {
        val heard = _assistant.value.partial
        listenJob?.cancel(); listenJob = null
        if (_assistant.value.mode == VoiceMode.LISTENING) _assistant.update { it.copy(mode = VoiceMode.IDLE, partial = "") }
        if (send && heard.isNotBlank()) send(heard)
    }

    fun send(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || _assistant.value.mode == VoiceMode.THINKING) return
        post(Role.USER, clean)
        _assistant.update { it.copy(mode = VoiceMode.THINKING) }
        askJob = viewModelScope.launch {
            try {
                val answer = brain.handle(clean) { action -> post(Role.ACTION, action) }
                post(Role.ASSISTANT, answer)
                speaker.speak(answer)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ClaudeException) {
                post(Role.ERROR, when (e.code) {
                    401 -> "Falta configurar la clave de la API (ver README)."
                    429 -> "Demasiados pedidos seguidos. Esperá unos segundos."
                    529, 503 -> "El servicio está saturado. Probá de nuevo en un momento."
                    else -> "El asistente no respondió (error ${e.code})."
                })
            } catch (e: Exception) {
                post(Role.ERROR, "No pude conectarme. Revisá la conexión a internet.")
            } finally {
                _assistant.update { it.copy(mode = VoiceMode.IDLE) }
            }
        }
    }

    private fun post(role: Role, text: String) =
        _assistant.update { it.copy(messages = it.messages + ChatMessage(msgId++, role, text)) }

    override fun onCleared() {
        speaker.shutdown()
        scaleSource.stop()
        super.onCleared()
    }
}
