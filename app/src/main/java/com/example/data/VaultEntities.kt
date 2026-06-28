package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_folders")
data class VaultFolder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val colorHex: String = "#4F46E5" // Default indigo accent
)

@Entity(tableName = "vault_photos")
data class VaultPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String, // Random unique name on internal storage
    val originalName: String,
    val folderId: Long, // Linked to VaultFolder
    val createdAt: Long = System.currentTimeMillis(),
    val fileSize: Long
)
