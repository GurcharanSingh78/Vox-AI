package com.voxai.studio.model

enum class VoiceCategory(val label: String) {
  ALL("All"),
  NARRATIVE("Narrative"),
  REELS("Reels & Shorts"),
  PODCAST("Podcast"),
  SPIRITUAL("Calm & Osho"),
}

data class VoicePreset(
  val id: String,
  val elevenLabsVoiceId: String,
  val name: String,
  val tagline: String,
  val language: String,
  val category: VoiceCategory,
  val pitch: Float,
  val rate: Float,
  val accentHex: Long,
)

data class GeneratedAudioItem(
  val id: String,
  val title: String,
  val voiceName: String,
  val filePath: String,
  val sourceMode: String,
  val durationSec: Int,
  val waveform: List<Float>,
)

data class VoxUiState(
  val scriptText: String = "Welcome to Vox AI Studio. Type any Hindi, Punjabi, or English script and generate realistic voiceovers in seconds.",
  val selectedCategory: VoiceCategory = VoiceCategory.ALL,
  val voices: List<VoicePreset> = defaultVoicePresets(),
  val selectedVoiceId: String = "aarav_deep",
  val stability: Float = 0.55f,
  val clarity: Float = 0.78f,
  val speed: Float = 1.0f,
  val pitch: Float = 1.0f,
  val elevenLabsApiKey: String = "",
  val showApiKeyDialog: Boolean = false,
  val isGenerating: Boolean = false,
  val isPlaying: Boolean = false,
  val playbackProgress: Float = 0f,
  val currentAudio: GeneratedAudioItem? = null,
  val statusMessage: String? = null,
) {
  val selectedVoice: VoicePreset
    get() = voices.firstOrNull { it.id == selectedVoiceId } ?: voices.first()

  val filteredVoices: List<VoicePreset>
    get() = if (selectedCategory == VoiceCategory.ALL) voices else voices.filter { it.category == selectedCategory }
}

fun defaultVoicePresets(): List<VoicePreset> = listOf(
  VoicePreset("aarav_deep", "pNInz6obpgDQGcFmaJgB", "Aarav", "Deep Narrative • Male", "Hindi / EN", VoiceCategory.NARRATIVE, 0.88f, 0.96f, 0xFF00F0FF),
  VoicePreset("zara_emotive", "EXAVITQu4vr4xnSDxMaL", "Zara", "Emotional Story • Female", "Hindi / EN", VoiceCategory.REELS, 1.12f, 1.04f, 0xFFA855F7),
  VoicePreset("kabir_pod", "TxGEqnHWrfWFTfGW9XjX", "Kabir", "Studio Podcast • Male", "English / HI", VoiceCategory.PODCAST, 0.95f, 1.0f, 0xFF14B8A6),
  VoicePreset("guru_calm", "VR6AewLTigWG4xSOukaG", "Bodhi", "Calm Discourse • Deep", "Hindi / EN", VoiceCategory.SPIRITUAL, 0.82f, 0.88f, 0xFFF59E0B),
  VoicePreset("nova_hype", "21m00Tcm4TlvDq8ikWAM", "Nova", "Viral Reels • Crisp", "English", VoiceCategory.REELS, 1.08f, 1.10f, 0xFFEC4899),
)
