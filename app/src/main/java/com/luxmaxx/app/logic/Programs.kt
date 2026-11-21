package com.luxmaxx.app.logic

import com.luxmaxx.app.data.Experience
import com.luxmaxx.app.data.SetEntry
import com.luxmaxx.app.data.WorkoutLog

data class PlannedExercise(
    val name: String,
    val sets: Int,
    val repMin: Int,
    val repMax: Int,
    val tip: String = "",
)

data class WorkoutDay(val name: String, val exercises: List<PlannedExercise>)

data class Program(val title: String, val description: String, val days: List<WorkoutDay>)

object Programs {
    private fun e(n: String, s: Int, a: Int, b: Int, t: String = "") = PlannedExercise(n, s, a, b, t)

    fun forProfile(days: Int, exp: Experience): Program {
        val extra = if (exp == Experience.BEGINNER) 0 else 1
        return when {
            days <= 3 -> Program(
                "Full Body A/B",
                "Всё тело 2–3 раза в неделю, чередуй A и B. Лучший вариант для новичка: каждая мышца получает стимул часто.",
                listOf(
                    WorkoutDay("Full Body A", listOf(
                        e("Приседания со штангой", 3 + extra, 5, 8, "Глубина — бедро параллельно полу или ниже"),
                        e("Жим лёжа", 3 + extra, 6, 10, "Лопатки сведены, штанга касается низа груди"),
                        e("Тяга штанги в наклоне", 3, 8, 12),
                        e("Жим гантелей сидя", 3, 8, 12),
                        e("Подъём на бицепс", 2, 10, 15),
                        e("Скручивания", 3, 10, 20),
                    )),
                    WorkoutDay("Full Body B", listOf(
                        e("Румынская тяга", 3 + extra, 6, 10, "Спина прямая, таз назад, чувствуй растяжение бицепса бедра"),
                        e("Жим штанги стоя", 3, 6, 10),
                        e("Подтягивания (или тяга верхнего блока)", 3 + extra, 6, 12),
                        e("Жим ногами", 3, 10, 15),
                        e("Отжимания на брусьях", 3, 8, 12),
                        e("Разгибания на трицепс", 2, 10, 15),
                    )),
                ),
            )
            days == 4 -> Program(
                "Верх / Низ",
                "4 дня: Верх, Низ, отдых, Верх, Низ. Каждая мышца 2 раза в неделю.",
                listOf(
                    WorkoutDay("Верх 1", listOf(
                        e("Жим лёжа", 4, 5, 8), e("Тяга штанги в наклоне", 4, 6, 10),
                        e("Жим гантелей сидя", 3, 8, 12), e("Тяга верхнего блока", 3, 8, 12),
                        e("Подъём на бицепс", 3, 8, 12), e("Французский жим", 3, 8, 12),
                    )),
                    WorkoutDay("Низ 1", listOf(
                        e("Приседания со штангой", 4, 5, 8), e("Румынская тяга", 3, 8, 10),
                        e("Выпады", 3, 10, 12), e("Сгибания ног", 3, 10, 15),
                        e("Подъёмы на носки", 4, 10, 15), e("Скручивания", 3, 12, 20),
                    )),
                    WorkoutDay("Верх 2", listOf(
                        e("Жим штанги стоя", 4, 5, 8), e("Подтягивания", 4, 6, 10),
                        e("Жим гантелей на наклонной", 3, 8, 12), e("Тяга гантели одной рукой", 3, 8, 12),
                        e("Махи гантелями в стороны", 3, 12, 20), e("Молотки", 3, 10, 12),
                    )),
                    WorkoutDay("Низ 2", listOf(
                        e("Становая тяга", 3, 3, 6, "Техника важнее веса"), e("Фронтальные приседания / Гакк", 3, 8, 10),
                        e("Болгарские выпады", 3, 8, 12), e("Разгибания ног", 3, 12, 15),
                        e("Подъёмы на носки сидя", 4, 12, 20), e("Планка (сек)", 3, 30, 60),
                    )),
                ),
            )
            else -> Program(
                "Push / Pull / Legs",
                "5–6 дней: Жим, Тяга, Ноги по кругу. Для тех, кто уже тренируется и хорошо восстанавливается.",
                listOf(
                    WorkoutDay("Push (жим)", listOf(
                        e("Жим лёжа", 4, 5, 8), e("Жим штанги стоя", 3, 6, 10),
                        e("Жим гантелей на наклонной", 3, 8, 12), e("Махи гантелями в стороны", 4, 12, 20),
                        e("Разгибания на трицепс", 3, 10, 15), e("Отжимания на брусьях", 2, 8, 12),
                    )),
                    WorkoutDay("Pull (тяга)", listOf(
                        e("Подтягивания", 4, 6, 10), e("Тяга штанги в наклоне", 4, 6, 10),
                        e("Тяга горизонтального блока", 3, 10, 12), e("Face pull", 3, 12, 20, "Здоровье плеч и осанка"),
                        e("Подъём на бицепс", 3, 8, 12), e("Молотки", 2, 10, 12),
                    )),
                    WorkoutDay("Legs (ноги)", listOf(
                        e("Приседания со штангой", 4, 5, 8), e("Румынская тяга", 3, 8, 10),
                        e("Жим ногами", 3, 10, 15), e("Сгибания ног", 3, 10, 15),
                        e("Подъёмы на носки", 4, 10, 15), e("Подъёмы ног в висе", 3, 10, 15),
                    )),
                ),
            )
        }
    }

    /** Подходы этого упражнения из последней тренировки, где оно было. */
    fun lastSets(logs: List<WorkoutLog>, exercise: String): List<SetEntry> =
        logs.sortedByDescending { it.id }
            .firstOrNull { log -> log.sets.any { it.exercise == exercise } }
            ?.sets?.filter { it.exercise == exercise } ?: emptyList()

    /** Двойная прогрессия: добил верх диапазона во всех подходах — добавляй вес. */
    fun progressionHint(ex: PlannedExercise, last: List<SetEntry>): String {
        if (last.isEmpty()) return "Первый раз: подбери вес, с которым делаешь ${ex.repMin}–${ex.repMax} повторений с запасом 1–2."
        val w = last.maxOf { it.weight }
        val allTop = last.size >= ex.sets && last.all { it.reps >= ex.repMax }
        val fmt = { d: Double -> if (d % 1.0 == 0.0) d.toInt().toString() else d.toString() }
        return if (allTop) "🔥 Ты добил ${ex.repMax} во всех подходах — сегодня ставь ${fmt(w + 2.5)} кг"
        else "Прошлый раз: ${last.joinToString { "${fmt(it.weight)}×${it.reps}" }}. Цель — добавить хотя бы 1 повтор."
    }

    /** Следующий день программы после последней записанной тренировки. */
    fun nextDay(program: Program, logs: List<WorkoutLog>): WorkoutDay {
        val last = logs.maxByOrNull { it.id } ?: return program.days.first()
        val idx = program.days.indexOfFirst { it.name == last.dayName }
        return program.days[(idx + 1).mod(program.days.size)]
    }

    fun volumeKg(log: WorkoutLog) = log.sets.sumOf { it.weight * it.reps }
}
