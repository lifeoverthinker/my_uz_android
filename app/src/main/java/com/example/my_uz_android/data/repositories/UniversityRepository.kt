package com.example.my_uz_android.data.repositories

import android.content.Context
import android.util.Log
import com.example.my_uz_android.data.models.ClassEntity
import com.example.my_uz_android.util.NetworkResult
import com.example.my_uz_android.util.NotificationHelper
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// =========================================================================
// MODELE DTO (Data Transfer Objects)
// =========================================================================

@Serializable
data class ClassScheduleDto(
    @SerialName("uid") val uidRaw: JsonElement? = null,
    @SerialName("id") val idRaw: JsonElement? = null,
    @SerialName("przedmiot") val przedmiot: String? = null,
    @SerialName("nazwa") val fallbackSubject: String? = null,
    @SerialName("rodzaj_zajec") val rodzaj: String? = null,
    @SerialName("typ") val fallbackType: String? = null,
    @SerialName("poczatek") val poczatek: String? = null,
    @SerialName("data_od") val fallbackStart: String? = null,
    @SerialName("koniec") val koniec: String? = null,
    @SerialName("data_do") val fallbackEnd: String? = null,
    @SerialName("sala") val salaRaw: JsonElement? = null,
    @SerialName("podgrupa") val podgrupaRaw: JsonElement? = null,
    @SerialName("grupa") val fallbackSubgroupRaw: JsonElement? = null,
    @SerialName("nauczyciel") val teacher: String? = null,
    @SerialName("wykladowca") val fallbackTeacher: String? = null
) {
    val id get() = (uidRaw ?: idRaw)?.jsonPrimitive?.contentOrNull
    val subjectName get() = przedmiot ?: fallbackSubject
    val classType get() = rodzaj ?: fallbackType
    val startDateTime get() = poczatek ?: fallbackStart
    val endDateTime get() = koniec ?: fallbackEnd
    val room get() = salaRaw?.jsonPrimitive?.contentOrNull
    val resolvedSubgroup get() = (podgrupaRaw ?: fallbackSubgroupRaw)?.jsonPrimitive?.contentOrNull
    val resolvedTeacher get() = teacher ?: fallbackTeacher
}

@Serializable
data class TeacherDetailsDto(
    @SerialName("nazwisko_imie") val name: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("jednostka") val institute: String? = null
)

@Serializable
data class GroupCodeDto(
    @SerialName("nazwa") val nazwaRaw: JsonElement? = null,
    @SerialName("kod") val kodRaw: JsonElement? = null
) {
    val code get() = (nazwaRaw ?: kodRaw)?.jsonPrimitive?.contentOrNull
}

@Serializable
data class GroupSearchDto(
    @SerialName("nazwa") val nazwaRaw: JsonElement? = null,
    @SerialName("kod") val kodRaw: JsonElement? = null,
    @SerialName("kierunki") val kierunkiRaw: JsonElement? = null
) {
    val code get() = (nazwaRaw ?: kodRaw)?.jsonPrimitive?.contentOrNull
}

data class GroupSearchMeta(
    val code: String,
    val studyFields: List<String>
)

@Serializable
data class GroupIdDto(
    @SerialName("grupa_id") val grupaIdRaw: JsonElement? = null,
    @SerialName("id") val idRaw: JsonElement? = null
) {
    val resolvedId get() = (grupaIdRaw ?: idRaw)?.jsonPrimitive?.contentOrNull
}

@Serializable
data class SubgroupDto(
    @SerialName("podgrupa") val podgrupaRaw: JsonElement? = null,
    @SerialName("nazwa") val nazwaRaw: JsonElement? = null
) {
    val subgroup get() = (podgrupaRaw ?: nazwaRaw)?.jsonPrimitive?.contentOrNull
}

@Serializable
data class TeacherDto(@SerialName("nazwisko_imie") val name: String? = null)

@Serializable
data class GroupDetailsDto(
    @SerialName("tryb") val studyMode: String? = null,
    @SerialName("semestr") val semesterRaw: JsonElement? = null,
    @SerialName("kierunki") val fieldInfo: FieldOfStudyDto? = null
) {
    val semester get() = semesterRaw?.jsonPrimitive?.contentOrNull
}

@Serializable
data class FieldOfStudyDto(
    @SerialName("wydzial") val faculty: String? = null,
    @SerialName("nazwa") val name: String? = null
)

@Serializable
data class TeacherClassScheduleDto(
    @SerialName("uid") val idRaw: JsonElement? = null,
    @SerialName("przedmiot") val subjectName: String? = null,
    @SerialName("rodzaj_zajec") val classType: String? = null,
    @SerialName("poczatek") val startDateTime: String? = null,
    @SerialName("koniec") val endDateTime: String? = null,
    @SerialName("sala") val roomRaw: JsonElement? = null,
    @SerialName("grupy") val groupsRaw: JsonElement? = null
) {
    val id get() = idRaw?.jsonPrimitive?.contentOrNull
    val room get() = roomRaw?.jsonPrimitive?.contentOrNull
    val groups get() = groupsRaw?.jsonPrimitive?.contentOrNull
}

@Serializable
data class TeacherIdDto(
    @SerialName("id") val idRaw: JsonElement? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("jednostka") val institute: String? = null
) {
    val id get() = idRaw?.jsonPrimitive?.contentOrNull
}

// =========================================================================
// REPOZYTORIUM API (UniversityRepository)
// =========================================================================
class UniversityRepository(
    private val supabase: Postgrest,
    private val context: Context
) {
    private fun extractStudyFieldNames(raw: JsonElement?): List<String> {
        if (raw == null) return emptyList()

        return try {
            when {
                raw is kotlinx.serialization.json.JsonArray -> raw.jsonArray
                    .mapNotNull { element ->
                        element.jsonObject["nazwa"]?.jsonPrimitive?.contentOrNull?.trim()
                    }
                    .filter { it.isNotBlank() }
                    .distinct()

                raw is kotlinx.serialization.json.JsonObject -> listOfNotNull(
                    raw.jsonObject["nazwa"]?.jsonPrimitive?.contentOrNull?.trim()
                ).filter { it.isNotBlank() }

                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun tokenizeSubgroups(rawValues: List<String>): List<String> {
        return rawValues
            .asSequence()
            .flatMap { it.split(Regex("[,;/|\\s]+")) .asSequence() }
            .map { it.trim().uppercase() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }

    private fun tokenizeSubgroups(rawValue: String?): List<String> {
        if (rawValue.isNullOrBlank()) return emptyList()
        return tokenizeSubgroups(listOf(rawValue))
    }

    private fun parseDateSafe(dateString: String?): LocalDateTime? {
        if (dateString.isNullOrBlank()) return null
        return try {
            val cleanString = dateString.substringBefore("+").substringBefore("Z").replace(" ", "T")
            LocalDateTime.parse(cleanString)
        } catch (e: Exception) { null }
    }

    private suspend fun getGrupaId(groupName: String): String? {
        return try {
            val result = supabase.from("grupy").select { filter { eq("nazwa", groupName) } }.decodeList<GroupIdDto>()
            result.firstOrNull()?.resolvedId
        } catch (e: Exception) {
            Log.e("UniversityRepository", "Błąd parsowania JSON dla tabeli 'grupy': ${e.message}", e)
            null
        }
    }

    suspend fun getSchedule(groupCode: String, subgroups: List<String>): NetworkResult<List<ClassEntity>> {
        return try {
            val grupaId = getGrupaId(groupCode) ?: return NetworkResult.Error("Nie znaleziono ID grupy.")

            val dtoList = supabase.from("zajecia_grupy").select {
                filter { eq("grupa_id", grupaId) }
            }.decodeList<ClassScheduleDto>()

            val safeSubgroups = tokenizeSubgroups(subgroups)

            val filteredList = if (safeSubgroups.isEmpty() || safeSubgroups.contains("ALL")) {
                dtoList
            } else {
                dtoList.filter { dto ->
                    val rawSub = dto.resolvedSubgroup?.trim()?.uppercase() ?: ""

                    val isCommon = rawSub.isBlank() ||
                            rawSub in listOf("EMPTY", "-", "BRAK", "ALL", "W", "WYK", "WYKŁAD", "SEM", "KONV", "PROJ") ||
                            rawSub.startsWith("WYK") ||
                            rawSub.startsWith("SEM")

                    if (isCommon) return@filter true

                    val classSubgroups = tokenizeSubgroups(listOf(rawSub))

                    safeSubgroups.any { userSub ->
                        classSubgroups.contains(userSub) || rawSub == userSub || rawSub.contains(userSub)
                    }
                }
            }

            val teacherNames = filteredList.mapNotNull { it.resolvedTeacher?.trim() }.filter { it.isNotBlank() }.distinct()
            val teacherMap = fetchTeacherDetails(teacherNames)
            val entities = mapDtoToEntity(filteredList, groupCode, teacherMap)

            NetworkResult.Success(entities)
        } catch (e: Exception) {
            Log.e("UniversityRepository", "Błąd zapytania Supabase dla planu: ${e.message}", e)
            NetworkResult.Error("Błąd komunikacji z bazą uczelni.")
        }
    }

    suspend fun searchGroups(query: String): NetworkResult<List<String>> {
        return try {
            val result = supabase.from("grupy")
                .select(columns = Columns.raw("nazwa, kierunki!inner(nazwa)")) {
                    filter { or { ilike("nazwa", "%$query%"); ilike("kierunki.nazwa", "%$query%") } }
                }.decodeList<GroupCodeDto>().mapNotNull { it.code }.distinct().sorted()
            NetworkResult.Success(result)
        } catch (e: Exception) {
            try {
                val fallback = supabase.from("grupy").select { filter { ilike("nazwa", "%$query%") } }
                    .decodeList<GroupCodeDto>().mapNotNull { it.code }.distinct().sorted()
                NetworkResult.Success(fallback)
            } catch (e2: Exception) { NetworkResult.Error("Błąd wyszukiwania grup.") }
        }
    }

    /** Wyszukuje wykładowców wykorzystując elastyczne mapowanie wielu słów z użyciem OR oraz dodaje empty states. */
    suspend fun searchTeachers(query: String): NetworkResult<List<TeacherDetailsDto>> {
        return try {
            val searchTerms = query.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }

            val result = supabase.from("nauczyciele").select {
                if (searchTerms.isNotEmpty()) {
                    filter {
                        or {
                            searchTerms.forEach { term ->
                                ilike("nazwisko_imie", "%$term%")
                            }
                        }
                    }
                }
            }.decodeList<TeacherDetailsDto>()

            val aggregated = result.groupBy { it.name ?: "" }
                .filterKeys { it.isNotBlank() }
                .map { (name, dtos) ->
                    TeacherDetailsDto(
                        name = name,
                        email = dtos.mapNotNull { it.email }.filter { it.isNotBlank() }.distinct().joinToString(" • ").ifBlank { "Brak e-maila" },
                        institute = dtos.mapNotNull { it.institute }.filter { it.isNotBlank() }.distinct().joinToString(" • ").ifBlank { "Brak przypisanej jednostki" }
                    )
                }.sortedBy { it.name }

            NetworkResult.Success(aggregated)
        } catch (e: Exception) {
            Log.e("UniversityRepository", "Błąd wyszukiwania: ${e.message}", e)
            NetworkResult.Error("Błąd wyszukiwania wykładowców.")
        }
    }

    suspend fun getAllTeachers(): NetworkResult<List<String>> {
        return try {
            val result = supabase.from("nauczyciele").select().decodeList<TeacherDto>().mapNotNull { it.name }.distinct().sorted()
            NetworkResult.Success(result)
        } catch (e: Exception) { NetworkResult.Error("Błąd pobierania wykładowców.") }
    }

    suspend fun getGroupCodes(): NetworkResult<List<String>> {
        return try {
            val result = supabase.from("grupy").select().decodeList<GroupCodeDto>().mapNotNull { it.code }.distinct().sorted()
            NetworkResult.Success(result)
        } catch (e: Exception) { NetworkResult.Error("Błąd pobierania grup.") }
    }

    suspend fun getGroupsForSearch(): NetworkResult<List<GroupSearchMeta>> {
        return try {
            val result = supabase.from("grupy")
                .select(columns = Columns.raw("nazwa, kod, kierunki:kierunek_id(nazwa)"))
                .decodeList<GroupSearchDto>()
                .mapNotNull { dto ->
                    val code = dto.code?.trim().orEmpty()
                    if (code.isBlank()) return@mapNotNull null
                    GroupSearchMeta(
                        code = code,
                        studyFields = extractStudyFieldNames(dto.kierunkiRaw)
                    )
                }
                .distinctBy { it.code }
                .sortedBy { it.code }

            NetworkResult.Success(result)
        } catch (e: Exception) {
            val fallback = when (val codes = getGroupCodes()) {
                is NetworkResult.Success -> (codes.data ?: emptyList()).map {
                    GroupSearchMeta(code = it, studyFields = emptyList())
                }
                is NetworkResult.Error -> emptyList()
            }

            if (fallback.isNotEmpty()) {
                NetworkResult.Success(fallback)
            } else {
                NetworkResult.Error("Błąd pobierania grup do wyszukiwania.")
            }
        }
    }

    suspend fun getSubgroups(groupCode: String): NetworkResult<List<String>> {
        return try {
            val grupaId = getGrupaId(groupCode) ?: return NetworkResult.Error("Brak grupy.")
            val result = supabase.from("zajecia_grupy").select { filter { eq("grupa_id", grupaId) } }.decodeList<SubgroupDto>()

            val safeSubgroups = result.mapNotNull { it.subgroup }.map { it.trim() }
                .filter { it.isNotBlank() && !it.equals("empty", ignoreCase = true) && it != "-" && !it.equals("brak", ignoreCase = true) && !it.equals("all", ignoreCase = true) }
                .distinct().sorted()

            NetworkResult.Success(safeSubgroups)
        } catch (e: Exception) {
            NetworkResult.Error("Błąd odczytu podgrup.")
        }
    }

    suspend fun getGroupDetails(groupCode: String): NetworkResult<GroupDetailsDto> {
        return try {
            val details = supabase.from("grupy").select(columns = Columns.raw("tryb, semestr, kierunki:kierunek_id(wydzial, nazwa)")) { filter { eq("nazwa", groupCode) } }.decodeList<GroupDetailsDto>()
            details.firstOrNull()?.let { NetworkResult.Success(it) } ?: NetworkResult.Error("Brak danych.")
        } catch (e: Exception) { NetworkResult.Error("Błąd pobierania detali grupy.") }
    }

    suspend fun getScheduleForTeacher(teacherName: String): NetworkResult<List<ClassEntity>> {
        return try {
            val teachers = supabase.from("nauczyciele").select { filter { eq("nazwisko_imie", teacherName.trim()) } }.decodeList<TeacherIdDto>()
            if (teachers.isEmpty()) return NetworkResult.Error("Brak danych w bazie.")

            val aggregatedEmail = teachers.mapNotNull { it.email }.filter { it.isNotBlank() }.distinct().joinToString(" • ").ifBlank { "Brak e-maila" }
            val aggregatedInstitute = teachers.mapNotNull { it.institute }.filter { it.isNotBlank() }.distinct().joinToString(" • ").ifBlank { "Brak przypisanej jednostki" }

            val teacherIds = teachers.mapNotNull { it.id }.filter { it.isNotBlank() }.distinct()
            val dtoList = supabase.from("zajecia_nauczyciela").select { filter { isIn("nauczyciel_id", teacherIds) } }.decodeList<TeacherClassScheduleDto>()

            val entities = dtoList.mapNotNull { dto ->
                val startDT = parseDateSafe(dto.startDateTime) ?: return@mapNotNull null
                val endDT = parseDateSafe(dto.endDateTime) ?: return@mapNotNull null
                ClassEntity(
                    supabaseId = dto.id ?: java.util.UUID.randomUUID().toString(), subjectName = dto.subjectName ?: "Brak nazwy",
                    classType = dto.classType ?: "Inne", startTime = startDT.format(DateTimeFormatter.ofPattern("HH:mm")),
                    endTime = endDT.format(DateTimeFormatter.ofPattern("HH:mm")), dayOfWeek = startDT.dayOfWeek.value,
                    date = startDT.toLocalDate().toString(), groupCode = dto.groups ?: "", subgroup = null,
                    teacherName = teacherName, teacherEmail = aggregatedEmail, teacherInstitute = aggregatedInstitute, room = dto.room ?: "Brak"
                )
            }.sortedWith(compareBy({ it.date }, { it.startTime }))

            NetworkResult.Success(entities)
        } catch (e: Exception) { NetworkResult.Error("Błąd odczytu planu wykładowcy.") }
    }

    private fun mapDtoToEntity(dtoList: List<ClassScheduleDto>, groupCode: String, teacherMap: Map<String, TeacherDetailsDto>): List<ClassEntity> {
        return dtoList.mapNotNull { dto ->
            val startDT = parseDateSafe(dto.startDateTime) ?: return@mapNotNull null
            val endDT = parseDateSafe(dto.endDateTime) ?: return@mapNotNull null

            val rawSub = dto.resolvedSubgroup?.trim()
            val isCommon = rawSub.isNullOrBlank() || rawSub.equals("empty", ignoreCase = true) || rawSub == "-" || rawSub.equals("brak", ignoreCase = true) || rawSub.equals("all", ignoreCase = true)

            ClassEntity(
                supabaseId = dto.id ?: java.util.UUID.randomUUID().toString(), subjectName = dto.subjectName ?: "Brak nazwy", classType = dto.classType ?: "Inne",
                startTime = startDT.format(DateTimeFormatter.ofPattern("HH:mm")), endTime = endDT.format(DateTimeFormatter.ofPattern("HH:mm")),
                dayOfWeek = startDT.dayOfWeek.value, date = startDT.toLocalDate().toString(), groupCode = groupCode,
                subgroup = if (isCommon) null else rawSub, teacherName = dto.resolvedTeacher ?: "Brak danych",
                teacherEmail = teacherMap[dto.resolvedTeacher]?.email, teacherInstitute = teacherMap[dto.resolvedTeacher]?.institute, room = dto.room ?: "Brak"
            )
        }.sortedWith(compareBy({ it.date }, { it.startTime }))
    }

    private suspend fun fetchTeacherDetails(teacherNames: List<String>): Map<String, TeacherDetailsDto> {
        if (teacherNames.isEmpty()) return emptyMap()
        return try {
            val list = supabase.from("nauczyciele").select { filter { isIn("nazwisko_imie", teacherNames) } }.decodeList<TeacherDetailsDto>()
            list.groupBy { it.name ?: "" }.filterKeys { it.isNotBlank() }.mapValues { (name, dtos) ->
                TeacherDetailsDto(
                    name = name,
                    email = dtos.mapNotNull { it.email }.filter { it.isNotBlank() }.distinct().joinToString(" • ").ifBlank { "Brak e-maila" },
                    institute = dtos.mapNotNull { it.institute }.filter { it.isNotBlank() }.distinct().joinToString(" • ").ifBlank { "Brak przypisanej jednostki" }
                )
            }
        } catch (e: Exception) { emptyMap() }
    }

    suspend fun getAllTeachersWithDetails(): NetworkResult<List<TeacherDetailsDto>> {
        return try {
            val result = supabase.from("nauczyciele").select().decodeList<TeacherDetailsDto>()
            val aggregated = result.groupBy { it.name ?: "" }.filterKeys { it.isNotBlank() }.map { (name, dtos) ->
                TeacherDetailsDto(
                    name = name,
                    email = dtos.mapNotNull { it.email }.filter { it.isNotBlank() }.distinct().joinToString(" • ").ifBlank { "Brak e-maila" },
                    institute = dtos.mapNotNull { it.institute }.filter { it.isNotBlank() }.distinct().joinToString(" • ").ifBlank { "Brak przypisanej jednostki" }
                )
            }.sortedBy { it.name }
            NetworkResult.Success(aggregated)
        } catch (e: Exception) { NetworkResult.Error("Błąd pobierania bazy wykładowców.") }
    }

    suspend fun refreshSchedule(groupCode: String, subgroup: String?, classRepository: ClassRepository): NetworkResult<Unit> {
        val subgroups = tokenizeSubgroups(subgroup)
        return when (val result = getSchedule(groupCode, subgroups)) {
            is NetworkResult.Success -> {
                val downloadedClasses = result.data ?: emptyList()
                classRepository.syncGroupClasses(groupCode, downloadedClasses)
                NotificationHelper.scheduleClassAlarms(context, downloadedClasses)
                NetworkResult.Success(Unit)
            }
            is NetworkResult.Error -> NetworkResult.Error(result.message ?: "Wystąpił nieoczekiwany błąd odświeżania.")
        }
    }
}