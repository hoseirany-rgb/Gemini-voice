package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.LiveVoiceOrb
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GeminiBlue
import com.example.ui.theme.GeminiCyan
import com.example.ui.theme.GeminiGlowCyan
import com.example.ui.theme.GeminiMagenta
import com.example.ui.theme.GeminiViolet
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.viewmodel.GeminiLiveViewModel
import com.example.ui.viewmodel.LiveVoiceState
import java.util.Locale

@Composable
fun LiveVoiceScreen(
    viewModel: GeminiLiveViewModel,
    onNavigateToChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val liveVoiceState by viewModel.liveVoiceState.collectAsState()
    val isMuted by viewModel.isTtsMuted.collectAsState()
    val selectedVoice by viewModel.selectedVoiceName.collectAsState()
    val attachedBitmap by viewModel.attachedBitmap.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var textInput by remember { mutableStateOf("") }

    val voiceList = listOf("Kore", "Aoede", "Charon", "Fenrir", "Puck", "Zephyr")

    // Speech Recognizer Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrEmpty()) {
                val text = spokenText!!
                textInput = text
                viewModel.sendPrompt(text)
            }
        }
        viewModel.setListeningState(false)
    }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                viewModel.attachImage(bitmap)
            } catch (e: Exception) {
                // handle image decode error
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "GEMINI LIVE AI",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = GeminiGlowCyan
                    )
                    Text(
                        text = activeSession?.title ?: "Real-Time Assistant",
                        fontSize = 13.sp,
                        color = TextSecondaryDark
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.toggleMuteTts() },
                        modifier = Modifier.testTag("mute_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Mute TTS",
                            tint = if (isMuted) GeminiMagenta else GeminiGlowCyan
                        )
                    }
                }
            }
        }

        // Voice Selector Chips
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Voice Persona",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondaryDark,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(voiceList) { voiceName ->
                    FilterChip(
                        selected = selectedVoice == voiceName,
                        onClick = { viewModel.setVoiceName(voiceName) },
                        label = { Text(voiceName, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GeminiViolet,
                            selectedLabelColor = TextPrimaryDark,
                            containerColor = DarkSurface,
                            labelColor = TextSecondaryDark
                        ),
                        modifier = Modifier.testTag("voice_chip_$voiceName")
                    )
                }
            }
        }

        // Center Animated Orb & Audio Wave Visualizer
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            LiveVoiceOrb(
                state = liveVoiceState,
                onClick = {
                    if (liveVoiceState == LiveVoiceState.SPEAKING) {
                        viewModel.stopSpeaking()
                    } else {
                        viewModel.setListeningState(true)
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening to your voice...")
                        }
                        try {
                            speechLauncher.launch(intent)
                        } catch (e: Exception) {
                            viewModel.setListeningState(false)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = when (liveVoiceState) {
                    LiveVoiceState.LISTENING -> "🎙️ Listening to you..."
                    LiveVoiceState.THINKING -> "🧠 Gemini Live thinking..."
                    LiveVoiceState.SPEAKING -> "🗣️ Speaking response..."
                    LiveVoiceState.IDLE -> "Tap orb or mic to start Gemini Live voice"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = when (liveVoiceState) {
                    LiveVoiceState.LISTENING -> GeminiGlowCyan
                    LiveVoiceState.THINKING -> GeminiViolet
                    LiveVoiceState.SPEAKING -> GeminiCyan
                    LiveVoiceState.IDLE -> TextSecondaryDark
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            AudioWaveformVisualizer(
                isAnimating = liveVoiceState == LiveVoiceState.SPEAKING || liveVoiceState == LiveVoiceState.LISTENING
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = GeminiMagenta.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.clearError() }
                ) {
                    Text(
                        text = errorMessage!!,
                        fontSize = 12.sp,
                        color = GeminiMagenta,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // Attached Image Preview
        if (attachedBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .padding(8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        bitmap = attachedBitmap!!.asImageBitmap(),
                        contentDescription = "Attached image",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Image Attached", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GeminiGlowCyan)
                        Text("Ask Gemini Live about this picture", fontSize = 11.sp, color = TextSecondaryDark)
                    }
                    IconButton(onClick = { viewModel.attachImage(null) }) {
                        Text("✕", color = GeminiMagenta, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Bottom Controls Bar
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = DarkCard,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.testTag("camera_attach_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera Vision",
                        tint = GeminiGlowCyan
                    )
                }

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Ask Gemini Live...", color = TextSecondaryDark, fontSize = 14.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_text_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeminiCyan,
                        unfocusedBorderColor = DarkSurface,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                if (textInput.isNotBlank() || attachedBitmap != null) {
                    IconButton(
                        onClick = {
                            val prompt = textInput
                            textInput = ""
                            viewModel.sendPrompt(prompt)
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(GeminiCyan)
                            .testTag("send_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Prompt",
                            tint = DarkBackground
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            viewModel.setListeningState(true)
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening to your voice...")
                            }
                            try {
                                speechLauncher.launch(intent)
                            } catch (e: Exception) {
                                viewModel.setListeningState(false)
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(GeminiViolet)
                            .testTag("mic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Mic Trigger",
                            tint = TextPrimaryDark
                        )
                    }
                }
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.trim().isEmpty()
}
