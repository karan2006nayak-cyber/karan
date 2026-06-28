package com.example.data

import android.content.Context
import android.net.Uri
import com.example.security.CryptoHelper
import com.example.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class VaultRepository(
    private val context: Context,
    private val dao: VaultDao,
    private val securityManager: SecurityManager
) {
    val allFolders: Flow<List<VaultFolder>> = dao.getAllFolders()
    val allPhotos: Flow<List<VaultPhoto>> = dao.getAllPhotos()

    fun getPhotosInFolder(folderId: Long): Flow<List<VaultPhoto>> = dao.getPhotosInFolder(folderId)

    suspend fun createFolder(name: String, colorHex: String): Long = withContext(Dispatchers.IO) {
        dao.insertFolder(VaultFolder(name = name, colorHex = colorHex))
    }

    suspend fun deleteFolder(folder: VaultFolder) = withContext(Dispatchers.IO) {
        // Delete all photos associated with this folder physically
        val vaultDir = File(context.filesDir, "vault/${folder.id}")
        if (vaultDir.exists()) {
            vaultDir.deleteRecursively()
        }
        // Room cascade or manual deletion of photos in DB:
        // We'll manually find and delete all photos in DB
        dao.deleteFolder(folder)
    }

    suspend fun savePhotoToVault(uri: Uri, folderId: Long, originalName: String): Result<VaultPhoto> = withContext(Dispatchers.IO) {
        val activeKey = securityManager.getActiveKey()
            ?: return@withContext Result.failure(IllegalStateException("Vault is locked"))

        try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Could not open input stream"))

            val originalBytes = inputStream.use { it.readBytes() }
            val encryptedBytes = CryptoHelper.encrypt(originalBytes, activeKey)

            // Ensure destination folder directory exists
            val folderDir = File(context.filesDir, "vault/$folderId")
            if (!folderDir.exists()) {
                folderDir.mkdirs()
            }

            val uniqueFileName = "${UUID.randomUUID()}.enc"
            val file = File(folderDir, uniqueFileName)
            
            FileOutputStream(file).use { output ->
                output.write(encryptedBytes)
            }

            val photo = VaultPhoto(
                fileName = uniqueFileName,
                originalName = originalName,
                folderId = folderId,
                fileSize = originalBytes.size.toLong()
            )

            val photoId = dao.insertPhoto(photo)
            val insertedPhoto = photo.copy(id = photoId)

            Result.success(insertedPhoto)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun loadPhotoFromVault(photo: VaultPhoto): Result<ByteArray> = withContext(Dispatchers.IO) {
        val activeKey = securityManager.getActiveKey()
            ?: return@withContext Result.failure(IllegalStateException("Vault is locked"))

        try {
            val file = File(context.filesDir, "vault/${photo.folderId}/${photo.fileName}")
            if (!file.exists()) {
                return@withContext Result.failure(Exception("File does not exist"))
            }

            val encryptedBytes = file.readBytes()
            val decryptedBytes = CryptoHelper.decrypt(encryptedBytes, activeKey)

            Result.success(decryptedBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun deletePhotoFromVault(photo: VaultPhoto) = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, "vault/${photo.folderId}/${photo.fileName}")
            if (file.exists()) {
                file.delete()
            }
            dao.deletePhoto(photo)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun exportPhotoFromVault(photo: VaultPhoto, outputStream: OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val decryptResult = loadPhotoFromVault(photo)
            if (decryptResult.isSuccess) {
                val bytes = decryptResult.getOrThrow()
                outputStream.use { out ->
                    out.write(bytes)
                }
                Result.success(Unit)
            } else {
                Result.failure(decryptResult.exceptionOrNull() ?: Exception("Decryption failed"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
