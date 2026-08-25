package com.example.my_uz_android.util

import com.example.my_uz_android.data.models.ClassEntity
import java.time.LocalDate
import java.time.LocalTime

/**
 * Weryfikuje, czy pojedyncze zajęcia są wciąż aktualne w kontekście zadanego dnia i czasu.
 *
 * Zabezpiecza logikę wyświetlania przed błędnym parsowaniem czasu (wykorzystując [runCatching]).
 * Funkcja niezbędna do dynamicznego odświeżania stanu aplikacji, pozwalająca na
 * identyfikację zajęć, które już się zakończyły i nie powinny angażować uwagi studenta.
 */
fun isClassStillRemainingToday(
    classItem: ClassEntity,
    today: LocalDate,
    nowTime: LocalTime
): Boolean {
    if (classItem.date != today.toString()) return false

    val endTime = runCatching { LocalTime.parse(classItem.endTime) }.getOrNull() ?: return false
    return endTime.isAfter(nowTime)
}

/**
 * Filtruje pełną listę zajęć, wyodrębniając wyłącznie te wydarzenia, które zaplanowano
 * na bieżący dzień i które wciąż trwają lub dopiero się rozpoczną.
 *
 * Ułatwia przygotowanie danych dla priorytetowych widoków interfejsu użytkownika
 * (np. sekcji "Najbliższe zajęcia" na ekranie głównym aplikacji), dostarczając
 * studentowi tylko wysoce relewantne informacje.
 */
fun classesStillRemainingToday(
    classes: List<ClassEntity>,
    today: LocalDate,
    nowTime: LocalTime
): List<ClassEntity> {
    return classes.filter { isClassStillRemainingToday(it, today, nowTime) }
}