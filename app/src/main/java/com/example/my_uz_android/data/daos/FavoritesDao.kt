package com.example.my_uz_android.data.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.my_uz_android.data.models.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorites ORDER BY name ASC")
    fun getAllFavoritesStream(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE resource_id = :resourceId LIMIT 1)")
    suspend fun isFavorite(resourceId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE resource_id = :resourceId AND type = :type LIMIT 1)")
    suspend fun existsByResourceIdAndType(resourceId: String, type: String): Boolean

    @Query("DELETE FROM favorites WHERE resource_id = :resourceId AND type = :type")
    suspend fun deleteByResourceIdAndType(resourceId: String, type: String)

    @Query("DELETE FROM favorites WHERE resource_id = :resourceId AND type = :type AND id NOT IN (SELECT MIN(id) FROM favorites WHERE resource_id = :resourceId AND type = :type)")
    suspend fun deleteDuplicateEntries(resourceId: String, type: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE resource_id = :resourceId")
    suspend fun deleteByResourceId(resourceId: String)

    @Query("DELETE FROM favorites")
    suspend fun deleteAll()
}