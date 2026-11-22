package com.luxmaxx.app.ai

import com.luxmaxx.app.data.ChatMessage
import java.net.HttpURLConnection
import java.net.URL
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
        model: String = "deepseek-chat",
        imageJpegBase64: String? = null,   // прикрепляется к последнему сообщению пользователя
        images: List<String> = emptyList(), // несколько фото (base64 JPEG) — тоже к последнему сообщению
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw DeepSeekException("Добавь API-ключ DeepSeek в Профиле")
        val allImages = listOfNotNull(imageJpegBase64) + images
        val body = buildJsonObject {
            put("model", model.trim())
            put("temperature", temperature)
            put("messages", buildJsonArray {
                messages.forEachIndexed { i, m ->
                    add(buildJsonObject {
                        put("role", m.role)
                        if (allImages.isNotEmpty() && i == messages.lastIndex) {
                            // Формат OpenAI-совместимого API: массив частей картинки + текст
                            put("content", buildJsonArray {
                                allImages.forEach { img ->
                                    add(buildJsonObject {
                                        put("type", "image_url")
                                        put("image_url", buildJsonObject { put("url", "data:image/jpeg;base64,$img") })
                                    })
                                }
                                add(buildJsonObject { put("type", "text"); put("text", m.content) })
                            })
                        } else put("content", m.content)
                    })
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
                        400 -> if (allImages.isNotEmpty())
                            "DeepSeek не принял фото (400). Проверь модель для фото в Профиле. Ответ: ${text.take(200)}"
                        else "Ошибка запроса (400): ${text.take(200)}"
                        401 -> "Неверный API-ключ DeepSeek"
                        402 -> "На балансе DeepSeek закончились деньги"
                        429 -> "Слишком много запросов, подожди немного"
                        404 -> "Модель «$model» не найдена — поменяй название модели в Профиле"
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
    suspend fun estimateFood(apiKey: String, model: String, description: String): List<EstimatedFood> {
        val sys = """Ты нутрициолог. Пользователь описывает, что съел. Оцени калории и БЖУ каждой позиции.
            |Ответь ТОЛЬКО JSON вида {"items":[{"name":"...","kcal":0,"protein":0,"carbs":0,"fat":0}]}.
            |Если вес не указан — бери типичную порцию и укажи её в name.""".trimMargin()
        val raw = chat(apiKey, listOf(ChatMessage("system", sys), ChatMessage("user", description)), 0.2, jsonMode = true, model = model)
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

    /** Анализ фото лица. Возвращает оценки и текстовый разбор. */
    suspend fun analyzeFacePhoto(
        apiKey: String, model: String, system: String, context: String, imageJpegBase64: String,
    ): FaceAiResult {
        val prompt = """$context
            |
            |На фото — моё лицо сегодня. Внимательно рассмотри кожу и лицо и ответь ТОЛЬКО JSON:
            |{"skin": 1-10 (общее состояние кожи, 10 — идеально),
            | "acne": 0-10 (высыпания, 0 — нет),
            | "puffiness": 0-10 (отёчность, 0 — нет),
            | "observations": "что конкретно видишь: зоны высыпаний, жирность/сухость, покраснения, тёмные круги, отёки, текстура",
            | "advice": "план на 2 недели по пунктам: уход утро/вечер с активными компонентами, сон, питание, вода, что убрать"}
            |Пиши observations и advice на русском. Будь честным, но тактичным, без оценок «красоты».""".trimMargin()
        val raw = chat(apiKey, listOf(ChatMessage("system", system), ChatMessage("user", prompt)),
            temperature = 0.3, model = model, imageJpegBase64 = imageJpegBase64)
        val start = raw.indexOf('{'); val end = raw.lastIndexOf('}')
        return try {
            val o = json.parseToJsonElement(raw.substring(start, end + 1)).jsonObject
            FaceAiResult(
                skin = o.num("skin").coerceIn(1, 10),
                acne = o.num("acne").coerceIn(0, 10),
                puffiness = o.num("puffiness").coerceIn(0, 10),
                observations = o.str("observations"),
                advice = o.str("advice"),
            )
        } catch (e: Exception) {
            // Модель ответила текстом — показываем как есть
            FaceAiResult(null, null, null, "", raw)
        }
    }

    private fun JsonObject.str(k: String) = (this[k] as? JsonPrimitive)?.content ?: ""
    private fun JsonObject.num(k: String) = (this[k] as? JsonPrimitive)?.content?.toDoubleOrNull()?.toInt() ?: 0
}

data class FaceAiResult(
    val skin: Int?, val acne: Int?, val puffiness: Int?,
    val observations: String, val advice: String,
) {
    fun asText() = listOf(
        observations.takeIf { it.isNotBlank() }?.let { "👀 Что видно:\n$it" },
        advice.takeIf { it.isNotBlank() }?.let { "📋 План:\n$it" },
    ).filterNotNull().joinToString("\n\n")
}

data class EstimatedFood(val name: String, val kcal: Int, val protein: Int, val carbs: Int, val fat: Int)
