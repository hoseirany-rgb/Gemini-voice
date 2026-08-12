package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [GeminiSessionEntity::class, GeminiMessageEntity::class, GeminiPresetEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao
    abstract fun presetDao(): PresetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gemini_live_db"
                )
                .addCallback(DatabaseCallback(context))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialPresets(getDatabase(context).presetDao())
                }
            }

            private suspend fun populateInitialPresets(presetDao: PresetDao) {
                val defaultPresets = listOf(
                    GeminiPresetEntity(
                        title = "Live Voice Companion",
                        description = "Natural conversational buddy for quick thinking, ideas, and real-time dialogue.",
                        category = "Voice",
                        systemInstruction = "You are Gemini Live, a warm, witty, and conversational AI voice companion. Keep responses concise, direct, and conversational as if speaking on a phone call.",
                        defaultVoice = "Kore",
                        samplePrompt = "Hey Gemini, what are three interesting facts about space exploration today?"
                    ),
                    GeminiPresetEntity(
                        title = "Code Reviewer & Debugger",
                        description = "Analyze code snippets, explain syntax, and spot potential bugs live.",
                        category = "Code",
                        systemInstruction = "You are a senior software architect and Kotlin/Android expert. Provide clear, concise explanations, code refactoring tips, and bug diagnoses.",
                        defaultVoice = "Fenrir",
                        samplePrompt = "Can you review this Jetpack Compose state management snippet and suggest improvements?"
                    ),
                    GeminiPresetEntity(
                        title = "Real-time Translator",
                        description = "Instant bidirectional translation with pronunciation notes.",
                        category = "Language",
                        systemInstruction = "You are a real-time multilingual translator. Translate input immediately into target languages, offering phonetic guides when helpful.",
                        defaultVoice = "Aoede",
                        samplePrompt = "How do I ask 'Where is the nearest train station?' politely in Japanese and Spanish?"
                    ),
                    GeminiPresetEntity(
                        title = "Personal Socratic Tutor",
                        description = "Step-by-step breakdown of complex subjects, science, and math.",
                        category = "Learning",
                        systemInstruction = "You are a patient Socratic tutor. Guide the user through concepts using encouraging questions, vivid analogies, and clear steps.",
                        defaultVoice = "Charon",
                        samplePrompt = "Explain how neural networks learn using a simple real-world analogy."
                    ),
                    GeminiPresetEntity(
                        title = "Brainstorming Buddy",
                        description = "Generate creative concepts, titles, app features, or storytelling arcs.",
                        category = "Creative",
                        systemInstruction = "You are an imaginative creative director. Spark innovative ideas, offer unexpected twists, and build upon user suggestions.",
                        defaultVoice = "Puck",
                        samplePrompt = "Give me 5 unique concept names and features for an AI-powered fitness app."
                    )
                )
                presetDao.insertPresets(defaultPresets)
            }
        }
    }
}
