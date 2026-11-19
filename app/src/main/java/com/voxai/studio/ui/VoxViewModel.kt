package com.voxai.studio.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voxai.studio.engine.VoxAudioEngine
import com.voxai.studio.model.VoiceCategory
import com.voxai.studio.model.VoxUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VoxViewModel(application: Application) : AndroidViewModel(application) {
  private val prefs = application.getSharedPreferences("vox_ai_prefs", Context.MODE_PRIVATE)
  private val engine = VoxAudioEngine(application)

  private val _uiState = MutableStateFlow(
    VoxUiState(elevenLabsApiKey = prefs.getString("eleven_api_key", "") ?: "")
  )
  val uiState: StateFlow<VoxUiState> = _uiState.asStateFlow()

  fun updateScript(text: String) {
    if (text.length <= 5000) _uiState.update { it.copy(scriptText = text) }
  }

  fun appendEmotionTag(tag: String) {
    _uiState.update {
      val space = if (it.scriptText.endsWith(" ") || it.scriptText.isEmpty()) "" else " "
      it.copy(scriptText = (it.scriptText + space + tag + " ").take(5000))
    }
  }

  fun selectCategory(cat: VoiceCategory) = _uiState.update { it.copy(selectedCategory = cat) }
  fun selectVoice(id: String) = _uiState.update { it.copy(selectedVoiceId = id) }
  fun updateStability(v: Float) = _uiState.update { it.copy(stability = v) }
  fun updateClarity(v: Float) = _uiState.update { it.copy(clarity = v) }
  fun updateSpeed(v: Float) = _uiState.update { it.copy(speed = v) }
  fun updatePitch(v: Float) = _uiState.update { it.copy(pitch = v) }

  fun toggleApiKeyDialog(show: Boolean) = _uiState.update { it.copy(showApiKeyDialog = show) }

  fun saveApiKey(key: String) {
    prefs.edit().putString("eleven_api_key", key.trim()).apply()
    _uiState.update {
      it.copy(
        elevenLabsApiKey = key.trim(),
        showApiKeyDialog = false,
        statusMessage = if (key.isBlank()) "Switched to Free Offline Neural Engine" else "ElevenLabs API Key Saved!",
      )
    }
  }

  fun clearStatus() = _uiState.update { it.copy(statusMessage = null) }

  fun generateSpeech() {
    val state = _uiState.value
    if (state.isGenerating) return
    viewModelScope.launch {
      _uiState.update { it.copy(isGenerating = true, statusMessage = null) }
      val result = engine.generateVoice(
        text = state.scriptText,
        voice = state.selectedVoice,
        stability = state.stability,
        clarity = state.clarity,
        speed = state.speed,
        pitch = state.pitch,
        apiKey = state.elevenLabsApiKey,
      )
      result.fold(
        onSuccess = { item ->
          _uiState.update {
            it.copy(
              isGenerating = false,
              currentAudio = item,
              statusMessage = "Generated via ${item.sourceMode}",
            )
          }
          togglePlayPause()
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(isGenerating = false, statusMessage = err.message ?: "Error generating voice")
          }
        }
      )
    }
  }

  fun togglePlayPause() {
    val state = _uiState.value
    val audio = state.currentAudio ?: return
    if (state.isPlaying) {
      engine.stopPlayback()
      _uiState.update { it.copy(isPlaying = false, playbackProgress = 0f) }
    } else {
      _uiState.update { it.copy(isPlaying = true, playbackProgress = 0f) }
      engine.playFile(audio.filePath) {
        _uiState.update { it.copy(isPlaying = false, playbackProgress = 1f) }
      }
      viewModelScope.launch {
        val steps = 40
        repeat(steps) { idx ->
          if (!_uiState.value.isPlaying) return@launch
          delay((audio.durationSec * 1000L) / steps)
          _uiState.update { it.copy(playbackProgress = ((idx + 1).toFloat() / steps).coerceIn(0f, 1f)) }
        }
      }
    }
  }

  fun saveCurrentAudioToMusic() {
    val item = _uiState.value.currentAudio ?: return
    viewModelScope.launch {
      val res = engine.saveToDeviceMusic(item)
      res.fold(
        onSuccess = { msg -> _uiState.update { it.copy(statusMessage = msg) } },
        onFailure = { err -> _uiState.update { it.copy(statusMessage = err.message ?: "Save failed") } }
      )
    }
  }

  override fun onCleared() {
    engine.release()
    super.onCleared()
  }
}
