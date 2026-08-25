package com.example.my_uz_android

import com.example.my_uz_android.ui.screens.onboarding.OnboardingViewModel
import com.example.my_uz_android.data.models.SettingsEntity
import com.example.my_uz_android.data.models.UserGender
import com.example.my_uz_android.data.repositories.ClassRepository
import com.example.my_uz_android.data.repositories.SettingsRepository
import com.example.my_uz_android.data.repositories.UniversityRepository
import com.example.my_uz_android.data.repositories.UserCourseRepository
import com.example.my_uz_android.util.NetworkResult
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: OnboardingViewModel

    private val settingsRepository: SettingsRepository = mockk()
    private val universityRepository: UniversityRepository = mockk()
    private val classRepository: ClassRepository = mockk()
    private val userCourseRepository: UserCourseRepository = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        coEvery { universityRepository.getGroupCodes() } returns mockk<NetworkResult.Success<List<String>>> {
            every { data } returns listOf("32INF-SP", "11MED-SP")
        }

        viewModel = OnboardingViewModel(
            settingsRepository,
            universityRepository,
            classRepository,
            userCourseRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `sprawdz czy na poczatku stan UI State jest poprawny i nie ma wybranej grupy`() = runTest {
        assertNull("Początkowa grupa powinna być null", viewModel.selectedGroup.value)
        assertTrue("Początkowe imię powinno być puste", viewModel.userName.value.isEmpty())
        assertTrue("Początkowe nazwisko powinno być puste", viewModel.userSurname.value.isEmpty())
        assertNull("Początkowa płeć powinna być null", viewModel.selectedGender.value)
        assertTrue("Lista podgrup powinna być pusta", viewModel.selectedSubgroups.value.isEmpty())
    }

    @Test
    fun `sprawdz czy saveOnboardingData poprawnie wola repository i wysyla sygnal zakonczenia`() = runTest {
        var isNavigationEventTriggered = false

        coEvery { universityRepository.getSubgroups(any()) } returns mockk<NetworkResult.Success<List<String>>> {
            every { data } returns listOf("L1", "L2")
        }

        coEvery { universityRepository.getGroupDetails(any()) } returns mockk<NetworkResult.Error<com.example.my_uz_android.data.repositories.GroupDetailsDto>>(relaxed = true)

        coEvery { settingsRepository.getSettingsStream() } returns flowOf(null)
        coEvery { settingsRepository.insertSettings(any<SettingsEntity>()) } just Runs

        viewModel.setUserName("Jan")
        viewModel.setUserSurname("Kowalski")
        viewModel.setGender(UserGender.STUDENT)
        viewModel.selectGroup("32INF-SP")

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleSubgroup("L1")

        viewModel.saveOnboardingData(
            onSuccess = { isNavigationEventTriggered = true }
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val settingsSlot = slot<SettingsEntity>()
        coVerify(exactly = 1) { settingsRepository.insertSettings(capture(settingsSlot)) }

        val savedEntity = settingsSlot.captured
        assertEquals("Jan Kowalski", savedEntity.userName)
        assertEquals(UserGender.STUDENT.name, savedEntity.gender)
        assertEquals("32INF-SP", savedEntity.selectedGroupCode)
        assertEquals("L1", savedEntity.selectedSubgroup)
        assertFalse("Flaga isAnonymous powinna być false przy pełnym zapisie", savedEntity.isAnonymous)
        assertFalse("Flaga isFirstRun powinna być zgaszona po onboardingu", savedEntity.isFirstRun)

        assertTrue("Sygnał o zakończeniu onboardingu (onSuccess) nie został wywołany", isNavigationEventTriggered)
    }
}