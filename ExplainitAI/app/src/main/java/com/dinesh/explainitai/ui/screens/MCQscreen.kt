package com.dinesh.explainitai.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dinesh.explainitai.network.ApiService
import com.dinesh.explainitai.network.MCQ
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MCQscreen(
    pageText: String,
    mcqCount: Int, // Receives the custom question count specified by the user
    onNavigateToResult: (Int, Int, List<MCQ>, List<String>) -> Unit
) {
    var mcqs by remember { mutableStateOf<List<MCQ>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }
    // Stores the full option string selected for each question index
    var userAnswers by remember { mutableStateOf<MutableMap<Int, String>>(mutableMapOf()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val successGreen = Color(0xFF10B981) // Modern Emerald Green

    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                // Passes both the page text and the dynamic user-defined count to the server
                val response = ApiService.instance.generateMcqs(
                    mapOf(
                        "text" to pageText,
                        "count" to mcqCount
                    )
                )
                withContext(Dispatchers.Main) {
                    isLoading = false
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        if (body.mcqs.isNullOrEmpty()) {
                            errorMsg = "Server returned empty MCQ list. Check page text length."
                        } else {
                            mcqs = body.mcqs
                        }
                    } else {
                        val errorBodyString = response.errorBody()?.string() ?: "Unknown server error"
                        errorMsg = "API Error (${response.code()}): $errorBodyString"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMsg = "Connection/Parsing Exception: ${e.javaClass.simpleName} - ${e.localizedMessage}"
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    CircularProgressIndicator(color = successGreen)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Crafting your custom $mcqCount MCQs test...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (errorMsg != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else if (mcqs.isNotEmpty()) {
            val currentMcq = mcqs[currentIndex]
            val progress = (currentIndex + 1).toFloat() / mcqs.size

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Progress Section
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Question ${currentIndex + 1} of ${mcqs.size}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${((progress) * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = successGreen,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                // Middle Question & Options Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = currentMcq.question,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        currentMcq.options.forEach { option ->
                            // Check if this option is selected
                            val isSelected = userAnswers[currentIndex] == option

                            val animatedContainerColor by animateColorAsState(
                                targetValue = if (isSelected) successGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                animationSpec = tween(durationMillis = 150), label = ""
                            )
                            val animatedBorderColor by animateColorAsState(
                                targetValue = if (isSelected) successGreen else Color.Transparent,
                                animationSpec = tween(durationMillis = 150), label = ""
                            )

                            Surface(
                                onClick = {
                                    userAnswers = userAnswers.toMutableMap().apply {
                                        this[currentIndex] = option
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = animatedContainerColor,
                                border = BorderStroke(1.5.dp, animatedBorderColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (isSelected) successGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) successGreen else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Navigation Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentIndex > 0) {
                        OutlinedButton(
                            onClick = { currentIndex-- },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Previous", fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Button(
                        onClick = {
                            if (currentIndex < mcqs.size - 1) {
                                currentIndex++
                            } else {
                                var score = 0
                                val answersList = mutableListOf<String>()
                                mcqs.forEachIndexed { idx, item ->
                                    val chosenFullOption = userAnswers[idx] ?: ""
                                    // Safely parse option letter using Regex matching A through D
                                    val chosenLetter = Regex("[A-D]").find(chosenFullOption)?.value?.uppercase() ?: ""
                                    answersList.add(chosenLetter)

                                    if (chosenLetter.equals(item.correctAnswer.trim(), ignoreCase = true)) {
                                        score++
                                    }
                                }
                                onNavigateToResult(score, mcqs.size, mcqs, answersList)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentIndex == mcqs.size - 1) successGreen else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (currentIndex < mcqs.size - 1) "Next" else "Submit Test",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}