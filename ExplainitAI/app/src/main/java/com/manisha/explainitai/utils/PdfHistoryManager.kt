package com.manisha.explainitai.utils

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream

data class PdfHistoryItem(
    val filePath: String,
    val fileName: String,
    val timestamp: Long = System.currentTimeMillis()
)

object PdfHistoryManager {
    private const val PREF_NAME = "pdf_history_prefs"
    private const val KEY_HISTORY = "history_list"

    fun savePdfToInternalStorage(context: Context, uri: Uri, fileName: String): Uri {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.filesDir, "saved_${System.currentTimeMillis()}_$fileName")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return Uri.fromFile(file)
    }

    fun getHistory(context: Context): List<PdfHistoryItem> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        val type = object : TypeToken<List<PdfHistoryItem>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun addHistoryItem(context: Context, item: PdfHistoryItem) {
        val currentList = getHistory(context).toMutableList()
        // Prevent duplicate file paths
        if (!currentList.any { it.filePath == item.filePath }) {
            currentList.add(0, item) // Add to top
            saveList(context, currentList)
        }
    }

    fun removeHistoryItem(context: Context, item: PdfHistoryItem) {
        val currentList = getHistory(context).toMutableList()
        currentList.removeAll { it.filePath == item.filePath }
        saveList(context, currentList)

        // Delete physical file from internal storage
        try {
            val file = File(item.filePath)
            if (file.exists()) file.delete()
        } catch (_: Exception) {}
    }

    private fun saveList(context: Context, list: List<PdfHistoryItem>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(list)
        prefs.edit().putString(KEY_HISTORY, json).apply()
    }
}