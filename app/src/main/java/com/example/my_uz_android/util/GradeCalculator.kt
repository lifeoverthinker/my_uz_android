package com.example.my_uz_android.util

import com.example.my_uz_android.data.models.GradeEntity

// Pomocnik do liczenia średniej ocen
object GradeCalculator {

    // Liczy średnią ważoną z ocen lub zwraca ocenę końcową, jeśli została już wpisana
    fun calculateGPA(grades: List<GradeEntity>): Double {
        // 1. Jeśli jest już wpisana ocena końcowa, to ona jest najważniejsza
        val finalGrade = grades.find {
            it.description?.contains("końcow", ignoreCase = true) == true ||
                    it.comment?.contains("końcow", ignoreCase = true) == true
        }

        if (finalGrade != null && finalGrade.grade > 0.0) {
            return finalGrade.grade
        }

        // 2. Bierzemy tylko zwykłe oceny (odrzucamy punkty, wpisy z wagą 0 i nieustawione oceny -1.0)
        val validGrades = grades.filter {
            !it.isPoints && it.grade != -1.0 && it.weight > 0
        }

        if (validGrades.isEmpty()) {
            return 0.0
        }

        // 3. Klasyczna średnia ważona: (ocena * waga) / suma wag
        val sum = validGrades.sumOf { it.grade * it.weight }
        val weightSum = validGrades.sumOf { it.weight }

        return if (weightSum > 0) sum / weightSum else 0.0
    }
}