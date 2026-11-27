package com.luxmaxx.app.data

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Надёжное локальное хранилище с атомарной записью и резервной копией. */
class Repository(context: Context) {
    private val dir = context.filesDir
    private val file = File(dir, "luxmaxx_data.json")
    private val backup = File(dir, "luxmaxx_data.json.bak")
    // Старые версии приложения сохраняли повреждённый файл под этим именем.
    private val legacyBroken = File(dir, "luxmaxx_data.broken.json")
    private val temp = File(dir, "luxmaxx_data.json.tmp")
    private val backupTemp = File(dir, "luxmaxx_data.json.bak.tmp")
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Volatile var loadWarning: String? = null
        private set
    @Volatile private var unrecoverableCorruption = false
    @Volatile private var recoveredFromBackup = false

    fun load(): AppData {
        loadWarning = null
        unrecoverableCorruption = false
        recoveredFromBackup = false
        if (!file.exists() && !backup.exists() && !legacyBroken.exists()) return AppData()

        fun decode(source: File): AppData = json.decodeFromString(AppData.serializer(), source.readText())
        fun hasUserData(d: AppData) = d.profile != null || d.weights.isNotEmpty() || d.workouts.isNotEmpty() ||
            d.foods.isNotEmpty() || d.faceChecks.isNotEmpty() || d.chat.isNotEmpty() || d.apiKey.isNotBlank() ||
            d.looks != Looks() || d.customProgram != null || d.faceRoutineDone.isNotEmpty()

        var current: AppData? = null
        var emptyBackup: AppData? = null
        if (file.exists()) {
            try {
                current = decode(file)
                if (hasUserData(current)) return current
            } catch (_: Exception) {
                // Не трогаем повреждённый файл: пробуем резервные копии.
            }
        }
        if (backup.exists()) {
            try {
                val recovered = decode(backup)
                if (hasUserData(recovered)) {
                    recoveredFromBackup = true
                    loadWarning = "Luxxmaxx восстановил данные из резервной копии. Проверь последние изменения."
                    return recovered
                } else emptyBackup = recovered
            } catch (_: Exception) {
                // Пробуем резервную копию старых версий приложения.
            }
        }
        if (legacyBroken.exists()) {
            try {
                val recovered = decode(legacyBroken)
                if (hasUserData(recovered)) {
                    recoveredFromBackup = true
                    loadWarning = "Найдена и восстановлена резервная копия, созданная предыдущей версией Luxxmaxx после ошибки чтения. Проверь свои данные."
                    return recovered
                }
            } catch (_: Exception) {
                // Ни один источник не удалось прочитать.
            }
        }
        if (current != null) return current
        if (emptyBackup != null) {
            recoveredFromBackup = true
            loadWarning = "Загружена пустая резервная копия. Исходные повреждённые файлы сохранены."
            return emptyBackup
        }
        unrecoverableCorruption = true
        loadWarning = "Не удалось прочитать данные Luxxmaxx. Исходные файлы сохранены; приложение не будет перезаписывать их пустым профилем."
        return AppData()
    }

    suspend fun save(data: AppData) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (unrecoverableCorruption) throw IOException("Хранилище повреждено. Запись заблокирована, чтобы не уничтожить исходные данные.")
            dir.mkdirs()
            val encoded = json.encodeToString(AppData.serializer(), data)
            FileOutputStream(temp, false).use { stream ->
                stream.write(encoded.toByteArray(Charsets.UTF_8))
                stream.flush()
                stream.fd.sync()
            }
            if (file.exists() && !recoveredFromBackup) {
                try {
                    Files.copy(file.toPath(), backupTemp.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    try {
                        Files.move(backupTemp.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                    } catch (_: Exception) {
                        if (backup.exists() && !backup.delete()) throw IOException("Не удалось обновить резервную копию")
                        if (!backupTemp.renameTo(backup)) throw IOException("Не удалось сохранить резервную копию")
                    }
                } catch (e: Exception) {
                    temp.delete()
                    backupTemp.delete()
                    throw IOException("Не удалось создать резервную копию данных: ${e.message}", e)
                }
            }
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: Exception) {
                // На некоторых файловых системах ATOMIC_MOVE недоступен.
                if (temp.exists()) {
                    if (file.exists() && !backup.exists()) Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    if (file.exists() && !file.delete()) throw IOException("Не удалось заменить файл данных")
                    if (!temp.renameTo(file)) throw IOException("Не удалось сохранить файл данных")
                } else if (!file.exists()) {
                    throw IOException("Не удалось сохранить файл данных")
                }
            }
            recoveredFromBackup = false
        }
    }
}
