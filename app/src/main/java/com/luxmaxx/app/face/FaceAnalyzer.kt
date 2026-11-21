package com.luxmaxx.app.face

import android.content.Context
import android.graphics.PointF
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.luxmaxx.app.data.FaceMetrics
import kotlinx.coroutines.tasks.await
import kotlin.math.abs
import kotlin.math.hypot

sealed class FaceResult {
    data class Ok(val metrics: FaceMetrics, val warning: String?) : FaceResult()
    data class Error(val message: String) : FaceResult()
}

/**
 * Локальный анализ лица через Google ML Kit (работает без интернета, фото никуда не отправляется).
 * Считаем:
 *  - симметрию по парным точкам относительно средней линии лица;
 *  - индекс ширины лица = ширина овала / межзрачковое расстояние.
 *    Он не зависит от расстояния до камеры, и его снижение со временем
 *    обычно означает меньше отёков/жира на лице.
 */
object FaceAnalyzer {
    private val detector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
        )
    }

    suspend fun analyze(context: Context, uri: Uri): FaceResult {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            return FaceResult.Error("Не удалось открыть фото")
        }
        val faces = try { detector.process(image).await() } catch (e: Exception) {
            return FaceResult.Error("Ошибка распознавания: ${e.message}")
        }
        if (faces.isEmpty()) return FaceResult.Error("Лицо не найдено. Сфотографируйся анфас при хорошем свете.")
        val face = faces.maxBy { it.boundingBox.width() * it.boundingBox.height() }

        fun lm(t: Int): PointF? = face.getLandmark(t)?.position
        val le = lm(FaceLandmark.LEFT_EYE); val re = lm(FaceLandmark.RIGHT_EYE)
        val nose = lm(FaceLandmark.NOSE_BASE)
        if (le == null || re == null || nose == null) return FaceResult.Error("Не видно глаз или носа — сними фото анфас")

        // Средняя линия: от середины глаз к основанию носа
        val mx = (le.x + re.x) / 2; val my = (le.y + re.y) / 2
        var dx = nose.x - mx; var dy = nose.y - my
        val dl = hypot(dx, dy).takeIf { it > 0 } ?: return FaceResult.Error("Плохое фото")
        dx /= dl; dy /= dl
        fun side(p: PointF) = abs(dx * (p.y - my) - dy * (p.x - mx))   // расстояние до средней линии

        val pairs = listOf(
            le to re,
            lm(FaceLandmark.LEFT_CHEEK) to lm(FaceLandmark.RIGHT_CHEEK),
            lm(FaceLandmark.MOUTH_LEFT) to lm(FaceLandmark.MOUTH_RIGHT),
            lm(FaceLandmark.LEFT_EAR) to lm(FaceLandmark.RIGHT_EAR),
        ).mapNotNull { (a, b) -> if (a != null && b != null) a to b else null }

        val asym = pairs.map { (a, b) ->
            val da = side(a); val db = side(b)
            if (da + db == 0f) 0.0 else abs(da - db) / ((da + db) / 2.0)
        }
        val symmetry = (100 * (1 - asym.average())).coerceIn(0.0, 100.0)

        val ipd = hypot(le.x - re.x, le.y - re.y).toDouble()
        val oval = face.getContour(FaceContour.FACE)?.points.orEmpty()
        val width = if (oval.isNotEmpty()) {
            // Ширина перпендикулярно средней линии
            // Проекция на направление (-dy, dx), перпендикулярное средней линии
            val proj = oval.map { p -> -dy * (p.x - mx) + dx * (p.y - my) }
            (proj.max() - proj.min()).toDouble()
        } else face.boundingBox.width().toDouble()
        val widthIndex = if (ipd > 0) width / ipd else 0.0

        val yaw = face.headEulerAngleY.toDouble()
        val warning = when {
            abs(yaw) > 12 -> "Голова повёрнута на ${abs(yaw).toInt()}° — для точного сравнения снимай строго анфас."
            abs(face.headEulerAngleX) > 12 -> "Голова наклонена — держи камеру на уровне глаз."
            else -> null
        }
        return FaceResult.Ok(
            FaceMetrics(
                symmetry = Math.round(symmetry * 10) / 10.0,
                widthIndex = Math.round(widthIndex * 1000) / 1000.0,
                headYaw = Math.round(yaw * 10) / 10.0,
                smileProb = face.smilingProbability?.toDouble(),
            ),
            warning,
        )
    }
}
