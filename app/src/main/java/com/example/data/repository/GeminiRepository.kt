package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.GeminiMessageEntity
import com.example.data.local.GeminiPresetEntity
import com.example.data.local.GeminiSessionEntity
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiPart
import com.example.data.remote.GeminiRequest
import com.example.data.remote.GenerationConfig
import com.example.data.remote.InlineData
import com.example.data.remote.PrebuiltVoiceConfig
import com.example.data.remote.RetrofitClient
import com.example.data.remote.SpeechConfig
import com.example.data.remote.VoiceConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class GeminiRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val sessionDao = db.sessionDao()
    private val messageDao = db.messageDao()
    private val presetDao = db.presetDao()

    val allSessions: Flow<List<GeminiSessionEntity>> = sessionDao.getAllSessions()
    val allPresets: Flow<List<GeminiPresetEntity>> = presetDao.getAllPresets()

    fun getMessagesForSession(sessionId: Long): Flow<List<GeminiMessageEntity>> {
        return messageDao.getMessagesForSession(sessionId)
    }

    suspend fun createSession(
        title: String,
        mode: String = "Live Voice",
        voiceName: String = "Kore",
        systemInstruction: String = "You are Gemini Live, a friendly, ultra-fast, intelligent AI assistant."
    ): Long = withContext(Dispatchers.IO) {
        val newSession = GeminiSessionEntity(
            title = title,
            mode = mode,
            voiceName = voiceName,
            systemInstruction = systemInstruction
        )
        sessionDao.insertSession(newSession)
    }

    suspend fun getSessionById(sessionId: Long): GeminiSessionEntity? = withContext(Dispatchers.IO) {
        sessionDao.getSessionById(sessionId)
    }

    suspend fun updateSession(session: GeminiSessionEntity) = withContext(Dispatchers.IO) {
        sessionDao.updateSession(session.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteSession(sessionId: Long) = withContext(Dispatchers.IO) {
        messageDao.deleteMessagesForSession(sessionId)
        sessionDao.deleteSessionById(sessionId)
    }

    suspend fun clearAllSessions() = withContext(Dispatchers.IO) {
        sessionDao.deleteAllSessions()
    }

    suspend fun addPreset(preset: GeminiPresetEntity) = withContext(Dispatchers.IO) {
        presetDao.insertPreset(preset)
    }

    suspend fun deletePreset(preset: GeminiPresetEntity) = withContext(Dispatchers.IO) {
        presetDao.deletePreset(preset)
    }

    suspend fun sendPrompt(
        sessionId: Long,
        promptText: String,
        bitmap: Bitmap? = null,
        history: List<GeminiMessageEntity> = emptyList(),
        systemInstruction: String? = null,
        voiceName: String = "Kore",
        requestVoiceMode: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(
                    IllegalStateException("Gemini API key is missing. Please configure your API Key in AI Studio Secrets panel.")
                )
            }

            // Save user message in DB
            val userMsg = GeminiMessageEntity(
                sessionId = sessionId,
                sender = "user",
                text = promptText,
                imageUri = if (bitmap != null) "image_attachment" else null,
                timestamp = System.currentTimeMillis()
            )
            messageDao.insertMessage(userMsg)

            // Construct contents history
            val contentsList = mutableListOf<GeminiContent>()

            // Add history
            history.takeLast(10).forEach { msg ->
                val role = if (msg.sender == "user") "user" else "model"
                contentsList.add(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = msg.text)),
                        role = role
                    )
                )
            }

            // Current prompt part
            val currentParts = mutableListOf<GeminiPart>()
            currentParts.add(GeminiPart(text = promptText))

            if (bitmap != null) {
                val base64Image = bitmapToBase64(bitmap)
                currentParts.add(
                    GeminiPart(
                        inlineData = InlineData(
                            mimeType = "image/jpeg",
                            data = base64Image
                        )
                    )
                )
            }

            contentsList.add(GeminiContent(parts = currentParts, role = "user"))

            val systemPart = if (!systemInstruction.isNull_or_blank()) {
                GeminiContent(parts = listOf(GeminiPart(text = systemInstruction)))
            } else null

            val genConfig = if (requestVoiceMode) {
                GenerationConfig(
                    responseModalities = listOf("TEXT"),
                    speechConfig = SpeechConfig(
                        voiceConfig = VoiceConfig(
                            prebuiltVoiceConfig = PrebuiltVoiceConfig(voiceName = voiceName)
                        )
                    )
                )
            } else {
                GenerationConfig()
            }

            val request = GeminiRequest(
                contents = contentsList,
                systemInstruction = systemPart,
                generationConfig = genConfig
            )

            val modelName = "gemini-3.5-flash"
            val response = RetrofitClient.apiService.generateContent(modelName, apiKey, request)

            if (response.isSuccessful && response.body() != null) {
                val candidate = response.body()?.candidates?.firstOrNull()
                val responseText = candidate?.content?.parts?.firstOrNull()?.text
                    ?: "Gemini Live received your request."

                // Save gemini response message
                val geminiMsg = GeminiMessageEntity(
                    sessionId = sessionId,
                    sender = "gemini",
                    text = responseText,
                    isAudioResponse = requestVoiceMode,
                    timestamp = System.currentTimeMillis()
                )
                messageDao.insertMessage(geminiMsg)

                // Update session updated timestamp
                sessionDao.getSessionById(sessionId)?.let {
                    sessionDao.updateSession(it.copy(updatedAt = System.currentTimeMillis()))
                }

                Result.success(responseText)
            } else {
                val errBody = response.errorBody()?.string() ?: "API Error ${response.code()}"
                Result.failure(Exception("Gemini Error: $errBody"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun CharSequence?.isNull_or_blank(): Boolean {
        return this == null || this.trim().isEmpty()
    }
}
