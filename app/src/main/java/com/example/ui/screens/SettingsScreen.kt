package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.GeminiCyan
import com.example.ui.theme.GeminiGlowCyan
import com.example.ui.theme.GeminiMagenta
import com.example.ui.theme.GeminiViolet
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.viewmodel.GeminiLiveViewModel

@Composable
fun SettingsScreen(
    viewModel: GeminiLiveViewModel,
    modifier: Modifier = Modifier
) {
    val selectedVoice by viewModel.selectedVoiceName.collectAsState()
    val isMuted by viewModel.isTtsMuted.collectAsState()

    val apiKeyConfigured = try {
        BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
    } catch (e: Exception) {
        false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Gemini Live Settings",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            color = GeminiGlowCyan
        )

        // API Key Status Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("api_key_status_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Gemini API Connection",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimaryDark
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (apiKeyConfigured) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (apiKeyConfigured) GeminiCyan else GeminiMagenta
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(
                            text = if (apiKeyConfigured) "Configured" else "Needs Key",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (apiKeyConfigured) GeminiCyan else GeminiMagenta
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (apiKeyConfigured)
                        "Your Gemini API Key is active from AI Studio Secrets. Live Voice, Multimodal, and Text models are ready."
                    else
                        "To use real-time Gemini Live features, ensure your GEMINI_API_KEY is set in the Secrets panel in AI Studio.",
                    fontSize = 13.sp,
                    color = TextSecondaryDark
                )
            }
        }

        // Mandatory Security Warning Banner
        Surface(
            color = GeminiViolet.copy(alpha = 0.25f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Security Note",
                    tint = GeminiGlowCyan,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.padding(6.dp))
                Column {
                    Text(
                        text = "Security Note",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GeminiGlowCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Security Warning: I have included your API keys in the generated APK file for this prototype. Please be aware that Android APKs can be easily decompiled, and these keys can be extracted by anyone who has access to the file. Do not share this APK file publicly or with unauthorized individuals to prevent potential misuse.",
                        fontSize = 12.sp,
                        color = TextPrimaryDark,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Preferences Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Preferences",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimaryDark
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Active Voice Persona", fontSize = 14.sp, color = TextSecondaryDark)
                    Text(selectedVoice, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GeminiGlowCyan)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Speech Synthesis Output", fontSize = 14.sp, color = TextSecondaryDark)
                    Text(if (isMuted) "Muted" else "Enabled", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isMuted) GeminiMagenta else GeminiCyan)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Gemini Model", fontSize = 14.sp, color = TextSecondaryDark)
                    Text("gemini-3.5-flash", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GeminiGlowCyan)
                }
            }
        }
    }
}
