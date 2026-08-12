package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.GeminiMessageEntity
import com.example.data.local.GeminiPresetEntity
import com.example.data.local.GeminiSessionEntity
import com.example.data.repository.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

enum class LiveVoiceState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

class GeminiLiveViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val repository = GeminiRepository(application)

    val sessions: StateFlow<List<GeminiSessionEntity>> = MutableStateFlow(emptyList())
    val presets: StateFlow<List<GeminiPresetEntity>> = MutableStateFlow(emptyList())

    private val _activeSessionId = MutableStateFlow<Long?>(null)
    val activeSessionId: StateFlow<Long?> = _activeSessionId.asStateFlow()

    private val _activeSession = MutableStateFlow<GeminiSessionEntity?>(null)
    val activeSession: StateFlow<GeminiSessionEntity?> = _activeSession.asStateFlow()

    private val _messages = MutableStateFlow<List<GeminiMessageEntity>>(emptyList())
    val messages: StateFlow<List<GeminiMessageEntity>> = _messages.asStateFlow()

    private val _liveVoiceState = MutableStateFlow(LiveVoiceState.IDLE)
    val liveVoiceState: StateFlow<LiveVoiceState> = _liveVoiceState.asStateFlow()

    private val _isTtsMuted = MutableStateFlow(false)
    val isTtsMuted: StateFlow<Boolean> = _isTtsMuted.asStateFlow()

    private val _selectedVoiceName = MutableStateFlow("Kore")
    val selectedVoiceName: StateFlow<String> = _selectedVoiceName.asStateFlow()

    private val _attachedBitmap = MutableStateFlow<Bitmap?>(null)
    val attachedBitmap: StateFlow<Bitmap?> = _attachedBitmap.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        tts = TextToSpeech(application, this)

        viewModelScope.launch {
            repository.allSessions.collect { list ->
                (sessions as MutableStateFlow).value = list
                if (_activeSessionId.value == null && list.isNotEmpty()) {
                    selectSession(list.first().id)
                } else if (list.isEmpty()) {
                    // Auto-create default session
                    createNewSession("Gemini Live Chat", "Live Voice", "Kore")
                }
            }
        }

        viewModelScope.launch {
            repository.allPresets.collect { list ->
                (presets as MutableStateFlow).value = list
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsReady = true
                setupTtsListener()
            }
        }
    }

    private fun setupTtsListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _liveVoiceState.value = LiveVoiceState.SPEAKING
            }

            override fun onDone(utteranceId: String?) {
                _liveVoiceState.value = LiveVoiceState.IDLE
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _liveVoiceState.value = LiveVoiceState.IDLE
            }
        })
    }

    fun selectSession(sessionId: Long) {
        _activeSessionId.value = sessionId
        viewModelScope.launch {
            val session = repository.getSessionById(sessionId)
            _activeSession.value = session
            if (session != null) {
                _selectedVoiceName.value = session.voiceName
            }
            repository.getMessagesForSession(sessionId).collect { msgList ->
                _messages.value = msgList
            }
        }
    }

    fun createNewSession(
        title: String,
        mode: String = "Live Voice",
        voiceName: String = "Kore",
        systemInstruction: String = "You are Gemini Live, a friendly, ultra-fast, intelligent AI assistant."
    ) {
        viewModelScope.launch {
            val newId = repository.createSession(title, mode, voiceName, systemInstruction)
            selectSession(newId)
        }
    }

    fun setVoiceName(voiceName: String) {
        _selectedVoiceName.value = voiceName
        val currentSession = _activeSession.value
        if (currentSession != null) {
            viewModelScope.launch {
                repository.updateSession(currentSession.copy(voiceName = voiceName))
                _activeSession.value = currentSession.copy(voiceName = voiceName)
            }
        }
    }

    fun attachImage(bitmap: Bitmap?) {
        _attachedBitmap.value = bitmap
    }

    fun toggleMuteTts() {
        _isTtsMuted.value = !_isTtsMuted.value
        if (_isTtsMuted.value && tts?.isSpeaking == true) {
            tts?.stop()
            _liveVoiceState.value = LiveVoiceState.IDLE
        }
    }

    fun stopSpeaking() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
            _liveVoiceState.value = LiveVoiceState.IDLE
        }
    }

    fun sendPrompt(promptText: String) {
        val sessionId = _activeSessionId.value ?: return
        if (promptText.trim().isEmpty() && _attachedBitmap.value == null) return

        val currentBitmap = _attachedBitmap.value
        _attachedBitmap.value = null // clear attached image
        _isLoading.value = true
        _errorMessage.value = null
        _liveVoiceState.value = LiveVoiceState.THINKING

        viewModelScope.launch {
            val session = repository.getSessionById(sessionId)
            val sysInstruction = session?.systemInstruction
            val voice = _selectedVoiceName.value

            val result = repository.sendPrompt(
                sessionId = sessionId,
                promptText = promptText,
                bitmap = currentBitmap,
                history = _messages.value,
                systemInstruction = sysInstruction,
                voiceName = voice,
                requestVoiceMode = true
            )

            _isLoading.value = false

            result.onSuccess { responseText ->
                _liveVoiceState.value = LiveVoiceState.IDLE
                if (!_isTtsMuted.value && responseText.isNotBlank()) {
                    speakText(responseText)
                }
            }.onFailure { exception ->
                _liveVoiceState.value = LiveVoiceState.IDLE
                _errorMessage.value = exception.message ?: "An error occurred."
            }
        }
    }

    fun speakText(text: String) {
        if (!isTtsReady || _isTtsMuted.value) return
        stopSpeaking()

        // Customize TTS pitch & pitch based on voice name preset
        when (_selectedVoiceName.value) {
            "Kore" -> { tts?.setPitch(1.0f); tts?.setSpeechRate(1.0f) }
            "Aoede" -> { tts?.setPitch(1.15f); tts?.setSpeechRate(1.05f) }
            "Charon" -> { tts?.setPitch(0.85f); tts?.setSpeechRate(0.95f) }
            "Fenrir" -> { tts?.setPitch(0.75f); tts?.setSpeechRate(1.0f) }
            "Puck" -> { tts?.setPitch(1.25f); tts?.setSpeechRate(1.1f) }
            "Zephyr" -> { tts?.setPitch(0.95f); tts?.setSpeechRate(1.0f) }
        }

        // Clean markdown syntax before speaking
        val cleanText = text.replace(Regex("[#*_`]"), "").take(1000)
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "gemini_voice_${System.currentTimeMillis()}")
    }

    fun applyPreset(preset: GeminiPresetEntity) {
        viewModelScope.launch {
            val title = "Gemini: ${preset.title}"
            val newId = repository.createSession(
                title = title,
                mode = preset.category,
                voiceName = preset.defaultVoice,
                systemInstruction = preset.systemInstruction
            )
            selectSession(newId)
            sendPrompt(preset.samplePrompt)
        }
    }

    fun addCustomPreset(
        title: String,
        description: String,
        category: String,
        systemInstruction: String,
        defaultVoice: String,
        samplePrompt: String
    ) {
        viewModelScope.launch {
            val newPreset = GeminiPresetEntity(
                title = title,
                description = description,
                category = category,
                systemInstruction = systemInstruction,
                defaultVoice = defaultVoice,
                samplePrompt = samplePrompt,
                isCustom = true
            )
            repository.addPreset(newPreset)
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_activeSessionId.value == sessionId) {
                _activeSessionId.value = null
                _activeSession.value = null
                _messages.value = emptyList()
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setListeningState(isListening: Boolean) {
        if (isListening) {
            stopSpeaking()
            _liveVoiceState.value = LiveVoiceState.LISTENING
        } else if (_liveVoiceState.value == LiveVoiceState.LISTENING) {
            _liveVoiceState.value = LiveVoiceState.IDLE
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}
