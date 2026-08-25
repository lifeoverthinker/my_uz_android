package com.example.my_uz_android.data.repositories

import com.example.my_uz_android.data.daos.FavoritesDao
import com.example.my_uz_android.data.models.FavoriteEntity
import kotlinx.coroutines.flow.Flow

class FavoritesRepository(private val favoritesDao: FavoritesDao) {

    val favoritesStream: Flow<List<FavoriteEntity>> = favoritesDao.getAllFavoritesStream()

    suspend fun insertFavorite(favorite: FavoriteEntity) = favoritesDao.insertFavorite(favorite)

    suspend fun deleteFavorite(favorite: FavoriteEntity) = favoritesDao.deleteFavorite(favorite)

    suspend fun deleteFavoriteByResourceId(resourceId: String) =
        favoritesDao.deleteByResourceId(resourceId)

    suspend fun existsByResourceIdAndType(resourceId: String, type: String): Boolean =
        favoritesDao.existsByResourceIdAndType(resourceId, type)

    suspend fun deleteFavoriteByResourceIdAndType(resourceId: String, type: String) =
        favoritesDao.deleteByResourceIdAndType(resourceId, type)

    suspend fun insertFavoriteIfAbsent(favorite: FavoriteEntity) {
        if (!favoritesDao.existsByResourceIdAndType(favorite.resourceId, favorite.type)) {
            favoritesDao.insertFavorite(favorite)
        }
        favoritesDao.deleteDuplicateEntries(favorite.resourceId, favorite.type)
    }
}