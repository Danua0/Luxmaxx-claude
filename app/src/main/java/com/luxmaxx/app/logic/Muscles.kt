package com.luxmaxx.app.logic

import com.luxmaxx.app.data.WorkoutLog
import java.time.LocalDate

enum class Muscle(val title: String, val weeklyTarget: Float) {
    CHEST("Грудь", 12f),
    SHOULDERS("Плечи", 12f),
    BICEPS("Бицепс", 10f),
    TRICEPS("Трицепс", 10f),
    FOREARMS("Предплечья", 6f),
    ABS("Пресс", 8f),
    BACK("Спина", 14f),
    TRAPS("Трапеции", 6f),
    LOWER_BACK("Поясница", 5f),
    GLUTES("Ягодицы", 8f),
    QUADS("Квадрицепс", 12f),
    HAMS("Бицепс бедра", 10f),
    CALVES("Икры", 8f),
}

object Muscles {
    private val M = Muscle.entries.associateBy { it.name }

    /** Порядок важен: побеждает первое совпадение. */
    private val rules: List<Pair<String, Map<Muscle, Float>>> = listOf(
        "подъёмы ног" to mapOf(Muscle.ABS to 1f, Muscle.FOREARMS to 0.3f),
        "подъёмы на носки" to mapOf(Muscle.CALVES to 1f),
        "разгибания ног" to mapOf(Muscle.QUADS to 1f),
        "сгибания ног" to mapOf(Muscle.HAMS to 1f),
        "жим ногами" to mapOf(Muscle.QUADS to 1f, Muscle.GLUTES to 0.5f),
        "брусья" to mapOf(Muscle.CHEST to 0.7f, Muscle.TRICEPS to 0.8f, Muscle.SHOULDERS to 0.3f),
        "жим лёжа" to mapOf(Muscle.CHEST to 1f, Muscle.TRICEPS to 0.5f, Muscle.SHOULDERS to 0.4f),
        "наклонной" to mapOf(Muscle.CHEST to 1f, Muscle.SHOULDERS to 0.5f, Muscle.TRICEPS to 0.4f),
        "жим штанги стоя" to mapOf(Muscle.SHOULDERS to 1f, Muscle.TRICEPS to 0.5f, Muscle.TRAPS to 0.2f),
        "жим гантелей сидя" to mapOf(Muscle.SHOULDERS to 1f, Muscle.TRICEPS to 0.5f),
        "махи" to mapOf(Muscle.SHOULDERS to 1f, Muscle.TRAPS to 0.2f),
        "face pull" to mapOf(Muscle.SHOULDERS to 0.7f, Muscle.TRAPS to 0.6f, Muscle.BACK to 0.3f),
        "подтягивания" to mapOf(Muscle.BACK to 1f, Muscle.BICEPS to 0.6f, Muscle.FOREARMS to 0.3f),
        "верхнего блока" to mapOf(Muscle.BACK to 1f, Muscle.BICEPS to 0.5f),
        "румынская" to mapOf(Muscle.HAMS to 1f, Muscle.GLUTES to 0.8f, Muscle.LOWER_BACK to 0.6f),
        "становая" to mapOf(Muscle.HAMS to 0.8f, Muscle.GLUTES to 0.8f, Muscle.LOWER_BACK to 1f, Muscle.BACK to 0.5f, Muscle.TRAPS to 0.5f),
        "тяга" to mapOf(Muscle.BACK to 1f, Muscle.BICEPS to 0.5f, Muscle.TRAPS to 0.4f, Muscle.LOWER_BACK to 0.2f),
        "присед" to mapOf(Muscle.QUADS to 1f, Muscle.GLUTES to 0.7f, Muscle.LOWER_BACK to 0.3f),
        "гакк" to mapOf(Muscle.QUADS to 1f, Muscle.GLUTES to 0.5f),
        "выпады" to mapOf(Muscle.QUADS to 1f, Muscle.GLUTES to 0.8f, Muscle.HAMS to 0.3f),
        "бицепс" to mapOf(Muscle.BICEPS to 1f, Muscle.FOREARMS to 0.4f),
        "молотки" to mapOf(Muscle.BICEPS to 0.8f, Muscle.FOREARMS to 0.8f),
        "трицепс" to mapOf(Muscle.TRICEPS to 1f),
        "французский" to mapOf(Muscle.TRICEPS to 1f),
        "отжимания" to mapOf(Muscle.CHEST to 1f, Muscle.TRICEPS to 0.5f),
        "скручивания" to mapOf(Muscle.ABS to 1f),
        "планка" to mapOf(Muscle.ABS to 1f, Muscle.LOWER_BACK to 0.3f),
    )

    fun of(exercise: String): Map<Muscle, Float> {
        val e = exercise.lowercase()
        return rules.firstOrNull { e.contains(it.first) }?.second ?: emptyMap()
    }

    fun primary(exercise: String): Muscle? = of(exercise).maxByOrNull { it.value }?.key

    /** Взвешенное число подходов на каждую мышцу за последние 7 дней. */
    fun weeklySets(logs: List<WorkoutLog>): Map<Muscle, Float> {
        val since = LocalDate.now().minusDays(6).toString()
        val res = HashMap<Muscle, Float>()
        logs.filter { it.date >= since }.forEach { w ->
            w.sets.forEach { s -> of(s.exercise).forEach { (m, k) -> res[m] = (res[m] ?: 0f) + k } }
        }
        return res
    }

    /** Интенсивность 0..1.3 относительно недельной нормы — для раскраски карты. */
    fun heat(sets: Map<Muscle, Float>): Map<Muscle, Float> =
        Muscle.entries.associateWith { ((sets[it] ?: 0f) / it.weeklyTarget).coerceIn(0f, 1.3f) }

    /** «Тепло» мышц для запланированного дня (по количеству подходов). */
    fun planHeat(exercises: List<PlannedExercise>): Map<Muscle, Float> {
        val res = HashMap<Muscle, Float>()
        exercises.forEach { ex -> of(ex.name).forEach { (m, k) -> res[m] = (res[m] ?: 0f) + k * ex.sets } }
        val max = res.values.maxOrNull() ?: 1f
        return Muscle.entries.associateWith { ((res[it] ?: 0f) / max) }
    }

    fun lagging(sets: Map<Muscle, Float>): List<Muscle> {
        val main = listOf(Muscle.CHEST, Muscle.BACK, Muscle.SHOULDERS, Muscle.QUADS, Muscle.HAMS, Muscle.BICEPS, Muscle.TRICEPS)
        return main.filter { (sets[it] ?: 0f) < it.weeklyTarget * 0.5f }
    }

    @Suppress("unused") private val unused = M
}
