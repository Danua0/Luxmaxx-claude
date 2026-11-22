package com.luxmaxx.app.data

import kotlinx.serialization.Serializable

@Serializable
enum class Sex { MALE, FEMALE }

@Serializable
enum class Activity(val factor: Double, val title: String) {
    LOW(1.375, "Сидячая работа"),
    MEDIUM(1.55, "Средняя"),
    HIGH(1.725, "Высокая (физ. работа)"),
}

@Serializable
enum class Experience(val title: String) {
    BEGINNER("Новичок (< 1 года)"),
    INTERMEDIATE("Средний (1–3 года)"),
    ADVANCED("Опытный (3+ лет)"),
}

@Serializable
data class Profile(
    val name: String = "",
    val sex: Sex = Sex.MALE,
    val age: Int = 20,
    val heightCm: Int = 178,
    val weightKg: Double = 70.0,
    val activity: Activity = Activity.LOW,
    val trainingDays: Int = 3,
    val experience: Experience = Experience.BEGINNER,
)

@Serializable
data class WeightEntry(val date: String, val kg: Double)

@Serializable
data class SetEntry(val exercise: String, val weight: Double, val reps: Int)

@Serializable
data class WorkoutLog(
    val id: Long,
    val date: String,
    val dayName: String,
    val sets: List<SetEntry>,
)

@Serializable
data class FoodEntry(
    val id: Long,
    val date: String,
    val name: String,
    val kcal: Int,
    val protein: Int,
    val carbs: Int = 0,
    val fat: Int = 0,
)

@Serializable
data class FaceMetrics(
    val symmetry: Double,          // 0..100
    val widthIndex: Double,        // ширина лица / межзрачковое расстояние
    val headYaw: Double,           // поворот головы, градусы
    val smileProb: Double? = null,
)

@Serializable
data class FaceCheck(
    val id: Long,
    val date: String,
    val photoPath: String? = null,
    val skin: Int = 5,        // 1..10 общее состояние кожи
    val acne: Int = 0,        // 0..10 количество высыпаний
    val puffiness: Int = 0,   // 0..10 отёчность
    val sleepHours: Double = 8.0,
    val waterLiters: Double = 2.0,
    val notes: String = "",
    val metrics: FaceMetrics? = null,
    val aiAdvice: String? = null,
)

@Serializable
data class ChatMessage(val role: String, val content: String)

@Serializable
data class Looks(
    // Кумир
    val idolName: String = "",
    val idolPhotoPath: String? = null,
    val idolAnalysis: String? = null,
    // Волосы
    val hairType: String = "Прямые",
    val hairLength: String = "Короткие",
    val hairIssues: List<String> = emptyList(),
    val hairPhotoPath: String? = null,
    val hairAdvice: String? = null,
    // Одежда
    val styleGoal: String = "",
    val budget: String = "Средний",
    val outfitPhotoPath: String? = null,
    val outfitAdvice: String? = null,
    val wardrobeAdvice: String? = null,
)

@Serializable
data class AppData(
    val profile: Profile? = null,
    val weights: List<WeightEntry> = emptyList(),
    val workouts: List<WorkoutLog> = emptyList(),
    val foods: List<FoodEntry> = emptyList(),
    val faceChecks: List<FaceCheck> = emptyList(),
    val chat: List<ChatMessage> = emptyList(),
    val apiKey: String = "",
    val textModel: String = "deepseek-chat",
    val visionModel: String = "deepseek-flash",
    val faceRoutineDone: Map<String, List<String>> = emptyMap(), // дата -> выполненные пункты ухода
    val looks: Looks = Looks(),
)
