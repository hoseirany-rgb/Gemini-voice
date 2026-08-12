package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class GeminiSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val mode: String = "Live Voice", // "Live Voice", "Multimodal", "Code Assistant", "Translator", "Tutor"
    val voiceName: String = "Kore", // "Kore", "Aoede", "Charon", "Fenrir", "Puck", "Zephyr"
    val systemInstruction: String = "You are Gemini Live, a friendly, ultra-fast, intelligent voice and multimodal AI assistant.",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class GeminiMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val sender: String, // "user" or "gemini"
    val text: String,
    val isAudioResponse: Boolean = false,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "presets")
data class GeminiPresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val category: String, // "Voice", "Code", "Language", "Learning", "Creative"
    val systemInstruction: String,
    val defaultVoice: String = "Kore",
    val samplePrompt: String,
    val isCustom: Boolean = false
)
