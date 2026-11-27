package com.luxmaxx.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luxmaxx.app.ai.DeepSeekClient
import com.luxmaxx.app.ai.DeepSeekException
import com.luxmaxx.app.ai.EstimatedFood
import com.luxmaxx.app.ai.FaceAiResult
import com.luxmaxx.app.data.AppData
import com.luxmaxx.app.data.ChatMessage
import com.luxmaxx.app.data.Equip
import com.luxmaxx.app.data.FaceCheck
import com.luxmaxx.app.data.FoodEntry
import com.luxmaxx.app.data.Looks
import com.luxmaxx.app.data.Profile
import com.luxmaxx.app.data.Repository
import com.luxmaxx.app.data.SavedProgram
import com.luxmaxx.app.data.SavedWorkoutDay
import com.luxmaxx.app.data.SavedPlannedExercise
import com.luxmaxx.app.data.SetEntry
import com.luxmaxx.app.data.WeightEntry
import com.luxmaxx.app.data.WorkoutLog
import com.luxmaxx.app.face.FaceAnalyzer
import com.luxmaxx.app.face.FaceResult
import com.luxmaxx.app.face.ImageEncoder
import com.luxmaxx.app.logic.Calculator
import com.luxmaxx.app.logic.HomeWorkouts
import com.luxmaxx.app.logic.Program
import com.luxmaxx.app.logic.Programs
import com.luxmaxx.app.logic.Progress
import com.luxmaxx.app.logic.Targets
import com.luxmaxx.app.ui.theme.AppTheme
import com.luxmaxx.app.ui.theme.ThemeState
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

fun today(): String = LocalDate.now().toString()

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)
    private val _data = MutableStateFlow(repo.load())
    val data: StateFlow<AppData> = _data.asStateFlow()
    private val saveQueue = Channel<AppData>(Channel.UNLIMITED)
    private val stateJson = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    val proposedProgram = MutableStateFlow<SavedProgram?>(null)
    val aiBusy = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)   // снекбар

    init {
        ThemeState.apply(themeOf(_data.value.theme))
        repo.loadWarning?.let { message.value = it }
        viewModelScope.launch {
            for (snapshot in saveQueue) {
                try { repo.save(snapshot) }
                catch (e: Exception) { message.value = "Не удалось сохранить данные: ${e.message}" }
            }
        }
    }

    private fun themeOf(name: String) = AppTheme.entries.firstOrNull { it.name == name } ?: AppTheme.GOLD

    fun setTheme(t: AppTheme) {
        ThemeState.apply(t)
        mutate { it.copy(theme = t.name) }
    }

    /** Праздничный экран: новая ачивка, новый уровень или завершённая тренировка. */
    data class Celebration(val emoji: String, val title: String, val subtitle: String, val color: Long = 0xFFFFC14D)
    val celebration = MutableStateFlow<Celebration?>(null)
    private val celebrationQueue = ArrayDeque<Celebration>()

    fun celebrate(c: Celebration) {
        if (celebration.value == null) celebration.value = c else celebrationQueue.addLast(c)
    }

    fun dismissCelebration() {
        celebration.value = celebrationQueue.removeFirstOrNull()
    }

    private fun mutate(block: (AppData) -> AppData) {
        val news = mutableListOf<Celebration>()
        _data.update { old ->
            var d = block(old)
            if (d.profile != null) {
                val protein = targets(d)?.protein ?: 140
                val unlocked = Progress.unlockedIds(d, protein)
                val fresh = unlocked - d.seenAchievements
                val lvl = Progress.level(d, protein)
                // при первом запуске не засыпаем пользователя старыми ачивками
                val firstSync = !d.achievementsInit
                if (firstSync) d = d.copy(achievementsInit = true)
                if (fresh.isNotEmpty()) {
                    if (!firstSync) Progress.achievements.filter { it.id in fresh }.forEach {
                        news += Celebration(it.emoji, "Ачивка: ${it.title}", it.description, it.color)
                    }
                    d = d.copy(seenAchievements = d.seenAchievements + fresh)
                }
                if (lvl.level > d.seenLevel) {
                    if (!firstSync) news += Celebration(lvl.rank.emoji, "Уровень ${lvl.level}!", "Твой ранг: ${lvl.rank.title}")
                    d = d.copy(seenLevel = lvl.level)
                }
            }
            d
        }
        news.forEach { celebrate(it) }
        saveQueue.trySend(_data.value)
    }

    fun levelInfo(d: AppData = data.value) = Progress.level(d, targets(d)?.protein ?: 140)

    // ---------- Производные значения ----------
    fun currentWeight(d: AppData = data.value): Double =
        d.weights.maxByOrNull { it.date }?.kg ?: d.profile?.weightKg ?: 70.0

    fun targets(d: AppData = data.value): Targets? = d.profile?.let { Calculator.targets(it, currentWeight(d)) }

    fun program(d: AppData = data.value): Program? =
        d.profile?.let {
            val saved = d.customProgram?.let { custom ->
                Program(custom.title, custom.description, custom.days.map { day ->
                    com.luxmaxx.app.logic.WorkoutDay(day.name, day.exercises.map { ex ->
                        com.luxmaxx.app.logic.PlannedExercise(ex.name, ex.sets, ex.repMin, ex.repMax, ex.tip)
                    })
                })
            } ?: Programs.forProfile(it.trainingDays, it.experience)
            if (d.trainAtHome && d.customProgram == null) HomeWorkouts.toHome(saved, d.homeEquipment) else saved
        }

    fun applyProposedProgram() {
        val proposed = proposedProgram.value ?: return
        mutate { old -> old.copy(
            customProgram = proposed,
            profile = old.profile?.copy(trainingDays = proposed.days.size.coerceIn(2, 6)),
        ) }
        proposedProgram.value = null
        message.value = "Новая программа тренировок применена"
    }

    fun discardProposedProgram() { proposedProgram.value = null }

    fun importChatPhoto(uri: Uri): File? = importFacePhoto(uri, "chat")

    fun setTrainAtHome(home: Boolean) = mutate { it.copy(trainAtHome = home) }
    fun toggleEquip(e: Equip) = mutate {
        it.copy(homeEquipment = if (e in it.homeEquipment) it.homeEquipment - e else it.homeEquipment + e)
    }

    // ---------- Профиль ----------
    fun saveProfile(p: Profile) = mutate {
        val weights = if (it.weights.none { w -> w.date == today() })
            it.weights + WeightEntry(today(), p.weightKg) else it.weights
        it.copy(profile = p, weights = weights)
    }

    fun setApiKey(k: String) = mutate { it.copy(apiKey = k.trim()) }

    fun setModels(text: String, vision: String) = mutate {
        it.copy(textModel = text.trim().ifBlank { "deepseek-chat" }, visionModel = vision.trim().ifBlank { "deepseek-flash" })
    }

    // ---------- Вес ----------
    fun addWeight(kg: Double) = mutate {
        it.copy(weights = it.weights.filterNot { w -> w.date == today() } + WeightEntry(today(), kg))
    }

    // ---------- Тренировки ----------
    val activeDay = MutableStateFlow<String?>(null)
    val activeSets = MutableStateFlow<List<SetEntry>>(emptyList())
    /** Замены упражнений в текущей тренировке: плановое название -> фактическое. */
    val activeSwaps = MutableStateFlow<Map<String, String>>(emptyMap())

    fun startWorkout(day: String) { activeDay.value = day; activeSets.value = emptyList(); activeSwaps.value = emptyMap() }

    fun swapExercise(planned: String, replacement: String) {
        val old = activeSwaps.value[planned] ?: planned
        // подходы, уже записанные под старым названием, переносим на новое
        activeSets.update { list -> list.map { if (it.exercise == old) it.copy(exercise = replacement) else it } }
        activeSwaps.update { if (replacement == planned) it - planned else it + (planned to replacement) }
    }
    fun addSet(s: SetEntry) { activeSets.update { it + s } }
    fun removeSet(index: Int) { activeSets.update { it.filterIndexed { i, _ -> i != index } } }
    fun cancelWorkout() { activeDay.value = null; activeSets.value = emptyList(); activeSwaps.value = emptyMap() }

    fun finishWorkout() {
        val day = activeDay.value ?: return
        val sets = activeSets.value
        if (sets.isNotEmpty()) {
            celebrate(Celebration("💪", "Тренировка завершена!",
                "Объём ${sets.sumOf { it.weight * it.reps }.toInt()} кг · ${sets.size} подходов · +${100 + sets.size * 5} XP"))
            mutate { it.copy(workouts = it.workouts + WorkoutLog(System.currentTimeMillis(), today(), day, sets)) }
        }
        cancelWorkout()
    }

    fun deleteWorkout(id: Long) = mutate { it.copy(workouts = it.workouts.filterNot { w -> w.id == id }) }

    // ---------- Питание ----------
    fun addFood(name: String, kcal: Int, p: Int, c: Int, f: Int) = mutate {
        it.copy(foods = it.foods + FoodEntry(System.nanoTime(), today(), name, kcal, p, c, f))
    }

    fun deleteFood(id: Long) = mutate { it.copy(foods = it.foods.filterNot { f -> f.id == id }) }

    fun estimateFood(text: String, onResult: (List<EstimatedFood>) -> Unit) = ai {
        onResult(DeepSeekClient.estimateFood(data.value.apiKey, data.value.textModel, text))
    }

    // ---------- Лицо ----------
    fun newFacePhotoFile(prefix: String = "face"): File {
        val dir = File(getApplication<Application>().filesDir, "faces").apply { mkdirs() }
        return File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
    }

    fun importFacePhoto(uri: Uri, prefix: String = "face"): File? = try {
        val f = newFacePhotoFile(prefix)
        getApplication<Application>().contentResolver.openInputStream(uri)!!.use { inp ->
            f.outputStream().use { inp.copyTo(it) }
        }
        f
    } catch (e: Exception) { message.value = "Не удалось скопировать фото"; null }

    suspend fun analyzeFace(file: File): FaceResult =
        FaceAnalyzer.analyze(getApplication<Application>(), Uri.fromFile(file))

    /** AI смотрит свежее фото и выставляет оценки (пользователь потом может поправить). */
    fun analyzeFacePhotoAi(file: File, notes: String, onResult: (FaceAiResult) -> Unit) = ai {
        val d = data.value
        val history = d.faceChecks.sortedBy { it.date }.takeLast(10).joinToString("\n") { faceLine(it) }
        val res = DeepSeekClient.analyzeFacePhoto(
            d.apiKey, d.visionModel, systemPrompt(),
            "История моих прошлых отметок (старые сверху):\n${history.ifBlank { "нет" }}\nЗаметки сегодня: ${notes.ifBlank { "нет" }}",
            ImageEncoder.toJpegBase64(file),
        )
        onResult(res)
    }

    fun saveFaceCheck(check: FaceCheck) = mutate {
        it.copy(faceChecks = it.faceChecks.filterNot { c -> c.id == check.id } + check)
    }

    fun deleteFaceCheck(check: FaceCheck) {
        check.photoPath?.let { File(it).delete() }
        mutate { it.copy(faceChecks = it.faceChecks.filterNot { c -> c.id == check.id }) }
    }

    fun toggleRoutine(item: String) = mutate {
        val done = it.faceRoutineDone[today()].orEmpty()
        val newDone = if (item in done) done - item else done + item
        // храним только последние 60 дней
        val map = (it.faceRoutineDone + (today() to newDone)).toList().sortedBy { p -> p.first }.takeLast(60).toMap()
        it.copy(faceRoutineDone = map)
    }

    fun faceAdvice(check: FaceCheck) = ai {
        val d = data.value
        val history = d.faceChecks.sortedBy { it.date }.takeLast(10).joinToString("\n") { faceLine(it) }
        val prompt = """Вот мои последние отметки состояния лица (старые сверху):
            |$history
            |
            |Сегодня: ${faceLine(check)}
            |Заметки: ${check.notes.ifBlank { "нет" }}
            |
            |Проанализируй динамику и дай конкретный план на ближайшие 2 недели: уход за кожей (утро/вечер, активные компоненты),
            |сон, вода, соль/сахар/молочка, отёки, тренировки и питание с учётом набора массы. Коротко, по пунктам.""".trimMargin()
        val photo = check.photoPath?.let { File(it) }?.takeIf { it.exists() }
        val answer = if (photo != null) {
            // Есть фото — отправляем его модели с компьютерным зрением
            DeepSeekClient.analyzeFacePhoto(
                d.apiKey, d.visionModel, systemPrompt(),
                "История отметок:\n$history\nЗаметки сегодня: ${check.notes.ifBlank { "нет" }}",
                ImageEncoder.toJpegBase64(photo),
            ).asText()
        } else {
            DeepSeekClient.chat(d.apiKey, listOf(ChatMessage("system", systemPrompt()), ChatMessage("user", prompt)), model = d.textModel)
        }
        saveFaceCheck(check.copy(aiAdvice = answer))
    }

    private fun faceLine(c: FaceCheck): String {
        val m = c.metrics
        return "${c.date}: кожа ${c.skin}/10, высыпания ${c.acne}/10, отёчность ${c.puffiness}/10, сон ${c.sleepHours} ч, вода ${c.waterLiters} л" +
            (m?.let { ", симметрия ${it.symmetry}%, индекс ширины лица ${it.widthIndex}" } ?: "")
    }

    // ---------- Образ: кумир, волосы, одежда ----------
    fun updateLooks(block: (Looks) -> Looks) = mutate { it.copy(looks = block(it.looks)) }

    /** Заменяет фото в разделе «Образ», удаляя старый файл. */
    fun setLooksPhoto(old: String?, new: String, block: (Looks, String) -> Looks) {
        if (old != null && old != new) File(old).delete()
        updateLooks { block(it, new) }
    }

    private fun latestFacePhoto(): File? = data.value.faceChecks.sortedByDescending { it.id }
        .firstNotNullOfOrNull { c -> c.photoPath?.let { File(it) }?.takeIf { it.exists() } }

    private suspend fun photos(vararg paths: String?): List<String> =
        paths.mapNotNull { p -> p?.let { File(it) }?.takeIf { it.exists() } }.map { ImageEncoder.toJpegBase64(it) }

    private suspend fun askVision(prompt: String, images: List<String>): String {
        val d = data.value
        return DeepSeekClient.chat(
            d.apiKey, listOf(ChatMessage("system", systemPrompt()), ChatMessage("user", prompt)),
            temperature = 0.5, model = if (images.isEmpty()) d.textModel else d.visionModel, images = images,
        )
    }

    fun analyzeIdol() = ai {
        val l = data.value.looks
        val idolImg = photos(l.idolPhotoPath)
        val me = latestFacePhoto()
        val meImg = photos(me?.absolutePath)
        val prompt = """Мой кумир / ориентир по внешности: ${l.idolName.ifBlank { "без имени" }}.
            |${if (idolImg.isNotEmpty()) "Первое фото — кумир." else "Фото кумира нет."}
            |${if (meImg.isNotEmpty()) "Второе фото — я сейчас." else "Моего фото нет, ориентируйся на мой профиль."}
            |
            |Помоги мне приблизиться к этому образу РЕАЛИСТИЧНО. Разбери по пунктам:
            |1. 💪 Телосложение: какой у него примерно % жира и какие мышцы доминируют; что мне качать в приоритете, какой вес/состав тела целиться и за какой срок это реально.
            |2. 🍽 Питание: нужно ли мне сейчас набирать или сначала подсушиться, чтобы прийти к такому виду.
            |3. 💇 Волосы и уход: стрижка, борода/щетина, брови, кожа — что перенять.
            |4. 👕 Стиль: как он одевается и что из этого подойдёт мне.
            |5. ⚖️ Честно: что зависит от генетики и чего не стоит ждать — без самоуничижения, фокус на том, что можно изменить.
            |Не оценивай «красоту» по шкале, будь конкретным и мотивирующим.""".trimMargin()
        val answer = askVision(prompt, idolImg + meImg)
        updateLooks { it.copy(idolAnalysis = answer) }
    }

    fun analyzeHair() = ai {
        val l = data.value.looks
        val img = photos(l.hairPhotoPath ?: latestFacePhoto()?.absolutePath)
        val prompt = """Помоги с волосами. Тип: ${l.hairType}, длина: ${l.hairLength}.
            |Проблемы: ${l.hairIssues.ifEmpty { listOf("нет") }.joinToString()}.
            |${if (img.isNotEmpty()) "На фото — я. Определи форму лица и текущее состояние волос." else ""}
            |${if (l.idolName.isNotBlank()) "Мой ориентир по внешности: ${l.idolName}." else ""}
            |Дай:
            |1. Какие 2–3 стрижки подойдут к моей форме лица и как попросить барбера (конкретными словами).
            |2. Уход: как часто мыть, какой шампунь/кондиционер (по составу), укладка и средства.
            |3. Решение моих проблем по пунктам. Если есть выпадение — скажи честно, когда пора к трихологу/дерматологу и какие методы научно доказаны.
            |4. Борода/щетина и брови — если уместно.""".trimMargin()
        val answer = askVision(prompt, img)
        updateLooks { it.copy(hairAdvice = answer) }
    }

    fun analyzeOutfit() = ai {
        val l = data.value.looks
        val img = photos(l.outfitPhotoPath)
        if (img.isEmpty()) throw DeepSeekException("Сначала добавь фото образа")
        val prompt = """На фото — мой образ (одежда). Цель стиля: ${l.styleGoal.ifBlank { "выглядеть мужественно и аккуратно" }}.
            |${if (l.idolName.isNotBlank()) "Ориентир: ${l.idolName}." else ""}
            |Разбери: посадка по фигуре (плечи, длина, ширина), цвета и сочетания, обувь, общее впечатление.
            |Что поменять, чтобы фигура выглядела мощнее и спортивнее? 3–5 конкретных правок по приоритету.""".trimMargin()
        val answer = askVision(prompt, img)
        updateLooks { it.copy(outfitAdvice = answer) }
    }

    fun wardrobePlan() = ai {
        val l = data.value.looks
        val prompt = """Составь мне базовый капсульный гардероб. Цель стиля: ${l.styleGoal.ifBlank { "универсальный, мужественный" }}.
            |Бюджет: ${l.budget}. ${if (l.idolName.isNotBlank()) "Ориентир: ${l.idolName}." else ""}
            |Учитывай мой рост и вес из профиля и то, что я набираю мышечную массу.
            |Дай: список 12–15 вещей (с цветами и фасонами), 5 готовых сочетаний, на что смотреть при примерке для спортивной фигуры,
            |и примерный бюджет. Коротко, по пунктам.""".trimMargin()
        val answer = askVision(prompt, emptyList())
        updateLooks { it.copy(wardrobeAdvice = answer) }
    }

    // ---------- AI-тренер ----------
    fun sendChat(text: String, imagePath: String? = null) {
        if (text.isBlank() && imagePath.isNullOrBlank()) return
        val userText = text.trim().ifBlank { "Проанализируй прикреплённое фото и дай рекомендации с учётом моих данных в Luxxmaxx." }
        mutate { it.copy(chat = it.chat + ChatMessage("user", userText, imagePath)) }
        ai {
            val d = data.value
            // AI получает структурированные данные приложения целиком. Секрет API и внутренние пути к файлам не передаются.
            val safeData = d.copy(
                apiKey = "",
                chat = d.chat.map { it.copy(imagePath = null) },
                faceChecks = d.faceChecks.map { it.copy(photoPath = null) },
                looks = d.looks.copy(idolPhotoPath = null, hairPhotoPath = null, outfitPhotoPath = null),
            )
            val contextJson = stateJson.encodeToString(AppData.serializer(), safeData)
            val agentRules = """

ПОЛНЫЙ КОНТЕКСТ ДАННЫХ ПРИЛОЖЕНИЯ (JSON):
$contextJson

Ты можешь предложить изменение тренировочной программы. Создавай action ТОЛЬКО если пользователь прямо просит составить, изменить или перестроить программу; при обычном обсуждении или вопросе actions=[] . Не утверждай, что что-либо уже изменено. Если пользователь прямо просит изменить программу, верни JSON с reply и actions
val history = d.chat.takeLast(30).map { ChatMessage(it.role, it.content) }
            val imageFile = imagePath?.let { File(it) }?.takeIf { it.exists() }
            val imageBase64 = imageFile?.let { ImageEncoder.toJpegBase64(it) }
            val raw = DeepSeekClient.chat(
                d.apiKey,
                listOf(ChatMessage("system", systemPrompt() + agentRules)) + history,
                temperature = 0.4,
                jsonMode = true,
                model = if (imageBase64 != null) d.visionModel else d.textModel,
                imageJpegBase64 = imageBase64,
            )
            val parsed = runCatching { stateJson.parseToJsonElement(raw).jsonObject }.getOrNull()
            val reply = parsed?.get("reply")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: raw
            val plan = runCatching {
                parsed?.get("actions")?.jsonArray?.firstNotNullOfOrNull { element ->
                    runCatching { parseProgramAction(element.jsonObject) }.getOrNull()
                }
            }.getOrNull()
            if (plan != null) proposedProgram.value = plan
            mutate { it.copy(chat = it.chat + ChatMessage("assistant", reply)) }
        }
    }

    private fun parseProgramAction(action: JsonObject): SavedProgram? {
        if (action["type"]?.jsonPrimitive?.contentOrNull != "replace_training_plan") return null
        val title = action["title"]?.jsonPrimitive?.contentOrNull?.take(80)?.ifBlank { "Моя программа" } ?: "Моя программа"
        val description = action["description"]?.jsonPrimitive?.contentOrNull?.take(500) ?: "Программа составлена AI-тренером"
        val daysJson = action["days"]?.jsonArray ?: return null
        if (daysJson.isEmpty() || daysJson.size > 7) return null
        val days = daysJson.mapIndexed { index, dayElement ->
            val day = dayElement.jsonObject
            val name = day["name"]?.jsonPrimitive?.contentOrNull?.take(60)?.ifBlank { "День ${index + 1}" } ?: "День ${index + 1}"
            val exercisesJson = day["exercises"]?.jsonArray ?: JsonArray(emptyList())
            require(exercisesJson.isNotEmpty() && exercisesJson.size <= 15) { "Некорректное количество упражнений" }
            val exercises = exercisesJson.map { exElement ->
                val ex = exElement.jsonObject
                val exName = ex["name"]?.jsonPrimitive?.contentOrNull?.take(100)?.trim().orEmpty()
                val sets = ex["sets"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 3
                val min = ex["repMin"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 8
                val max = ex["repMax"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 12
                require(exName.isNotBlank() && sets in 1..8 && min in 1..30 && max in min..40)
                SavedPlannedExercise(exName, sets, min, max, ex["tip"]?.jsonPrimitive?.contentOrNull?.take(250) ?: "")
            }
            SavedWorkoutDay(name, exercises)
        }
        return SavedProgram(title, description, days)
    }

    fun clearChat() {
        data.value.chat.mapNotNull { it.imagePath }.distinct().forEach { File(it).delete() }
        mutate { it.copy(chat = emptyList()) }
    }

    /** Системный промпт с актуальными данными пользователя — чтобы советы были персональными. */
    fun systemPrompt(): String {
        val d = data.value
        val p = d.profile
        val t = targets(d)
        val todayFoods = d.foods.filter { it.date == today() }
        val recentWorkouts = d.workouts.sortedByDescending { it.id }.take(5).joinToString("\n") { w ->
            "- ${w.date} ${w.dayName}: " + w.sets.groupBy { it.exercise }.entries.joinToString("; ") { (ex, s) ->
                "$ex ${s.joinToString(",") { "${it.weight}×${it.reps}" }}"
            }
        }
        val weights = d.weights.sortedBy { it.date }.takeLast(14).joinToString { "${it.date}: ${it.kg}" }
        val lastFace = d.faceChecks.maxByOrNull { it.id }?.let { faceLine(it) } ?: "нет"
        return """Ты — персональный тренер, нутрициолог и консультант по уходу за кожей в приложении LuxMaxx.
            |Цель пользователя: максимально набрать мышечную массу и улучшить внешний вид/состояние лица.
            |Отвечай на русском, по-дружески, кратко и конкретно, опирайся на научные данные.
            |Не ставь медицинских диагнозов; при тревожных симптомах советуй врача/дерматолога. Не советуй стероиды, SARMs и опасные практики.
            |
            |Профиль: ${p?.let { "${if (it.sex.name == "MALE") "мужчина" else "женщина"}, ${it.age} лет, рост ${it.heightCm} см, вес ${currentWeight(d)} кг, опыт: ${it.experience.title}, тренировок в неделю: ${it.trainingDays}" } ?: "не заполнен"}
            |Цели в день: ${t?.let { "${it.kcal} ккал, белок ${it.protein} г, жиры ${it.fat} г, углеводы ${it.carbs} г" } ?: "-"}
            |Сегодня съедено: ${todayFoods.sumOf { it.kcal }} ккал, белок ${todayFoods.sumOf { it.protein }} г
            |Вес (последние записи): ${weights.ifBlank { "нет" }}
            |Программа: ${program(d)?.title ?: "-"}
            |Где тренируется: ${if (d.trainAtHome) "дома, инвентарь: " + (d.homeEquipment.joinToString { it.title }.ifBlank { "только вес тела" }) else "в зале"}
            |Последние тренировки:
            |${recentWorkouts.ifBlank { "нет" }}
            |Последняя отметка лица: $lastFace
            |Ориентир по внешности (кумир): ${d.looks.idolName.ifBlank { "не задан" }}${d.looks.idolAnalysis?.let { "\nКраткий разбор кумира: ${it.take(700)}" } ?: ""}
            |Волосы: ${d.looks.hairType}, ${d.looks.hairLength}, проблемы: ${d.looks.hairIssues.ifEmpty { listOf("нет") }.joinToString()}
            |Цель по стилю: ${d.looks.styleGoal.ifBlank { "не задана" }}""".trimMargin()
    }

    private fun ai(block: suspend () -> Unit) {
        if (aiBusy.value) return
        viewModelScope.launch {
            aiBusy.value = true
            try { block() } catch (e: DeepSeekException) { message.value = e.message }
            catch (e: Exception) { message.value = "Ошибка: ${e.message}" }
            finally { aiBusy.value = false }
        }
    }
}
