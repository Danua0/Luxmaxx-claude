package com.luxmaxx.app.logic

import com.luxmaxx.app.data.Equip

/** Домашнее упражнение. equip = null — только вес тела (стул, диван, стена). */
data class HomeAlt(val name: String, val equip: Equip?, val repMin: Int = 10, val repMax: Int = 20, val tip: String = "")

/**
 * Замены зальных упражнений на домашние.
 * Дома нагрузка меньше, поэтому компенсируем: больше подходов, больше повторов,
 * медленный негатив, паузы и дополнительные упражнения-добивки.
 */
object HomeWorkouts {
    private fun bw(n: String, a: Int = 10, b: Int = 20, t: String = "") = HomeAlt(n, null, a, b, t)
    private fun db(n: String, a: Int = 8, b: Int = 15, t: String = "") = HomeAlt(n, Equip.DUMBBELLS, a, b, t)
    private fun bar(n: String, a: Int = 5, b: Int = 12, t: String = "") = HomeAlt(n, Equip.BAR, a, b, t)
    private fun band(n: String, a: Int = 12, b: Int = 25, t: String = "") = HomeAlt(n, Equip.BANDS, a, b, t)

    private val squat = listOf(
        db("Гоблет-приседания с гантелью", 10, 15, "Гантель у груди, садись глубоко, локти между коленями"),
        db("Болгарские выпады с гантелями", 8, 12),
        bw("Приседания с паузой 3 сек внизу", 15, 25, "Пауза убирает инерцию — ноги работают сильнее"),
        bw("Болгарские выпады (нога на диване)", 10, 15),
        bw("Пистолетик с опорой", 5, 10),
        bw("Приседания-прыжки", 10, 15),
    )
    private val hinge = listOf(
        db("Румынская тяга с гантелями", 10, 15, "Таз назад, спина прямая, гантели скользят по бёдрам"),
        bw("Румынская тяга на одной ноге", 10, 15, "Держи рюкзак с книгами для веса"),
        band("Good morning с резинкой", 15, 25),
        bw("Ягодичный мост на одной ноге", 12, 20, "Пауза 2 сек наверху"),
    )
    private val legCurl = listOf(
        bw("Скандинавские сгибания (ноги под диваном)", 4, 8, "Опускайся как можно медленнее"),
        bw("Сгибания ног лёжа на полотенце", 10, 20, "Ноги на полотенце, таз в мосте, подтягивай пятки"),
    )
    private val legExt = listOf(
        bw("Сисси-приседания у стены", 10, 15),
        bw("Зашагивания на стул", 10, 15),
    )
    private val calves = listOf(
        db("Подъёмы на носки с гантелью на одной ноге", 12, 20),
        bw("Подъёмы на носки со ступеньки на одной ноге", 15, 25, "Полная амплитуда, пауза внизу"),
    )
    private val chest = listOf(
        db("Жим гантелей лёжа на полу", 8, 15),
        bw("Отжимания с паузой внизу", 10, 20, "Грудь в 2 см от пола, пауза 1 сек"),
        bw("Отжимания ноги на диване", 8, 15),
        bw("Отжимания между стульями (глубокие)", 8, 15),
        bw("Отжимания", 12, 30),
    )
    private val dips = listOf(
        bw("Отжимания между стульями (брусья)", 8, 15),
        bw("Обратные отжимания от стула", 12, 20),
    )
    private val press = listOf(
        db("Жим гантелей стоя", 8, 12),
        bw("Пайк-отжимания", 8, 15, "Таз вверх, голова между руками — нагружает плечи"),
        bw("Отжимания в стойке у стены", 3, 10),
    )
    private val lateral = listOf(
        db("Махи гантелями в стороны", 12, 20),
        band("Махи с резинкой", 15, 25),
        bw("Махи с бутылками воды", 20, 30),
    )
    private val rearDelt = listOf(
        band("Face pull с резинкой", 15, 25),
        db("Разведения гантелей в наклоне", 12, 20),
        bw("Y-T-W лёжа на полу", 10, 15),
    )
    private val vertPull = listOf(
        bar("Подтягивания", 5, 12),
        band("Тяга резинки сверху", 12, 20),
        bw("Австралийские подтягивания под столом", 8, 15, "Ляг под крепкий стол, тянись грудью к краю"),
    )
    private val row = listOf(
        db("Тяга гантели в наклоне", 10, 15),
        bw("Австралийские подтягивания под столом", 8, 15, "Ляг под крепкий стол, тянись грудью к краю"),
        band("Тяга резинки к поясу", 15, 25),
        bw("Тяга полотенца в дверном проёме", 12, 20),
    )
    private val biceps = listOf(
        db("Подъём гантелей на бицепс", 10, 15),
        db("Молотки с гантелями", 10, 15),
        band("Подъём на бицепс с резинкой", 15, 25),
        bar("Подтягивания обратным хватом", 5, 12),
        bw("Сгибания рук с рюкзаком", 12, 20),
    )
    private val triceps = listOf(
        db("Французский жим гантели", 10, 15),
        bw("Алмазные отжимания", 8, 20),
        bw("Обратные отжимания от стула", 12, 20),
    )
    private val abs = listOf(
        bar("Подъёмы ног в висе", 8, 15),
        bw("Подъёмы ног лёжа", 12, 20),
        bw("Скручивания", 15, 25),
        bw("Велосипед", 20, 30),
        bw("Планка (сек)", 30, 60),
    )

    /** Порядок важен: побеждает первое совпадение. */
    private val rules: List<Pair<String, List<HomeAlt>>> = listOf(
        "подъёмы ног" to abs, "подъёмы на носки" to calves, "разгибания ног" to legExt, "сгибания ног" to legCurl,
        "жим ногами" to squat, "брусья" to dips, "жим лёжа" to chest, "наклонной" to chest,
        "жим штанги стоя" to press, "жим гантелей сидя" to press, "махи" to lateral, "face pull" to rearDelt,
        "подтягивания" to vertPull, "верхнего блока" to vertPull, "румынская" to hinge, "становая" to hinge,
        "тяга" to row, "присед" to squat, "гакк" to squat, "выпады" to squat,
        "бицепс" to biceps, "молотки" to biceps, "трицепс" to triceps, "французский" to triceps,
        "скручивания" to abs, "планка" to abs,
    )

    fun allHome(): List<HomeAlt> =
        listOf(squat, hinge, legCurl, legExt, calves, chest, dips, press, lateral, rearDelt, vertPull, row, biceps, triceps, abs)
            .flatten().distinctBy { it.name }

    private fun available(list: List<HomeAlt>, eq: Set<Equip>) = list.filter { it.equip == null || it.equip in eq }

    fun alternatives(exercise: String, eq: Set<Equip>): List<HomeAlt> {
        val e = exercise.lowercase()
        val list = rules.firstOrNull { e.contains(it.first) }?.second ?: return emptyList()
        return available(list, eq)
    }

    /** Добивки в конце домашней тренировки — компенсируют отсутствие тяжёлых весов. */
    private fun finishers(dayName: String): List<PlannedExercise> {
        val n = dayName.lowercase()
        return when {
            "низ" in n || "legs" in n -> listOf(
                PlannedExercise("Ягодичный мост", 3, 15, 25, "Пауза 2 сек наверху"),
                PlannedExercise("Приседания-прыжки", 3, 10, 15, "Добивка: взрывно вверх, мягко вниз"),
            )
            "верх" in n || "push" in n -> listOf(
                PlannedExercise("Отжимания до отказа", 2, 10, 40, "Добивка: последний подход до отказа"),
                PlannedExercise("Обратные отжимания от стула", 3, 12, 20),
            )
            "pull" in n -> listOf(
                PlannedExercise("Тяга полотенца в дверном проёме", 3, 12, 20, "Сжимай лопатки в конце"),
                PlannedExercise("Y-T-W лёжа на полу", 2, 10, 15),
            )
            else -> listOf(
                PlannedExercise("Бёрпи", 3, 8, 15, "Добивка на выносливость и жиросжигание"),
                PlannedExercise("Планка (сек)", 3, 30, 60),
            )
        }
    }

    /** Превращает зальную программу в домашнюю: замены + больше подходов и упражнений. */
    fun toHome(program: Program, eq: Set<Equip>): Program {
        val days = program.days.map { day ->
            val used = mutableSetOf<String>()
            val converted = day.exercises.map { ex ->
                val alt = alternatives(ex.name, eq).firstOrNull { it.name !in used }
                if (alt == null) ex.copy(sets = ex.sets + 1)
                else {
                    used += alt.name
                    PlannedExercise(alt.name, ex.sets + 1, alt.repMin, alt.repMax,
                        alt.tip.ifBlank { "Дома: опускай за 3 сек, пауза внизу — так меньший вес даёт тот же стимул" })
                }
            }
            val extra = finishers(day.name).filter { f -> converted.none { it.name == f.name } }
            WorkoutDay("${day.name} · Дом", converted + extra)
        }
        val eqText = if (eq.isEmpty()) "только вес тела" else eq.joinToString { it.title.lowercase() }
        return Program(
            "${program.title} · Дома",
            "Домашняя версия ($eqText): +1 подход в каждом упражнении, больше повторов и добивки в конце. " +
                "Работай медленно и близко к отказу — мышцам важна нагрузка, а не место.",
            days,
        )
    }

    /** Все варианты замены упражнения: дома и в зале, по той же основной мышце. */
    fun swapOptions(exercise: String, eq: Set<Equip>, gymNames: List<String>): Pair<List<HomeAlt>, List<String>> {
        val target = Muscles.primary(exercise)
        val home = (alternatives(exercise, eq) + allHome().filter { (it.equip == null || it.equip in eq) && Muscles.primary(it.name) == target })
            .distinctBy { it.name }.filter { it.name != exercise }
        val gym = gymNames.distinct().filter { it != exercise && target != null && Muscles.primary(it) == target }
        return home to gym
    }
}
