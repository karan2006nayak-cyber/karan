package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<VaultFolder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: VaultFolder): Long

    @Update
    suspend fun updateFolder(folder: VaultFolder)

    @Delete
    suspend fun deleteFolder(folder: VaultFolder)

    @Query("SELECT * FROM vault_photos ORDER BY createdAt DESC")
    fun getAllPhotos(): Flow<List<VaultPhoto>>

    @Query("SELECT * FROM vault_photos WHERE folderId = :folderId ORDER BY createdAt DESC")
    fun getPhotosInFolder(folderId: Long): Flow<List<VaultPhoto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: VaultPhoto): Long

    @Delete
    suspend fun deletePhoto(photo: VaultPhoto)

    @Query("SELECT COUNT(*) FROM vault_photos WHERE folderId = :folderId")
    suspend fun getPhotoCountForFolder(folderId: Long): Int
}
