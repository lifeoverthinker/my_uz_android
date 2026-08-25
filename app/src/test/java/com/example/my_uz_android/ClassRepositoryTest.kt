package com.example.my_uz_android

import android.util.Log
import com.example.my_uz_android.data.daos.ClassDao
import com.example.my_uz_android.data.models.ClassEntity
import com.example.my_uz_android.data.repositories.ClassRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class ClassRepositoryTest {

    private lateinit var classDao: ClassDao
    private lateinit var classRepository: ClassRepository

    @Before
    fun setUp() {
        classDao = mockk()
        classRepository = ClassRepository(classDao)

        // Mockujemy klasę Log z Androida, aby testy nie "wybuchały" przy próbie logowania
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getUpcomingClasses o 12_00 powinno zwrocic tylko zajecia popoludniowe z dzisiaj`() = runTest {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val tomorrow = today.plusDays(1)
        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        val endedTime = if (now.toLocalTime() > LocalTime.of(0, 1)) {
            now.minusMinutes(1).toLocalTime().format(formatter)
        } else {
            "00:00"
        }

        val fakeClasses = listOf(
            createClass(1, today.minusDays(1).toString(), "08:00", "09:30"),
            createClass(2, today.toString(), "00:00", endedTime),
            createClass(3, tomorrow.toString(), "08:00", "09:30"),
            createClass(4, tomorrow.plusDays(1).toString(), "10:00", "11:30")
        )

        every { classDao.getAllClasses() } returns flowOf(fakeClasses)

        val result = classRepository.getUpcomingClasses().first()

        assertEquals(2, result.size)
        assertEquals(3, result[0].id)
        assertEquals(4, result[1].id)
    }

    @Test
    fun `getUpcomingClasses o 21_00 powinno zwrocic zajecia z kolejnego dnia`() = runTest {
        val today = LocalDate.now()
        val nextDay = today.plusDays(1)
        val dayAfterNext = today.plusDays(2)

        val fakeClasses = listOf(
            createClass(1, today.minusDays(2).toString(), "08:00", "09:30"),
            createClass(2, today.minusDays(1).toString(), "14:00", "15:30"),
            createClass(3, nextDay.toString(), "08:00", "09:30"),
            createClass(4, dayAfterNext.toString(), "10:00", "11:30")
        )

        every { classDao.getAllClasses() } returns flowOf(fakeClasses)

        val result = classRepository.getUpcomingClasses().first()

        assertEquals(2, result.size)
        assertEquals(3, result[0].id)
        assertEquals(nextDay.toString(), result[0].date)
    }

    @Test
    fun `syncGroupClasses przy spam klikach zapisuje plan bez duplikatow`() = runTest {
        val duplicateA = createClass(10, "2026-04-15", "08:00", "09:30").copy(
            supabaseId = "SAME-1",
            subjectName = "Algorytmy"
        )
        val duplicateB = duplicateA.copy(id = 11)

        coEvery { classDao.replaceGroupClasses(any(), any()) } returns Unit

        classRepository.syncGroupClasses("INF-1", listOf(duplicateA, duplicateB))

        coVerify(exactly = 1) {
            classDao.replaceGroupClasses(
                "INF-1",
                match { deduped -> deduped.size == 1 && deduped.first().supabaseId == "SAME-1" }
            )
        }
    }

    // Funkcja pomocnicza uzupełniona o wymagane parametry
    private fun createClass(id: Int, date: String, start: String, end: String): ClassEntity {
        return ClassEntity(
            id = id,
            date = date,
            startTime = start,
            endTime = end,
            subjectName = "Test",
            classType = "W",
            room = "A1",
            groupCode = "G1",
            teacherName = "Dr Test",
            dayOfWeek = 1,
            subgroup = "Grupa ogólna"
        )
    }
}