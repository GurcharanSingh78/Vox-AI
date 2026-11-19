package com.voxai.studio.engine

import android.content.ContentValues
import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.voxai.studio.model.GeneratedAudioItem
import com.voxai.studio.model.VoicePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.random.Random

class VoxAudioEngine(private val context: Context) {
  private var tts: TextToSpeech? = null
  private var ttsReady = false
  private var mediaPlayer: MediaPlayer? = null

  init {
    tts = TextToSpeech(context) { status ->
      if (status == TextToSpeech.SUCCESS) {
        tts?.language = Locale("hi", "IN")
        ttsReady = true
      }
    }
  }

  suspend fun generateVoice(
    text: String,
    voice: VoicePreset,
    stability: Float,
    clarity: Float,
    speed: Float,
    pitch: Float,
    apiKey: String,
  ): Result<GeneratedAudioItem> = withContext(Dispatchers.IO) {
    val cleanText = text.trim()
    if (cleanText.isEmpty()) return@withContext Result.failure(Exception("Please enter some text first."))

    val outFile = File(context.cacheDir, "vox_${System.currentTimeMillis()}.mp3")
    val usedElevenLabs: Boolean = if (apiKey.isNotBlank()) {
      try {
        callElevenLabsApi(cleanText, voice.elevenLabsVoiceId, stability, clarity, speed, apiKey.trim(), outFile)
      } catch (e: Exception) {
        false
      }
    } else {
      false
    }

    val finalFile = if (usedElevenLabs && outFile.exists() && outFile.length() > 500L) {
      outFile
    } else {
      val wavFile = File(context.cacheDir, "vox_${System.currentTimeMillis()}.wav")
      val ok = synthesizeOfflineTts(cleanText, voice, speed, pitch, wavFile)
      if (!ok || !wavFile.exists()) {
        return@withContext Result.failure(Exception("Voice synthesis failed on this device."))
      }
      wavFile
    }

    val durationSec = maxOf(2, (cleanText.length / 13))
    val bars = List(36) { Random.nextFloat().coerceIn(0.2f, 1.0f) }

    Result.success(
      GeneratedAudioItem(
        id = UUID.randomUUID().toString(),
        title = cleanText.take(28).ifBlank { "Vox Audio" },
        voiceName = voice.name,
        filePath = finalFile.absolutePath,
        sourceMode = if (usedElevenLabs) "ElevenLabs HD" else "Vox Neural Offline",
        durationSec = durationSec,
        waveform = bars,
      )
    )
  }

  private fun callElevenLabsApi(
    text: String,
    voiceId: String,
    stability: Float,
    clarity: Float,
    speed: Float,
    apiKey: String,
    outFile: File,
  ): Boolean {
    val url = URL("https://api.elevenlabs.io/v1/text-to-speech/$voiceId")
    val conn = (url.openConnection() as HttpURLConnection).apply {
      requestMethod = "POST"
      setRequestProperty("xi-api-key", apiKey)
      setRequestProperty("Content-Type", "application/json")
      setRequestProperty("Accept", "audio/mpeg")
      doOutput = true
      connectTimeout = 15000
      readTimeout = 25000
    }

    val payload = JSONObject().apply {
      put("text", text)
      put("model_id", "eleven_multilingual_v2")
      put("voice_settings", JSONObject().apply {
        put("stability", stability.toDouble())
        put("similarity_boost", clarity.toDouble())
        put("speed", speed.toDouble())
      })
    }

    conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
    if (conn.responseCode !in 200..299) return false

    conn.inputStream.use { input ->
      outFile.outputStream().use { output -> input.copyTo(output) }
    }
    return true
  }

  private suspend fun synthesizeOfflineTts(
    text: String,
    voice: VoicePreset,
    speed: Float,
    pitch: Float,
    outFile: File,
  ): Boolean = suspendCancellableCoroutine { cont ->
    val engine = tts
    if (engine == null || !ttsReady) {
      cont.resume(false)
      return@suspendCancellableCoroutine
    }

    val hasHindi = text.any { it in '\u0900'..'\u097F' }
    engine.language = if (hasHindi) Locale("hi", "IN") else Locale.US
    engine.setPitch((voice.pitch * pitch).coerceIn(0.5f, 1.8f))
    engine.setSpeechRate((voice.rate * speed).coerceIn(0.5f, 2.0f))

    val utteranceId = UUID.randomUUID().toString()
    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
      override fun onStart(id: String?) {}
      override fun onDone(id: String?) {
        if (id == utteranceId && cont.isActive) cont.resume(true)
      }
      @Deprecated("Deprecated in Java")
      override fun onError(id: String?) {
        if (id == utteranceId && cont.isActive) cont.resume(false)
      }
    })

    val res = engine.synthesizeToFile(text, Bundle(), outFile, utteranceId)
    if (res != TextToSpeech.SUCCESS && cont.isActive) {
      cont.resume(false)
    }
  }

  fun playFile(filePath: String, onCompleted: () -> Unit) {
    stopPlayback()
    mediaPlayer = MediaPlayer().apply {
      setDataSource(filePath)
      prepare()
      setOnCompletionListener { onCompleted() }
      start()
    }
  }

  fun stopPlayback() {
    mediaPlayer?.run {
      if (isPlaying) stop()
      release()
    }
    mediaPlayer = null
  }

  suspend fun saveToDeviceMusic(item: GeneratedAudioItem): Result<String> = withContext(Dispatchers.IO) {
    try {
      val src = File(item.filePath)
      if (!src.exists()) return@withContext Result.failure(Exception("Audio file not found."))

      val isMp3 = src.extension.equals("mp3", ignoreCase = true)
      val fileName = "VoxAI_${item.voiceName}_${System.currentTimeMillis()}.${if (isMp3) "mp3" else "wav"}"
      val mimeType = if (isMp3) "audio/mpeg" else "audio/wav"

      val values = ContentValues().apply {
        put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/VoxAI")
          put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
      }

      val resolver = context.contentResolver
      val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
        ?: return@withContext Result.failure(Exception("Could not create MediaStore entry."))

      resolver.openOutputStream(uri)?.use { out ->
        FileInputStream(src).use { input -> input.copyTo(out) }
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        values.clear()
        values.put(MediaStore.Audio.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
      }

      Result.success("Saved to Music/VoxAI/$fileName")
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun release() {
    stopPlayback()
    tts?.shutdown()
  }
}
