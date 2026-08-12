package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.MainAppNavigation
import com.example.ui.theme.GeminiLiveTheme
import com.example.ui.viewmodel.GeminiLiveViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeminiLiveTheme {
                val viewModel: GeminiLiveViewModel = viewModel()
                MainAppNavigation(viewModel = viewModel)
            }
        }
    }
}
