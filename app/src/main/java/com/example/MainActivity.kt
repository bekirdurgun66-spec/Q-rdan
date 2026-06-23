package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.TradingDashboardScreen
import com.example.ui.screens.DeepBg
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.TradingViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = DeepBg
        ) {
          val viewModel: TradingViewModel = viewModel()
          TradingDashboardScreen(viewModel = viewModel)
        }
      }
    }
  }
}

