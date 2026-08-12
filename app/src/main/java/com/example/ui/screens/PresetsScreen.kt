package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.GeminiPresetEntity
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GeminiBlue
import com.example.ui.theme.GeminiCyan
import com.example.ui.theme.GeminiGlowCyan
import com.example.ui.theme.GeminiViolet
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.viewmodel.GeminiLiveViewModel

@Composable
fun PresetsScreen(
    viewModel: GeminiLiveViewModel,
    onPresetApplied: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presets by viewModel.presets.collectAsState()
    var showCustomDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Preset Assistant Studio",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = GeminiGlowCyan
                )
                Text(
                    text = "Choose a specialized mode or build your own",
                    fontSize = 13.sp,
                    color = TextSecondaryDark
                )
            }

            Button(
                onClick = { showCustomDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = GeminiViolet),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("create_custom_preset_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preset List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(presets) { preset ->
                PresetCard(
                    preset = preset,
                    onApply = {
                        viewModel.applyPreset(preset)
                        onPresetApplied()
                    }
                )
            }
        }
    }

    if (showCustomDialog) {
        CreatePresetDialog(
            onDismiss = { showCustomDialog = false },
            onConfirm = { title, desc, category, sysInst, voice, prompt ->
                viewModel.addCustomPreset(title, desc, category, sysInst, voice, prompt)
                showCustomDialog = false
            }
        )
    }
}

@Composable
fun PresetCard(
    preset: GeminiPresetEntity,
    onApply: () -> Unit
) {
    val categoryIcon: ImageVector = when (preset.category) {
        "Voice" -> Icons.Default.RecordVoiceOver
        "Code" -> Icons.Default.Code
        "Language" -> Icons.Default.Translate
        "Learning" -> Icons.Default.School
        "Creative" -> Icons.Default.Lightbulb
        else -> Icons.Default.AutoAwesome
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onApply() }
            .testTag("preset_card_${preset.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = preset.category,
                        tint = GeminiGlowCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = preset.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimaryDark
                    )
                }

                Text(
                    text = preset.category,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GeminiViolet
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = preset.description,
                fontSize = 13.sp,
                color = TextSecondaryDark
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Default Voice: ${preset.defaultVoice}",
                    fontSize = 11.sp,
                    color = GeminiBlue
                )

                Button(
                    onClick = onApply,
                    colors = ButtonDefaults.buttonColors(containerColor = GeminiCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("apply_preset_${preset.id}")
                ) {
                    Text("Start Live Mode", color = DarkBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CreatePresetDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, category: String, sysInstruction: String, voice: String, samplePrompt: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Custom") }
    var sysInst by remember { mutableStateOf("") }
    var voice by remember { mutableStateOf("Kore") }
    var samplePrompt by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = { Text("Create Custom Assistant Preset", color = TextPrimaryDark, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark)
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark)
                )
                OutlinedTextField(
                    value = sysInst,
                    onValueChange = { sysInst = it },
                    label = { Text("System Instruction") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark)
                )
                OutlinedTextField(
                    value = samplePrompt,
                    onValueChange = { samplePrompt = it },
                    label = { Text("Sample Prompt") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, desc, category, sysInst, voice, samplePrompt)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GeminiCyan)
            ) {
                Text("Save", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondaryDark)
            }
        }
    )
}
