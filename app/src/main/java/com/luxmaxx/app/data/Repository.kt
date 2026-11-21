package com.luxmaxx.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/** Хранит все данные локально в одном JSON-файле. Ничего не уходит в сеть, кроме запросов к DeepSeek. */
class Repository(context: Context) {
    private val file = File(context.filesDir, "luxmaxx_data.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun load(): AppData = try {
        if (file.exists()) json.decodeFromString(AppData.serializer(), file.readText()) else AppData()
    } catch (e: Exception) {
        // Если файл повреждён — сохраняем копию и начинаем с чистого листа
        file.copyTo(File(file.parentFile, "luxmaxx_data.broken.json"), overwrite = true)
        AppData()
    }

    suspend fun save(data: AppData) = withContext(Dispatchers.IO) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json.encodeToString(AppData.serializer(), data))
        tmp.renameTo(file)
    }
}
