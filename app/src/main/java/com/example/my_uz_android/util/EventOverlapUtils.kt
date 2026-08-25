package com.example.my_uz_android.util

import com.example.my_uz_android.data.models.ClassEntity
import kotlin.math.max

// Przechowuje pozycję kafelka: w której kolumnie ma być i na ile części dzielimy szerokość
data class EventLayoutInfo(
    val classEntity: ClassEntity,
    val colIndex: Int,
    val totalCols: Int
)

// Oblicza układ kafelków na siatce, żeby nakładające się zajęcia nie zasłaniały się nawzajem
fun calculateEventLayouts(classes: List<ClassEntity>): List<EventLayoutInfo> {
    if (classes.isEmpty()) return emptyList()

    // Sortujemy po godzinie startu (a jak start ten sam, to dłuższe zajęcia pierwsze)
    val sortedClasses = classes.sortedWith(
        compareBy({ it.startTime.toMinutes() }, { -it.endTime.toMinutes() })
    )

    val result = mutableListOf<EventLayoutInfo>()
    val clusters = mutableListOf<List<ClassEntity>>()
    var currentCluster = mutableListOf<ClassEntity>()
    var clusterEnd = 0

    // 1. Zbieramy zajęcia w grupy (klastry), które na siebie nachodzą
    for (classItem in sortedClasses) {
        val start = classItem.startTime.toMinutes()
        val end = classItem.endTime.toMinutes()

        if (currentCluster.isEmpty()) {
            currentCluster.add(classItem)
            clusterEnd = end
        } else {
            if (start < clusterEnd) {
                currentCluster.add(classItem)
                clusterEnd = max(clusterEnd, end)
            } else {
                clusters.add(currentCluster)
                currentCluster = mutableListOf(classItem)
                clusterEnd = end
            }
        }
    }

    if (currentCluster.isNotEmpty()) {
        clusters.add(currentCluster)
    }

    // 2. W każdym klastrze przydzielamy zajęcia do kolejnych kolumn
    for (cluster in clusters) {
        val columns = mutableListOf<MutableList<ClassEntity>>()
        val classToColumnMap = mutableMapOf<ClassEntity, Int>()

        for (classItem in cluster) {
            val start = classItem.startTime.toMinutes()
            var isPlaced = false

            // Szukamy kolumny, w której poprzednie zajęcia już się skończyły
            for (i in columns.indices) {
                val lastEventEnd = columns[i].last().endTime.toMinutes()
                if (start >= lastEventEnd) {
                    columns[i].add(classItem)
                    classToColumnMap[classItem] = i
                    isPlaced = true
                    break
                }
            }

            // Jeśli wszystkie kolumny są zajęte, dodajemy nową kolumnę obok
            if (!isPlaced) {
                columns.add(mutableListOf(classItem))
                classToColumnMap[classItem] = columns.size - 1
            }
        }

        val totalCols = columns.size
        for (classItem in cluster) {
            result.add(
                EventLayoutInfo(
                    classEntity = classItem,
                    colIndex = classToColumnMap[classItem] ?: 0,
                    totalCols = totalCols
                )
            )
        }
    }

    return result
}

// Zamienia "08:30" na minuty od północy (np. 510) do łatwego porównywania
private fun String.toMinutes(): Int {
    val parts = this.split(":")
    if (parts.size != 2) return 0
    return (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
}