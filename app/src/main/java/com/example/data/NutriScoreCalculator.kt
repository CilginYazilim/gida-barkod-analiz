package com.example.data

import kotlin.math.roundToInt

/**
 * Resmi Nutri-Score (genel gıda) puanlama tablolarını yapısal besin değerlerinden uygular.
 *
 * Skorlar deterministik ve tekrarlanabilirdir (AI tahmini DEĞİL). İlgili besin değeri yoksa
 * o skor `null` ("Belirsiz") döner; UI gri gösterir.
 *
 * Sub-skorlar 0–100 ölçeğinde, YÜKSEK = DAHA SAĞLIKLI (uygulamanın mevcut kuralı).
 */
object NutriScoreCalculator {

    data class Input(
        val energyKcal: Double? = null,
        val sugar: Double? = null,       // g/100g
        val satFat: Double? = null,      // g/100g
        val salt: Double? = null,        // g/100g
        val protein: Double? = null,     // g/100g
        val fiber: Double? = null        // g/100g
    )

    data class Result(
        val grade: Char?,          // A–E veya null
        val healthScore: Int?,     // 0–100 veya null
        val sugarScore: Int?,      // 0–100 (yüksek=az şeker) veya null
        val nutritionScore: Int?   // 0–100 (yüksek=iyi besin) veya null
    )

    fun compute(i: Input): Result {
        val energyPts = i.energyKcal?.let { energyPoints(it * 4.184) }      // kcal → kJ
        val sugarPts = i.sugar?.let { sugarPoints(it) }
        val satFatPts = i.satFat?.let { satFatPoints(it) }
        val sodiumPts = i.salt?.let { sodiumPoints(it * 400.0) }            // tuz g → sodyum mg
        val fiberPts = i.fiber?.let { fiberPoints(it) }
        val proteinPts = i.protein?.let { proteinPoints(it) }

        // Genel gıda Nutri-Score harfi: enerji+şeker+doymuşyağ+tuz hepsi gerekli
        val grade: Char? = if (energyPts != null && sugarPts != null && satFatPts != null && sodiumPts != null) {
            val negative = energyPts + sugarPts + satFatPts + sodiumPts
            val positive = (fiberPts ?: 0) + (proteinPts ?: 0)
            // Kural: negatif ≥ 11 ve meyve/sebze puanı tam değilse protein puanı sayılmaz
            val effectivePositive = if (negative >= 11) (fiberPts ?: 0) else positive
            gradeFromScore(negative - effectivePositive)
        } else null

        val health = grade?.let { healthFromGrade(it) }
        val sugarScore = sugarPts?.let { 100 - it * 10 }                   // 0 puan=100, 10 puan=0
        val nutritionScore = if (fiberPts != null || proteinPts != null) {
            val good = ((fiberPts ?: 0) + (proteinPts ?: 0)) / 10.0 * 100.0
            val penalty = (satFatPts ?: 0) * 5.0
            (good - penalty).coerceIn(0.0, 100.0).roundToInt()
        } else null

        return Result(grade, health, sugarScore, nutritionScore)
    }

    private fun healthFromGrade(g: Char): Int = when (g) {
        'A' -> 90; 'B' -> 72; 'C' -> 54; 'D' -> 36; else -> 18
    }

    private fun gradeFromScore(s: Int): Char = when {
        s <= -1 -> 'A'
        s <= 2 -> 'B'
        s <= 10 -> 'C'
        s <= 18 -> 'D'
        else -> 'E'
    }

    private fun energyPoints(kj: Double): Int = thresholds(kj, listOf(335.0, 670.0, 1005.0, 1340.0, 1675.0, 2010.0, 2345.0, 2680.0, 3015.0, 3350.0))
    private fun sugarPoints(g: Double): Int = thresholds(g, listOf(4.5, 9.0, 13.5, 18.0, 22.5, 27.0, 31.0, 36.0, 40.0, 45.0))
    private fun satFatPoints(g: Double): Int = thresholds(g, listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0))
    private fun sodiumPoints(mg: Double): Int = thresholds(mg, listOf(90.0, 180.0, 270.0, 360.0, 450.0, 540.0, 630.0, 720.0, 810.0, 900.0))
    private fun fiberPoints(g: Double): Int = thresholds(g, listOf(0.9, 1.9, 2.8, 3.7, 4.7))
    private fun proteinPoints(g: Double): Int = thresholds(g, listOf(1.6, 3.2, 4.8, 6.4, 8.0))

    /** value <= bounds[i] ise i puan; hepsini aşarsa bounds.size puan. */
    private fun thresholds(value: Double, bounds: List<Double>): Int {
        for ((idx, b) in bounds.withIndex()) if (value <= b) return idx
        return bounds.size
    }
}
