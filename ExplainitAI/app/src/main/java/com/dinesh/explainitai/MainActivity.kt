package com.dinesh.explainitai

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.dinesh.explainitai.network.MCQ
import com.dinesh.explainitai.ui.screens.*
import com.dinesh.explainitai.ui.theme.ExplainItAITheme

sealed class Screen {
    object Upload : Screen()
    data class SelectPage(val uri: Uri, val initialPage: Int = 0) : Screen()
    data class Mcq(val text: String, val count: Int = 3, val uri: Uri, val returnPage: Int) : Screen()
    data class Chat(val text: String, val uri: Uri, val returnPage: Int) : Screen()
    data class Result(val score: Int, val total: Int, val mcqs: List<MCQ>, val answers: List<String>, val uri: Uri, val returnPage: Int) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            ExplainItAITheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Upload) }

                    when (val screen = currentScreen) {
                        is Screen.Upload -> {
                            UploadScreen(
                                onPdfSelected = { uri -> currentScreen = Screen.SelectPage(uri) }
                            )
                        }
                        is Screen.SelectPage -> {
                            SelectPage(
                                pdfUri = screen.uri,
                                initialPage = screen.initialPage,
                                onNavigateToMcq = { text, count, pageIndex ->
                                    currentScreen = Screen.Mcq(text, count, screen.uri, pageIndex)
                                },
                                onNavigateToChat = { text, pageIndex ->
                                    currentScreen = Screen.Chat(text, screen.uri, pageIndex)
                                },
                                onBack = { currentScreen = Screen.Upload } // Returns smoothly to the upload/history screen
                            )
                        }

                        is Screen.Mcq -> {
                            MCQscreen(
                                pageText = screen.text,
                                mcqCount = screen.count,
                                onNavigateToResult = { score, total, mcqs, answers ->
                                    currentScreen = Screen.Result(score, total, mcqs, answers, screen.uri, screen.returnPage)
                                }
                            )
                        }
                        is Screen.Chat -> {
                            ChatWithBook(
                                pageText = screen.text,
                                onBack = { currentScreen = Screen.SelectPage(screen.uri, screen.returnPage) }
                            )
                        }
                        is Screen.Result -> {
                            ResultScreen(
                                score = screen.score,
                                total = screen.total,
                                mcqs = screen.mcqs,
                                userAnswers = screen.answers,
                                onRestart = { currentScreen = Screen.SelectPage(screen.uri, screen.returnPage) } // Restores the exact page!
                            )
                        }
                    }
                }
            }
        }
    }
}