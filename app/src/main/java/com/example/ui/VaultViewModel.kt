package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.VaultFolder
import com.example.data.VaultPhoto
import com.example.data.VaultRepository
import com.example.security.SecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class VaultViewModel(
    private val repository: VaultRepository,
    private val securityManager: SecurityManager
) : ViewModel() {

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    val selectedFolderId: StateFlow<Long?> = _selectedFolderId.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _decryptedPreviewBytes = MutableStateFlow<ByteArray?>(null)
    val decryptedPreviewBytes: StateFlow<ByteArray?> = _decryptedPreviewBytes.asStateFlow()

    private val _previewPhoto = MutableStateFlow<VaultPhoto?>(null)
    val previewPhoto: StateFlow<VaultPhoto?> = _previewPhoto.asStateFlow()

    private val _biometricTrigger = MutableStateFlow(false)
    val biometricTrigger: StateFlow<Boolean> = _biometricTrigger.asStateFlow()

    val folders: StateFlow<List<VaultFolder>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPhotos: StateFlow<List<VaultPhoto>> = repository.allPhotos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Photos filtered by the selected folder
    val photosInSelectedFolder: StateFlow<List<VaultPhoto>> = combine(
        repository.allPhotos,
        _selectedFolderId
    ) { photos, selectedId ->
        if (selectedId == null) {
            photos
        } else {
            photos.filter { it.folderId == selectedId }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isSetupComplete: Boolean
        get() = securityManager.isSetupComplete()

    val isBiometricsEnabled: Boolean
        get() = securityManager.isBiometricsEnabled()

    init {
        // Initially vault is locked
        securityManager.lockVault()
        _isUnlocked.value = false
    }

    fun setupPin(pin: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = securityManager.setupPin(pin)
            if (success) {
                // Auto seed default folders
                repository.createFolder("📸 Personal Photos", "#EC4899") // Pink
                repository.createFolder("💳 Cards & IDs", "#10B981") // Green
                repository.createFolder("📁 Secret Docs", "#3B82F6") // Blue
                _isUnlocked.value = true
                _error.value = null
            } else {
                _error.value = "PIN setup failed. Please try again."
            }
            _isLoading.value = false
        }
    }

    fun enterPin(pin: String): Boolean {
        val success = securityManager.verifyPin(pin)
        if (success) {
            _isUnlocked.value = true
            _error.value = null
        } else {
            _error.value = "Incorrect PIN. Access Denied."
        }
        return success
    }

    fun lockVault() {
        securityManager.lockVault()
        _isUnlocked.value = false
        _selectedFolderId.value = null
        _decryptedPreviewBytes.value = null
        _previewPhoto.value = null
    }

    fun selectFolder(folderId: Long?) {
        _selectedFolderId.value = folderId
    }

    fun createFolder(name: String, colorHex: String) {
        viewModelScope.launch {
            repository.createFolder(name, colorHex)
        }
    }

    fun deleteFolder(folder: VaultFolder) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.deleteFolder(folder)
            if (_selectedFolderId.value == folder.id) {
                _selectedFolderId.value = null
            }
            _isLoading.value = false
        }
    }

    fun importPhoto(uri: Uri, originalName: String) {
        val folderId = _selectedFolderId.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.savePhotoToVault(uri, folderId, originalName)
            if (result.isFailure) {
                _error.value = "Failed to encrypt and hide photo: ${result.exceptionOrNull()?.message}"
            } else {
                _error.value = null
            }
            _isLoading.value = false
        }
    }

    suspend fun decryptPhotoBytes(photo: VaultPhoto): ByteArray? {
        return repository.loadPhotoFromVault(photo).getOrNull()
    }

    fun showPhotoPreview(photo: VaultPhoto) {
        viewModelScope.launch {
            _isLoading.value = true
            _previewPhoto.value = photo
            val result = repository.loadPhotoFromVault(photo)
            if (result.isSuccess) {
                _decryptedPreviewBytes.value = result.getOrThrow()
                _error.value = null
            } else {
                _error.value = "Failed to decrypt and load image."
                _previewPhoto.value = null
                _decryptedPreviewBytes.value = null
            }
            _isLoading.value = false
        }
    }

    fun closePhotoPreview() {
        _decryptedPreviewBytes.value = null
        _previewPhoto.value = null
    }

    fun deletePhoto(photo: VaultPhoto) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.deletePhotoFromVault(photo)
            if (_previewPhoto.value?.id == photo.id) {
                closePhotoPreview()
            }
            _isLoading.value = false
        }
    }

    fun toggleBiometrics(enabled: Boolean, context: Context): Boolean {
        // If enabling, verify we can associate with biometrics
        if (enabled) {
            val pin = securityManager.getSavedPinForBiometrics()
            if (pin != null) {
                securityManager.setBiometricsEnabled(true)
                return true
            }
            // Need to save PIN first if enabling biometrics
            _error.value = "Please enter your PIN again to link biometrics."
            return false
        } else {
            securityManager.setBiometricsEnabled(false)
            securityManager.clearPinForBiometrics()
            return true
        }
    }

    fun linkBiometricsWithPin(pin: String): Boolean {
        if (securityManager.verifyPin(pin)) {
            securityManager.savePinForBiometrics(pin)
            securityManager.setBiometricsEnabled(true)
            _error.value = null
            return true
        } else {
            _error.value = "Invalid PIN. Cannot link biometrics."
            return false
        }
    }

    fun unlockWithBiometricsOnSuccess() {
        val savedPin = securityManager.getSavedPinForBiometrics()
        if (savedPin != null) {
            val success = securityManager.verifyPin(savedPin)
            if (success) {
                _isUnlocked.value = true
                _error.value = null
            }
        } else {
            _error.value = "Biometrics unlock configured, but PIN credentials missing."
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun triggerBiometricPrompt(show: Boolean) {
        _biometricTrigger.value = show
    }
}
