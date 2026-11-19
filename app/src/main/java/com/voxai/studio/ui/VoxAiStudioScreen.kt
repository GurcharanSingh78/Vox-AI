package com.voxai.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxai.studio.model.VoiceCategory
import kotlinx.coroutines.delay

private val BgDark = Color(0xFF090B10)
private val PanelDark = Color(0xFF111520)
private val CardDark = Color(0xFF181E2E)
private val BorderDark = Color(0xFF263047)
private val NeonCyan = Color(0xFF00F0FF)
private val NeonPurple = Color(0xFF9333EA)

@Composable
fun VoxAiStudioScreen(viewModel: VoxViewModel) {
  val state by viewModel.uiState.collectAsState()

  LaunchedEffect(state.statusMessage) {
    if (state.statusMessage != null) {
      delay(2800L)
      viewModel.clearStatus()
    }
  }

  Scaffold(
    containerColor = BgDark,
    contentWindowInsets = WindowInsets.safeDrawing,
  ) { padding ->
    Box(modifier = Modifier.fillMaxSize().background(BgDark).padding(padding)) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Top Header Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(NeonCyan, NeonPurple))),
              contentAlignment = Alignment.Center,
            ) {
              Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = BgDark, modifier = Modifier.size(20.dp))
            }
            Column {
              Text("Vox AI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
              Text(
                text = if (state.elevenLabsApiKey.isBlank()) "Free Neural Mode" else "ElevenLabs HD Connected",
                color = if (state.elevenLabsApiKey.isBlank()) Color(0xFF94A3B8) else NeonCyan,
                fontSize = 11.sp,
              )
            }
          }

          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(CardDark)
              .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
              .clickable { viewModel.toggleApiKeyDialog(true) }
              .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Icon(Icons.Filled.Key, contentDescription = "API Key", tint = NeonCyan, modifier = Modifier.size(15.dp))
            Text("API Key", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
          }
        }

        // Script Editor Card
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelDark)
            .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Script Studio", color = Color(0xFFCBD5E1), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("${state.scriptText.length} / 5000", color = Color(0xFF64748B), fontSize = 11.sp)
          }

          OutlinedTextField(
            value = state.scriptText,
            onValueChange = viewModel::updateScript,
            placeholder = { Text("Write Hindi, Punjabi, or English script...", color = Color(0xFF64748B)) },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = NeonCyan,
              unfocusedBorderColor = BorderDark,
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedContainerColor = CardDark,
              unfocusedContainerColor = CardDark,
            ),
            modifier = Modifier.fillMaxWidth().height(135.dp),
          )

          // Emotion / Pause Quick Chips
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            listOf("[pause]", "[whispers]", "[excited]", "[dramatic]", "—").forEach { tag ->
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(CardDark)
                  .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                  .clickable { viewModel.appendEmotionTag(tag) }
                  .padding(horizontal = 10.dp, vertical = 5.dp),
              ) {
                Text(tag, color = NeonCyan, fontSize = 11.sp)
              }
            }
          }
        }

        // Voice Library Selector
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("AI Voice Library", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            VoiceCategory.entries.forEach { cat ->
              val selected = state.selectedCategory == cat
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(20.dp))
                  .background(if (selected) NeonPurple.copy(alpha = 0.25f) else PanelDark)
                  .border(1.dp, if (selected) NeonPurple else BorderDark, RoundedCornerShape(20.dp))
                  .clickable { viewModel.selectCategory(cat) }
                  .padding(horizontal = 12.dp, vertical = 6.dp),
              ) {
                Text(cat.label, color = if (selected) Color.White else Color(0xFF94A3B8), fontSize = 11.sp)
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            state.filteredVoices.forEach { voice ->
              val selected = voice.id == state.selectedVoiceId
              val accent = Color(voice.accentHex)
              Column(
                modifier = Modifier
                  .width(148.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(if (selected) CardDark else PanelDark)
                  .border(1.5.dp, if (selected) accent else BorderDark, RoundedCornerShape(12.dp))
                  .clickable { viewModel.selectVoice(voice.id) }
                  .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(accent))
                  Text(voice.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Text(voice.tagline, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1)
                Text(voice.language, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Medium)
              }
            }
          }
        }

        // ElevenLabs-Style Tuning Sliders
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelDark)
            .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text("Voice Tuning", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
          TuningSlider("Stability", "${(state.stability * 100).toInt()}%", state.stability, 0f..1f, NeonCyan, viewModel::updateStability)
          TuningSlider("Clarity + Similarity", "${(state.clarity * 100).toInt()}%", state.clarity, 0f..1f, NeonPurple, viewModel::updateClarity)
          TuningSlider("Speed", "${String.format("%.1f", state.speed)}x", state.speed, 0.6f..1.6f, NeonCyan, viewModel::updateSpeed)
        }

        // Primary Generate Speech Button
        Button(
          onClick = viewModel::generateSpeech,
          enabled = !state.isGenerating,
          colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
          contentPadding = PaddingValues(),
          modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(12.dp)),
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Brush.horizontalGradient(listOf(NeonCyan, NeonPurple))),
            contentAlignment = Alignment.Center,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = BgDark)
              Text(
                text = if (state.isGenerating) "Synthesizing Voice..." else "Generate Speech",
                color = BgDark,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
              )
            }
          }
        }

        // Waveform Audio Player & Direct Music/VoxAI Export
        state.currentAudio?.let { item ->
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(CardDark)
              .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("${item.voiceName} • ${item.sourceMode}", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(item.title, color = Color.White, fontSize = 13.sp, maxLines = 1)
              }
              IconButton(
                onClick = viewModel::saveCurrentAudioToMusic,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(PanelDark),
              ) {
                Icon(Icons.Filled.Download, contentDescription = "Save MP3", tint = NeonCyan)
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(NeonCyan)
                  .clickable { viewModel.togglePlayPause() },
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                  contentDescription = "Play/Pause",
                  tint = BgDark,
                )
              }

              Row(
                modifier = Modifier.weight(1f).height(32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
              ) {
                item.waveform.forEachIndexed { idx, amp ->
                  val active = (idx.toFloat() / item.waveform.size) <= state.playbackProgress
                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .fillMaxHeight(amp)
                      .clip(RoundedCornerShape(2.dp))
                      .background(if (active) NeonCyan else Color(0xFF334155))
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
      }

      // Status Toast Pill
      state.statusMessage?.let { msg ->
        Box(
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardDark)
            .border(1.dp, NeonCyan, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        ) {
          Text(msg, color = NeonCyan, fontSize = 12.sp)
        }
      }

      // ElevenLabs API Key Dialog
      if (state.showApiKeyDialog) {
        var tempKey by remember { mutableStateOf(state.elevenLabsApiKey) }
        AlertDialog(
          onDismissRequest = { viewModel.toggleApiKeyDialog(false) },
          containerColor = PanelDark,
          title = { Text("ElevenLabs API Key", color = Color.White) },
          text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                "Paste your ElevenLabs API key for ultra-realistic voices, or leave blank to use Free Offline Neural TTS.",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
              )
              OutlinedTextField(
                value = tempKey,
                onValueChange = { tempKey = it },
                singleLine = true,
                placeholder = { Text("sk_...", color = Color(0xFF64748B)) },
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = NeonCyan,
                  unfocusedBorderColor = BorderDark,
                  focusedTextColor = Color.White,
                  unfocusedTextColor = Color.White,
                ),
              )
            }
          },
          confirmButton = {
            TextButton(onClick = { viewModel.saveApiKey(tempKey) }) {
              Text("Save", color = NeonCyan, fontWeight = FontWeight.Bold)
            }
          },
          dismissButton = {
            TextButton(onClick = { viewModel.toggleApiKeyDialog(false) }) {
              Text("Cancel", color = Color(0xFF94A3B8))
            }
          },
        )
      }
    }
  }
}

@Composable
private fun TuningSlider(
  label: String,
  valueText: String,
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  accent: Color,
  onChange: (Float) -> Unit,
) {
  Column {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(label, color = Color(0xFF94A3B8), fontSize = 11.sp)
      Text(valueText, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
    Slider(
      value = value,
      onValueChange = onChange,
      valueRange = range,
      colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = CardDark),
      modifier = Modifier.fillMaxWidth().height(26.dp),
    )
  }
}
