package com.luxmaxx.app.logic

import com.luxmaxx.app.data.Profile
import com.luxmaxx.app.data.Sex
import com.luxmaxx.app.data.WeightEntry
import kotlin.math.roundToInt

data class Targets(
    val bmr: Int,
    val tdee: Int,
    val kcal: Int,
    val protein: Int,
    val fat: Int,
    val carbs: Int,
    val waterL: Double,
)

object Calculator {
    /** Формула Миффлина–Сан Жеора + профицит ~10% для «чистого» набора массы. */
    fun targets(p: Profile, currentWeight: Double = p.weightKg): Targets {
        val base = 10 * currentWeight + 6.25 * p.heightCm - 5 * p.age
        val bmr = if (p.sex == Sex.MALE) base + 5 else base - 161
        // Тренировки добавляют к активности
        val trainingBonus = 0.025 * p.trainingDays
        val tdee = bmr * (p.activity.factor + trainingBonus - 0.1)
        val kcal = tdee * 1.10
        val protein = currentWeight * 2.0          // 1.6–2.2 г/кг — оптимум для гипертрофии
        val fat = currentWeight * 0.9              // ~0.8–1 г/кг
        val carbs = (kcal - protein * 4 - fat * 9) / 4
        return Targets(
            bmr = bmr.roundToInt(),
            tdee = tdee.roundToInt(),
            kcal = kcal.roundToInt(),
            protein = protein.roundToInt(),
            fat = fat.roundToInt(),
            carbs = carbs.roundToInt().coerceAtLeast(0),
            waterL = (currentWeight * 0.035 * 10).roundToInt() / 10.0,
        )
    }

    fun bmi(p: Profile, w: Double): Double = w / ((p.heightCm / 100.0) * (p.heightCm / 100.0))

    /**
     * Темп набора в кг/неделю по линейной регрессии последних 4 недель.
     * Оптимум для набора мышц: +0.25–0.5% массы тела в неделю.
     */
    fun weeklyRate(weights: List<WeightEntry>): Double? {
        val pts = weights.sortedBy { it.date }.takeLast(28)
        if (pts.size < 3) return null
        val d0 = java.time.LocalDate.parse(pts.first().date)
        val xs = pts.map { java.time.temporal.ChronoUnit.DAYS.between(d0, java.time.LocalDate.parse(it.date)).toDouble() }
        val ys = pts.map { it.kg }
        val mx = xs.average(); val my = ys.average()
        val den = xs.sumOf { (it - mx) * (it - mx) }
        if (den == 0.0) return null
        val slope = xs.indices.sumOf { (xs[it] - mx) * (ys[it] - my) } / den
        return slope * 7
    }

    fun rateAdvice(rate: Double?, weight: Double): String {
        if (rate == null) return "Взвешивайся утром натощак хотя бы 3 раза, чтобы увидеть темп."
        val pct = rate / weight * 100
        return when {
            pct < 0.1 -> "Вес почти стоит. Добавь ~200 ккал в день (например, +50 г риса и ложку арахисовой пасты)."
            pct <= 0.6 -> "Отличный темп — это в основном мышцы. Так держать!"
            else -> "Набираешь слишком быстро — будет много жира. Убери ~200 ккал в день."
        }
    }
}
