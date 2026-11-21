package com.luxmaxx.app.ai

import com.luxmaxx.app.data.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL

class DeepSeekException(msg: String) : Exception(msg)

/** Минимальный клиент для https://api.deepseek.com (совместим с форматом OpenAI). */
object DeepSeekClient {
    private const val URL_CHAT = "https://api.deepseek.com/chat/completions"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun chat(
        apiKey: String,
        messages: List<ChatMessage>,
        temperature: Double = 0.7,
        jsonMode: Boolean = false,
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw DeepSeekException("Добавь API-ключ DeepSeek в Профиле")
        val body = buildJsonObject {
            put("model", "deepseek-chat")
            put("temperature", temperature)
            put("messages", buildJsonArray {
                messages.forEach { m ->
                    add(buildJsonObject { put("role", m.role); put("content", m.content) })
                }
            })
            if (jsonMode) put("response_format", buildJsonObject { put("type", "json_object") })
        }
        val conn = (URL(URL_CHAT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                throw DeepSeekException(
                    when (code) {
                        401 -> "Неверный API-ключ DeepSeek"
                        402 -> "На балансе DeepSeek закончились деньги"
                        429 -> "Слишком много запросов, подожди немного"
                        else -> "Ошибка DeepSeek ($code): ${text.take(200)}"
                    }
                )
            }
            val root = json.parseToJsonElement(text).jsonObject
            root["choices"]!!.jsonArray[0].jsonObject["message"]!!.jsonObject["content"]!!
                .jsonPrimitive.content.trim()
        } catch (e: DeepSeekException) {
            throw e
        } catch (e: java.io.IOException) {
            throw DeepSeekException("Нет соединения с DeepSeek: ${e.message}")
        } finally {
            conn.disconnect()
        }
    }

    /** Оценка КБЖУ по описанию еды. Возвращает список позиций. */
    suspend fun estimateFood(apiKey: String, description: String): List<EstimatedFood> {
        val sys = """Ты нутрициолог. Пользователь описывает, что съел. Оцени калории и БЖУ каждой позиции.
            |Ответь ТОЛЬКО JSON вида {"items":[{"name":"...","kcal":0,"protein":0,"carbs":0,"fat":0}]}.
            |Если вес не указан — бери типичную порцию и укажи её в name.""".trimMargin()
        val raw = chat(apiKey, listOf(ChatMessage("system", sys), ChatMessage("user", description)), 0.2, jsonMode = true)
        return try {
            val items = json.parseToJsonElement(raw).jsonObject["items"] as JsonArray
            items.map {
                val o = it.jsonObject
                EstimatedFood(
                    name = o.str("name"),
                    kcal = o.num("kcal"), protein = o.num("protein"),
                    carbs = o.num("carbs"), fat = o.num("fat"),
                )
            }
        } catch (e: Exception) {
            throw DeepSeekException("Не удалось разобрать ответ ИИ, попробуй переформулировать")
        }
    }

    private fun JsonObject.str(k: String) = (this[k] as? JsonPrimitive)?.content ?: ""
    private fun JsonObject.num(k: String) = (this[k] as? JsonPrimitive)?.content?.toDoubleOrNull()?.toInt() ?: 0
}

data class EstimatedFood(val name: String, val kcal: Int, val protein: Int, val carbs: Int, val fat: Int)
