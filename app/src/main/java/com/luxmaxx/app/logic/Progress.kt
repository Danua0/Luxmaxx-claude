package com.luxmaxx.app.logic

import com.luxmaxx.app.data.AppData
import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.sqrt

data class Rank(val minLevel: Int, val title: String, val emoji: String)

data class LevelInfo(
    val xp: Int,
    val level: Int,
    val rank: Rank,
    val levelStartXp: Int,
    val nextLevelXp: Int,
) {
    val progress: Float get() = ((xp - levelStartXp).toFloat() / (nextLevelXp - levelStartXp)).coerceIn(0f, 1f)
    val toNext: Int get() = nextLevelXp - xp
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val color: Long,
    val progress: (AppData, Int) -> Float,   // (данные, цель по белку) -> 0..1
)

object Progress {
    val ranks = listOf(
        Rank(1, "Новичок", "🌱"),
        Rank(3, "Любитель", "💪"),
        Rank(5, "Атлет", "🏃"),
        Rank(8, "Машина", "⚙️"),
        Rank(12, "Зверь", "🦍"),
        Rank(17, "Титан", "🗿"),
        Rank(25, "Легенда", "👑"),
    )

    private fun proteinDays(d: AppData, target: Int): Int =
        d.foods.groupBy { it.date }.count { (_, f) -> f.sumOf { it.protein } >= target * 0.95 }

    private fun totalVolume(d: AppData) = d.workouts.sumOf { Programs.volumeKg(it) }

    /** Сколько раз пользователь побил свой рекорд веса в упражнении. */
    fun personalRecords(d: AppData): Int {
        var prs = 0
        val best = HashMap<String, Double>()
        d.workouts.sortedBy { it.id }.forEach { w ->
            w.sets.groupBy { it.exercise }.forEach { (ex, s) ->
                val top = s.maxOf { it.weight }
                val prev = best[ex]
                if (prev != null && top > prev && top > 0) prs++
                if (prev == null || top > prev) best[ex] = top
            }
        }
        return prs
    }

    fun xp(d: AppData, proteinTarget: Int): Int {
        var xp = 0
        xp += d.workouts.sumOf { 100 + it.sets.size * 5 }
        xp += proteinDays(d, proteinTarget) * 50
        xp += d.foods.map { it.date }.distinct().size * 10
        xp += d.faceChecks.size * 30
        xp += d.faceRoutineDone.values.sumOf { it.size } * 5
        xp += d.weights.size * 10
        xp += personalRecords(d) * 40
        if (d.looks.idolAnalysis != null) xp += 50
        if (d.looks.hairAdvice != null) xp += 30
        if (d.looks.wardrobeAdvice != null) xp += 30
        return xp
    }

    fun level(d: AppData, proteinTarget: Int): LevelInfo {
        val xp = xp(d, proteinTarget)
        // уровень растёт по квадратичной кривой: L = floor(sqrt(xp / 120)) + 1
        val level = floor(sqrt(xp / 120.0)).toInt() + 1
        val start = (level - 1) * (level - 1) * 120
        val next = level * level * 120
        val rank = ranks.last { level >= it.minLevel }
        return LevelInfo(xp, level, rank, start, next)
    }

    fun nextRank(level: Int): Rank? = ranks.firstOrNull { it.minLevel > level }

    /** Дни подряд (включая сегодня или вчера) с любой активностью. */
    fun streak(d: AppData): Int {
        val active = (d.workouts.map { it.date } + d.foods.map { it.date } + d.faceChecks.map { it.date } +
            d.weights.map { it.date }).toSet()
        var day = LocalDate.now()
        if (day.toString() !in active) day = day.minusDays(1)
        var n = 0
        while (day.toString() in active) { n++; day = day.minusDays(1) }
        return n
    }

    val achievements: List<Achievement> = listOf(
        Achievement("w1", "Первая кровь", "Первая тренировка", "🩸", 0xFFFF6B6B) { d, _ -> d.workouts.size / 1f },
        Achievement("w10", "Десятка", "10 тренировок", "🔟", 0xFFFFC14D) { d, _ -> d.workouts.size / 10f },
        Achievement("w50", "Полтинник", "50 тренировок", "🏅", 0xFFFF8A3D) { d, _ -> d.workouts.size / 50f },
        Achievement("w100", "Сотка", "100 тренировок", "💯", 0xFFE0553A) { d, _ -> d.workouts.size / 100f },
        Achievement("vol10", "10 тонн", "Суммарный объём 10 000 кг", "🏗️", 0xFF5AB2FF) { d, _ -> (totalVolume(d) / 10_000).toFloat() },
        Achievement("vol100", "Подъёмный кран", "Суммарный объём 100 000 кг", "🏋️", 0xFF3D8BFF) { d, _ -> (totalVolume(d) / 100_000).toFloat() },
        Achievement("pr1", "Рекордсмен", "Побей свой рекорд веса", "📈", 0xFF3DDC97) { d, _ -> personalRecords(d) / 1f },
        Achievement("pr10", "Машина прогресса", "10 личных рекордов", "🚀", 0xFF2FBF7F) { d, _ -> personalRecords(d) / 10f },
        Achievement("p7", "Белковый маньяк", "7 дней с нормой белка", "🥩", 0xFFFF8A3D) { d, t -> proteinDays(d, t) / 7f },
        Achievement("p30", "Мясорубка", "30 дней с нормой белка", "🍗", 0xFFE0553A) { d, t -> proteinDays(d, t) / 30f },
        Achievement("s7", "Неделя огня", "7 дней активности подряд", "🔥", 0xFFFF6B3D) { d, _ -> streak(d) / 7f },
        Achievement("s30", "Железная воля", "30 дней подряд", "⚡", 0xFFFFC14D) { d, _ -> streak(d) / 30f },
        Achievement("f1", "Первый взгляд", "Первая отметка лица", "📸", 0xFFA48BFF) { d, _ -> d.faceChecks.size / 1f },
        Achievement("f14", "Лицо в порядке", "14 отметок лица", "✨", 0xFF8B6BFF) { d, _ -> d.faceChecks.size / 14f },
        Achievement("r8", "Идеальный уход", "Весь чек-лист ухода за день", "🧴", 0xFF3DDC97) { d, _ ->
            (d.faceRoutineDone.values.maxOfOrNull { it.size } ?: 0) / 8f
        },
        Achievement("g2", "+2 кг массы", "Набери 2 кг от старта", "⚖️", 0xFF5AB2FF) { d, _ ->
            val ws = d.weights.sortedBy { it.date }
            if (ws.size < 2) 0f else ((ws.last().kg - ws.first().kg) / 2.0).toFloat()
        },
        Achievement("g5", "+5 кг массы", "Набери 5 кг от старта", "🦾", 0xFF3D8BFF) { d, _ ->
            val ws = d.weights.sortedBy { it.date }
            if (ws.size < 2) 0f else ((ws.last().kg - ws.first().kg) / 5.0).toFloat()
        },
        Achievement("idol", "Есть ориентир", "Разбор кумира от AI", "🏆", 0xFFFFC14D) { d, _ -> if (d.looks.idolAnalysis != null) 1f else 0f },
        Achievement("style", "Стиляга", "Гардероб или разбор образа", "🧥", 0xFFA48BFF) { d, _ ->
            if (d.looks.wardrobeAdvice != null || d.looks.outfitAdvice != null) 1f else 0f
        },
        Achievement("hair", "Барбер одобряет", "Подбор стрижки от AI", "💈", 0xFFFF6B6B) { d, _ -> if (d.looks.hairAdvice != null) 1f else 0f },
        Achievement("ai10", "На связи с тренером", "10 вопросов AI-тренеру", "🤖", 0xFF5AB2FF) { d, _ -> d.chat.count { it.role == "user" } / 10f },
    )

    fun unlockedIds(d: AppData, proteinTarget: Int): Set<String> =
        achievements.filter { it.progress(d, proteinTarget) >= 1f }.map { it.id }.toSet()
}
