package com.voxai.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voxai.studio.ui.VoxAiStudioScreen
import com.voxai.studio.ui.VoxViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val vm: VoxViewModel = viewModel()
      VoxAiStudioScreen(viewModel = vm)
    }
  }
}
