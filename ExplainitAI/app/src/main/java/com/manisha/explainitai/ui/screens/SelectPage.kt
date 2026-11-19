package com.manisha.explainitai.ui.screens

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.manisha.explainitai.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@Composable
fun SelectPage(
    pdfUri: Uri,
    initialPage: Int = 0,
    onNavigateToMcq: (String, Int, Int) -> Unit,
    onNavigateToChat: (String, Int) -> Unit,
    onBack: () -> Unit // 1. Add an onBack parameter here
) {
    val context = LocalContext.current
    var currentPageIndex by remember { mutableStateOf(initialPage) }
    var totalPages by remember { mutableStateOf(1) }
    var pdfBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Zoom and pan states
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Popup state for MCQ count dialog
    var showMcqDialog by remember { mutableStateOf(false) }
    var mcqCountInput by remember { mutableStateOf("3") }

    val coroutineScope = rememberCoroutineScope()

    androidx.activity.compose.BackHandler {
        onBack()
    }
    // Initialize PdfRenderer from URI
    DisposableEffect(pdfUri) {
        try {
            val parcelFileDescriptor = run {
                val inputStream: InputStream? = context.contentResolver.openInputStream(pdfUri)
                val tempFile = File(context.cacheDir, "temp_study.pdf")
                val outputStream = FileOutputStream(tempFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            }
            fileDescriptor = parcelFileDescriptor
            val renderer = PdfRenderer(parcelFileDescriptor)
            pdfRenderer = renderer
            totalPages = renderer.pageCount
        } catch (e: Exception) {
            errorMessage = "Failed to load PDF viewer: ${e.localizedMessage}"
        }

        onDispose {
            try {
                pdfRenderer?.close()
                fileDescriptor?.close()
            } catch (_: Exception) {}
        }
    }

    // Render current page to High-Resolution Bitmap with solid white background
    LaunchedEffect(pdfRenderer, currentPageIndex) {
        pdfRenderer?.let { renderer ->
            if (renderer.pageCount > 0) {
                scale = 1f
                offsetX = 0f
                offsetY = 0f

                val page = renderer.openPage(currentPageIndex)
                val bitmap = Bitmap.createBitmap(page.width * 3, page.height * 3, Bitmap.Config.ARGB_8888)

                // Ensures original PDF colors and maximum text clarity/readability
                bitmap.eraseColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                pdfBitmap = bitmap
            }
        }
    }

    // Helper to fetch text and trigger MCQ with user count
    fun handleGenerateMcq(count: Int) {
        isLoading = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(pdfUri)
                val bytes = inputStream?.readBytes() ?: byteArrayOf()
                inputStream?.close()

                val requestFile = bytes.toRequestBody("application/pdf".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("pdf", "document.pdf", requestFile)
                val pageBody = (currentPageIndex + 1).toString().toRequestBody("text/plain".toMediaTypeOrNull())

                val response = ApiService.instance.extractPage(body, pageBody)
                withContext(Dispatchers.Main) {
                    isLoading = false
                    if (response.isSuccessful && response.body() != null) {
                        onNavigateToMcq(response.body()!!.pageText, count, currentPageIndex)
                    } else {
                        errorMessage = "Extraction error for MCQ."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMessage = "Error: ${e.localizedMessage}"
                }
            }
        }
    }

    // Helper to extract text and navigate to Chat Screen, passing current page index
    fun handleChatWithPage() {
        isLoading = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(pdfUri)
                val bytes = inputStream?.readBytes() ?: byteArrayOf()
                inputStream?.close()

                val requestFile = bytes.toRequestBody("application/pdf".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("pdf", "document.pdf", requestFile)
                val pageBody = (currentPageIndex + 1).toString().toRequestBody("text/plain".toMediaTypeOrNull())

                val response = ApiService.instance.extractPage(body, pageBody)
                withContext(Dispatchers.Main) {
                    isLoading = false
                    if (response.isSuccessful && response.body() != null) {
                        onNavigateToChat(response.body()!!.pageText, currentPageIndex)
                    } else {
                        errorMessage = "Extraction error for Chat."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMessage = "Error: ${e.localizedMessage}"
                }
            }
        }
    }

    // MCQ Count Dialog Popup
    if (showMcqDialog) {
        AlertDialog(
            onDismissRequest = { showMcqDialog = false },
            title = { Text("Generate MCQs") },
            text = {
                Column {
                    Text("How many multiple-choice questions do you want to generate for this page?")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = mcqCountInput,
                        onValueChange = { mcqCountInput = it },
                        label = { Text("Number of Questions") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showMcqDialog = false
                    val count = mcqCountInput.toIntOrNull() ?: 3
                    handleGenerateMcq(count)
                }) {
                    Text("Generate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMcqDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Pagination Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                        enabled = currentPageIndex > 0
                    ) {
                        Text("Previous")
                    }

                    Text(
                        text = "Page ${currentPageIndex + 1} of $totalPages",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = { if (currentPageIndex < totalPages - 1) currentPageIndex++ },
                        enabled = currentPageIndex < totalPages - 1
                    ) {
                        Text("Next")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // High-Readability Zoomable & Pannable PDF Page Viewer Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                    if (scale > 1f) {
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    } else {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (pdfBitmap != null) {
                            Image(
                                bitmap = pdfBitmap!!.asImageBitmap(),
                                contentDescription = "Zoomable PDF Page Viewer",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offsetX,
                                        translationY = offsetY
                                    )
                            )
                        } else {
                            CircularProgressIndicator()
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons Side-by-Side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showMcqDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Generate MCQ", style = MaterialTheme.typography.bodyMedium)
                    }

                    Button(
                        onClick = { handleChatWithPage() },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ask Doubts",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Full screen loader when fetching from server
            if (isLoading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Processing page with AI...",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}