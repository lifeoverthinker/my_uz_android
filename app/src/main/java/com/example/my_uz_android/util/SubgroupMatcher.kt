package com.example.my_uz_android.util

import com.example.my_uz_android.data.models.SettingsEntity
import com.example.my_uz_android.data.models.UserCourseEntity

// Pomocnik do sprawdzania, czy zajęcia pasują do podgrupy studenta (np. Lab 1, Ćw 2)
object SubgroupMatcher {

    // Znaczniki oznaczające, że zajęcia są dla całego roku/grupy
    private val COMMON_TOKENS = setOf("all", "-", "brak", "w", "wyk", "wyklad", "wykład", "sem", "seminarium")

    // Główna funkcja sprawdzająca podgrupy (używana w testach i w AbsencesViewModel)
    fun matchesSubgroups(classSubgroupRaw: String?, selectedSubgroupsRaw: List<String?>): Boolean {
        val classTokens = classSubgroupRaw?.split(Regex("[,;/|\\s]+"))
            ?.map { it.trim().lowercase() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        // Brak podgrupy lub wykład -> widoczne dla każdego
        if (classTokens.isEmpty()) return true
        if (classTokens.any { it in COMMON_TOKENS }) return true

        val allowedTokens = selectedSubgroupsRaw.mapNotNull { it?.trim()?.lowercase() }
            .flatMap { it.split(Regex("[,;/|\\s]+")) }
            .filter { it.isNotBlank() }

        // Jeśli student nic nie wybrał, widzi wszystko
        if (allowedTokens.isEmpty()) return true

        // Sprawdzamy czy podgrupa zajęć pokrywa się z wybraną przez studenta
        return classTokens.any { classToken ->
            allowedTokens.any { allowedToken ->
                classToken == allowedToken || classToken.contains(allowedToken)
            }
        }
    }

    // Alias dla wstecznej kompatybilności (gdyby inne pliki wołały matches)
    fun matches(classSubgroupRaw: String?, selectedSubgroupsRaw: List<String?>): Boolean {
        return matchesSubgroups(classSubgroupRaw, selectedSubgroupsRaw)
    }

    // Tworzy mapę: "kod_grupy" -> ["lab 1", "ćw 2"] dla wszystkich kierunków studenta
    fun buildUserEnrollments(
        settings: SettingsEntity?,
        courses: List<UserCourseEntity>,
        activeGroupCodesLower: Set<String>? = null
    ): Map<String, List<String>> {
        val enrollments = mutableMapOf<String, MutableList<String>>()

        fun addEnrollment(groupCode: String?, subgroupRaw: String?) {
            val code = groupCode?.trim()?.lowercase() ?: return
            if (code.isBlank()) return

            if (activeGroupCodesLower != null && !activeGroupCodesLower.contains(code)) return

            val tokens = subgroupRaw?.split(Regex("[,;/|\\s]+"))
                ?.map { it.trim().lowercase() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            val currentList = enrollments.getOrPut(code) { mutableListOf() }
            currentList.addAll(tokens)
        }

        addEnrollment(settings?.selectedGroupCode, settings?.selectedSubgroup)
        courses.forEach { addEnrollment(it.groupCode, it.selectedSubgroup) }

        return enrollments
    }

    // Decyduje, czy dany kafelek zajęć ma się pokazać w Kalendarzu i na Ekranie Głównym
    fun isClassVisible(
        classGroupCode: String?,
        classType: String?,
        classSubgroup: String?,
        userEnrollments: Map<String, List<String>>
    ): Boolean {
        val code = classGroupCode?.trim()?.lowercase() ?: return false

        if (!userEnrollments.containsKey(code)) return false

        val userSubgroups = userEnrollments[code] ?: emptyList()

        if (userSubgroups.isEmpty()) return true

        val classTokens = classSubgroup?.split(Regex("[,;/|\\s]+"))
            ?.map { it.trim().lowercase() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        if (classTokens.isEmpty()) return true

        if (classTokens.any { it in COMMON_TOKENS }) return true

        // Wykłady, seminaria i egzaminy pokazujemy zawsze całemu kierunkowi
        val typeName = classType?.trim()?.lowercase() ?: ""
        if (typeName.contains("wykład") || typeName.contains("wyklad") ||
            typeName.contains("sem") || typeName.contains("egzamin") ||
            typeName.contains("samokształcenie")) {
            return true
        }

        return classTokens.any { classToken ->
            userSubgroups.any { userToken ->
                classToken == userToken || classToken.contains(userToken)
            }
        }
    }
}