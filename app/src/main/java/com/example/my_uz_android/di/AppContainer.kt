package com.example.my_uz_android.di

import android.content.Context
import com.example.my_uz_android.data.db.AppDatabase
import com.example.my_uz_android.data.provideSupabaseClient
import com.example.my_uz_android.data.repositories.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

interface AppContainer {
    val settingsRepository: SettingsRepository
    val universityRepository: UniversityRepository
    val classRepository: ClassRepository
    val tasksRepository: TasksRepository
    val gradesRepository: GradesRepository
    val absenceRepository: AbsenceRepository
    val eventRepository: EventRepository
    val favoritesRepository: FavoritesRepository
    val userCourseRepository: UserCourseRepository
    val notificationsRepository: NotificationsRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }
    private val supabase: SupabaseClient by lazy {
        provideSupabaseClient()
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(database.settingsDao())
    }

    override val universityRepository: UniversityRepository by lazy {
        UniversityRepository(supabase.postgrest, context)
    }

    override val classRepository: ClassRepository by lazy {
        ClassRepository(database.classDao())
    }

    override val tasksRepository: TasksRepository by lazy {
        TasksRepository(database.tasksDao(), supabase)
    }

    override val gradesRepository: GradesRepository by lazy {
        GradesRepository(database.gradesDao())
    }

    override val absenceRepository: AbsenceRepository by lazy {
        AbsenceRepository(database.absenceDao())
    }

    override val eventRepository: EventRepository by lazy {
        EventRepository(database.eventDao())
    }

    override val favoritesRepository: FavoritesRepository by lazy {
        FavoritesRepository(database.favoritesDao())
    }

    override val userCourseRepository: UserCourseRepository by lazy {
        UserCourseRepository(database.userCourseDao())
    }

    override val notificationsRepository: NotificationsRepository by lazy {
        NotificationsRepository(database.notificationDao())
    }
}