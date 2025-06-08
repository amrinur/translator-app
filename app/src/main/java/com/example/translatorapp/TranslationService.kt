package com.example.translatorapp

import android.app.Application
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class TranslationService(private val application: Application) {

    companion object {
        val AVAILABLE_LANGUAGES = listOf(
            Pair("English", TranslateLanguage.ENGLISH),
            Pair("Spanish", TranslateLanguage.SPANISH),
            Pair("French", TranslateLanguage.FRENCH),
            Pair("German", TranslateLanguage.GERMAN),
            Pair("Italian", TranslateLanguage.ITALIAN),
            Pair("Japanese", TranslateLanguage.JAPANESE),
            Pair("Korean", TranslateLanguage.KOREAN),
            Pair("Chinese", TranslateLanguage.CHINESE),
            Pair("Russian", TranslateLanguage.RUSSIAN),
            Pair("Indonesian", TranslateLanguage.INDONESIAN)
        )
        private const val TAG = "TranslationService"
    }

    private var translator: Translator? = null
    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress
    private val modelManager = RemoteModelManager.getInstance()

    suspend fun translateText(
        text: String,
        sourceLanguage: String = TranslateLanguage.ENGLISH,
        targetLanguage: String = TranslateLanguage.SPANISH
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting translation from $sourceLanguage to $targetLanguage")

                // Create translator options
                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLanguage)
                    .setTargetLanguage(targetLanguage)
                    .build()

                // Close previous translator if exists
                translator?.close()
                translator = Translation.getClient(options)

                // Set download conditions - remove WiFi requirement
                val downloadConditions = DownloadConditions.Builder()
                    .build() // Remove requireWifi() to allow cellular downloads

                // Try to pre-download the model
                try {
                    _downloadProgress.value = 0.1f
                    Log.d(TAG, "Starting model download if needed")

                    val downloadTask = translator!!.downloadModelIfNeeded(downloadConditions)

                    // Add listeners to track download progress
                    downloadTask.addOnSuccessListener {
                        Log.d(TAG, "Model download successful")
                        _downloadProgress.value = 1.0f
                    }.addOnFailureListener { exception ->
                        Log.e(TAG, "Model download failed", exception)
                        _downloadProgress.value = 0f
                    }

                    // Wait for download to complete with timeout
                    Tasks.await(downloadTask)
                    Log.d(TAG, "Model download completed or already available")

                } catch (e: Exception) {
                    Log.e(TAG, "Error downloading model", e)
                    return@withContext "Error downloading translation model: ${e.message}"
                }

                // Perform translation
                try {
                    Log.d(TAG, "Starting actual translation of: $text")
                    val result = Tasks.await(translator!!.translate(text))
                    Log.d(TAG, "Translation successful: $result")
                    result
                } catch (e: Exception) {
                    Log.e(TAG, "Translation failed", e)
                    "Translation error: ${e.message}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "General translation service error", e)
                "Translation service error: ${e.message}"
            }
        }
    }

    // Check if models are already downloaded
    suspend fun isModelDownloaded(languageCode: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val model = TranslateRemoteModel.Builder(languageCode).build()
                val downloadedModels = Tasks.await(modelManager.getDownloadedModels(TranslateRemoteModel::class.java))
                downloadedModels.contains(model)
            } catch (e: Exception) {
                Log.e(TAG, "Error checking if model is downloaded", e)
                false
            }
        }
    }

    fun cleanup() {
        translator?.close()
        translator = null
    }
}