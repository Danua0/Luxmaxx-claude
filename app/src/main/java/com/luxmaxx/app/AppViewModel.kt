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
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun today(): String = LocalDate.now().toString()

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)
    private val _data = MutableStateFlow(repo.load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    val aiBusy = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)   // снекбар

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
        viewModelScope.launch { repo.save(_data.value) }
    }

    fun levelInfo(d: AppData = data.value) = Progress.level(d, targets(d)?.protein ?: 140)

    // ---------- Производные значения ----------
    fun currentWeight(d: AppData = data.value): Double =
        d.weights.maxByOrNull { it.date }?.kg ?: d.profile?.weightKg ?: 70.0

    fun targets(d: AppData = data.value): Targets? = d.profile?.let { Calculator.targets(it, currentWeight(d)) }

    fun program(d: AppData = data.value): Program? =
        d.profile?.let {
            val gym = Programs.forProfile(it.trainingDays, it.experience)
            if (d.trainAtHome) HomeWorkouts.toHome(gym, d.homeEquipment) else gym
        }

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
    fun sendChat(text: String) {
        if (text.isBlank()) return
        mutate { it.copy(chat = it.chat + ChatMessage("user", text.trim())) }
        ai {
            val history = data.value.chat.takeLast(20)
            val answer = DeepSeekClient.chat(data.value.apiKey, listOf(ChatMessage("system", systemPrompt())) + history,
                model = data.value.textModel)
            mutate { it.copy(chat = it.chat + ChatMessage("assistant", answer)) }
        }
    }

    fun clearChat() = mutate { it.copy(chat = emptyList()) }

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
