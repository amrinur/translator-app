package com.example.translatorapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.translatorapp.ui.theme.TranslatorAppTheme
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var translationService: TranslationService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize translation service
        translationService = TranslationService(application)

        setContent {
            TranslatorAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TranslatorScreen(translationService)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        translationService.cleanup()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(translationService: TranslationService? = null) {
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }

    var sourceLanguage by remember { mutableStateOf(TranslationService.AVAILABLE_LANGUAGES.first()) }
    var targetLanguage by remember { mutableStateOf(TranslationService.AVAILABLE_LANGUAGES.find { it.second == TranslateLanguage.SPANISH }!!) }

    var isTranslating by remember { mutableStateOf(false) }
    var showSourceLanguageDropdown by remember { mutableStateOf(false) }
    var showTargetLanguageDropdown by remember { mutableStateOf(false) }

    val downloadProgress by translationService?.downloadProgress?.collectAsState() ?: remember { mutableStateOf(0f) }
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with language selectors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Source language selector
            Box {
                Row(
                    modifier = Modifier
                        .clickable { showSourceLanguageDropdown = true }
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(0.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sourceLanguage.first,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select source language"
                    )
                }

                DropdownMenu(
                    expanded = showSourceLanguageDropdown,
                    onDismissRequest = { showSourceLanguageDropdown = false }
                ) {
                    TranslationService.AVAILABLE_LANGUAGES.forEach { language ->
                        DropdownMenuItem(
                            text = { Text(language.first) },
                            onClick = {
                                sourceLanguage = language
                                showSourceLanguageDropdown = false
                            }
                        )
                    }
                }
            }

            // Target language selector
            Box {
                Row(
                    modifier = Modifier
                        .clickable { showTargetLanguageDropdown = true }
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(0.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = targetLanguage.first,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select target language"
                    )
                }

                DropdownMenu(
                    expanded = showTargetLanguageDropdown,
                    onDismissRequest = { showTargetLanguageDropdown = false }
                ) {
                    TranslationService.AVAILABLE_LANGUAGES.forEach { language ->
                        DropdownMenuItem(
                            text = { Text(language.first) },
                            onClick = {
                                targetLanguage = language
                                showTargetLanguageDropdown = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input Text Field - Brutalist style with thick borders
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(0.dp)),
            placeholder = { Text("Enter text to translate") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (translationService != null && inputText.isNotEmpty()) {
                    scope.launch {
                        isTranslating = true
                        outputText = translationService.translateText(
                            inputText,
                            sourceLanguage.second,
                            targetLanguage.second
                        )
                        isTranslating = false
                    }
                }
            }),
            textStyle = TextStyle(fontSize = 18.sp),
            shape = RoundedCornerShape(0.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )

        // Action buttons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Translate button
            Button(
                onClick = {
                    if (translationService != null && inputText.isNotEmpty()) {
                        scope.launch {
                            isTranslating = true
                            outputText = translationService.translateText(
                                inputText,
                                sourceLanguage.second,
                                targetLanguage.second
                            )
                            isTranslating = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = !isTranslating && inputText.isNotEmpty()
            ) {
                if (isTranslating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text("TRANSLATE")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Translate"
                    )
                }
            }
        }

        // Download progress indicator (only visible when downloading model)
        if (downloadProgress > 0 && downloadProgress < 1) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    "Downloading translation model: ${(downloadProgress * 100).toInt()}%",
                    style = TextStyle(fontSize = 14.sp)
                )
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Output container - Brutalist style
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(0.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "TRANSLATION",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = outputText.ifEmpty { "Translation will appear here" },
                style = TextStyle(fontSize = 18.sp),
                modifier = Modifier.fillMaxWidth(),
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Copy button
        Button(
            onClick = {
                if (outputText.isNotEmpty()) {
                    clipboardManager.setText(AnnotatedString(outputText))
                    // You could show a snackbar or Toast here
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black
            ),
            enabled = outputText.isNotEmpty()
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("COPY", color = Color.White)
        }
    }
}

@Preview
@Composable
fun GreetingPreview() {
    TranslatorAppTheme {
        TranslatorScreen()
    }
}