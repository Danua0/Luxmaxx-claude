package com.luxmaxx.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luxmaxx.app.ai.DeepSeekClient
import com.luxmaxx.app.ai.DeepSeekException
import com.luxmaxx.app.ai.EstimatedFood
import com.luxmaxx.app.data.AppData
import com.luxmaxx.app.data.ChatMessage
import com.luxmaxx.app.data.FaceCheck
import com.luxmaxx.app.data.FoodEntry
import com.luxmaxx.app.data.Profile
import com.luxmaxx.app.data.Repository
import com.luxmaxx.app.data.SetEntry
import com.luxmaxx.app.data.WeightEntry
import com.luxmaxx.app.data.WorkoutLog
import com.luxmaxx.app.face.FaceAnalyzer
import com.luxmaxx.app.face.FaceResult
import com.luxmaxx.app.logic.Calculator
import com.luxmaxx.app.logic.Program
import com.luxmaxx.app.logic.Programs
import com.luxmaxx.app.logic.Targets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

fun today(): String = LocalDate.now().toString()

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)
    private val _data = MutableStateFlow(repo.load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    val aiBusy = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)   // снекбар

    private fun mutate(block: (AppData) -> AppData) {
        _data.update(block)
        viewModelScope.launch { repo.save(_data.value) }
    }

    // ---------- Производные значения ----------
    fun currentWeight(d: AppData = data.value): Double =
        d.weights.maxByOrNull { it.date }?.kg ?: d.profile?.weightKg ?: 70.0

    fun targets(d: AppData = data.value): Targets? = d.profile?.let { Calculator.targets(it, currentWeight(d)) }

    fun program(d: AppData = data.value): Program? =
        d.profile?.let { Programs.forProfile(it.trainingDays, it.experience) }

    // ---------- Профиль ----------
    fun saveProfile(p: Profile) = mutate {
        val weights = if (it.weights.none { w -> w.date == today() })
            it.weights + WeightEntry(today(), p.weightKg) else it.weights
        it.copy(profile = p, weights = weights)
    }

    fun setApiKey(k: String) = mutate { it.copy(apiKey = k.trim()) }

    // ---------- Вес ----------
    fun addWeight(kg: Double) = mutate {
        it.copy(weights = it.weights.filterNot { w -> w.date == today() } + WeightEntry(today(), kg))
    }

    // ---------- Тренировки ----------
    val activeDay = MutableStateFlow<String?>(null)
    val activeSets = MutableStateFlow<List<SetEntry>>(emptyList())

    fun startWorkout(day: String) { activeDay.value = day; activeSets.value = emptyList() }
    fun addSet(s: SetEntry) { activeSets.update { it + s } }
    fun removeSet(index: Int) { activeSets.update { it.filterIndexed { i, _ -> i != index } } }
    fun cancelWorkout() { activeDay.value = null; activeSets.value = emptyList() }

    fun finishWorkout() {
        val day = activeDay.value ?: return
        val sets = activeSets.value
        if (sets.isNotEmpty()) {
            mutate { it.copy(workouts = it.workouts + WorkoutLog(System.currentTimeMillis(), today(), day, sets)) }
            message.value = "Тренировка сохранена 💪 Объём: ${sets.sumOf { it.weight * it.reps }.toInt()} кг"
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
        onResult(DeepSeekClient.estimateFood(data.value.apiKey, text))
    }

    // ---------- Лицо ----------
    fun newFacePhotoFile(): File {
        val dir = File(getApplication<Application>().filesDir, "faces").apply { mkdirs() }
        return File(dir, "face_${System.currentTimeMillis()}.jpg")
    }

    fun importFacePhoto(uri: Uri): File? = try {
        val f = newFacePhotoFile()
        getApplication<Application>().contentResolver.openInputStream(uri)!!.use { inp ->
            f.outputStream().use { inp.copyTo(it) }
        }
        f
    } catch (e: Exception) { message.value = "Не удалось скопировать фото"; null }

    suspend fun analyzeFace(file: File): FaceResult =
        FaceAnalyzer.analyze(getApplication(), Uri.fromFile(file))

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
        val answer = DeepSeekClient.chat(d.apiKey, listOf(ChatMessage("system", systemPrompt()), ChatMessage("user", prompt)))
        saveFaceCheck(check.copy(aiAdvice = answer))
    }

    private fun faceLine(c: FaceCheck): String {
        val m = c.metrics
        return "${c.date}: кожа ${c.skin}/10, высыпания ${c.acne}/10, отёчность ${c.puffiness}/10, сон ${c.sleepHours} ч, вода ${c.waterLiters} л" +
            (m?.let { ", симметрия ${it.symmetry}%, индекс ширины лица ${it.widthIndex}" } ?: "")
    }

    // ---------- AI-тренер ----------
    fun sendChat(text: String) {
        if (text.isBlank()) return
        mutate { it.copy(chat = it.chat + ChatMessage("user", text.trim())) }
        ai {
            val history = data.value.chat.takeLast(20)
            val answer = DeepSeekClient.chat(data.value.apiKey, listOf(ChatMessage("system", systemPrompt())) + history)
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
            |Последние тренировки:
            |${recentWorkouts.ifBlank { "нет" }}
            |Последняя отметка лица: $lastFace""".trimMargin()
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
