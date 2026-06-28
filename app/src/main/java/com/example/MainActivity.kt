package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.data.VaultDatabase
import com.example.data.VaultRepository
import com.example.security.SecurityManager
import com.example.ui.VaultViewModel
import com.example.ui.components.DashboardScreen
import com.example.ui.components.LockScreen
import com.example.ui.components.PhotoLightbox
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {

    private lateinit var securityManager: SecurityManager
    private lateinit var repository: VaultRepository
    private lateinit var viewModel: VaultViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core dependency setup
        securityManager = SecurityManager(applicationContext)
        val database = VaultDatabase.getDatabase(applicationContext)
        repository = VaultRepository(applicationContext, database.vaultDao(), securityManager)
        viewModel = VaultViewModel(repository, securityManager)

        setContent {
            MyApplicationTheme {
                val isUnlocked by viewModel.isUnlocked.collectAsState()
                val biometricTrigger by viewModel.biometricTrigger.collectAsState()
                val error by viewModel.error.collectAsState()

                // Trigger biometric prompt on request from VM or auto-trigger on launch if setup is complete
                LaunchedEffect(biometricTrigger) {
                    if (biometricTrigger) {
                        triggerBiometricAuthentication()
                        viewModel.triggerBiometricPrompt(false) // Reset trigger
                    }
                }

                // Auto-trigger biometric authentication when app starts if biometrics is enabled and setup is complete
                LaunchedEffect(Unit) {
                    if (securityManager.isSetupComplete() && securityManager.isBiometricsEnabled() && checkBiometricsAvailable()) {
                        triggerBiometricAuthentication()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Crossfade(targetState = isUnlocked, animationSpec = tween(500)) { unlocked ->
                        if (unlocked) {
                            DashboardScreen(
                                viewModel = viewModel,
                                onLockClicked = {
                                    viewModel.lockVault()
                                    Toast.makeText(applicationContext, "Vault Locked & Cleared from Memory", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            LockScreen(
                                isFirstTimeSetup = !viewModel.isSetupComplete,
                                onPinEntered = { pin ->
                                    if (viewModel.isSetupComplete) {
                                        val success = viewModel.enterPin(pin)
                                        if (success) {
                                            Toast.makeText(applicationContext, "Vault Access Authorized", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        viewModel.setupPin(pin)
                                        Toast.makeText(applicationContext, "Vault Master PIN Initialized", Toast.LENGTH_LONG).show()
                                    }
                                },
                                onBiometricClicked = {
                                    triggerBiometricAuthentication()
                                },
                                biometricsEnabled = securityManager.isBiometricsEnabled() && checkBiometricsAvailable(),
                                errorMessage = error,
                                onClearError = { viewModel.clearError() }
                            )
                        }
                    }

                    // Encrypted Photo Lightbox Viewer (overlay)
                    PhotoLightbox(viewModel = viewModel)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // High security protocol: Lock the vault automatically whenever the user navigates away or pauses the app!
        viewModel.lockVault()
    }

    /**
     * Checks if biometric hardware is present and user has enrolled biometric credentials.
     */
    private fun checkBiometricsAvailable(): Boolean {
        val biometricManager = BiometricManager.from(this)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Invokes Android native BiometricPrompt overlay to authenticate the user.
     */
    private fun triggerBiometricAuthentication() {
        if (!checkBiometricsAvailable()) {
            Toast.makeText(this, "Biometrics not available or configured on this device", Toast.LENGTH_LONG).show()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModel.unlockWithBiometricsOnSuccess()
                    Toast.makeText(applicationContext, "Biometric Authorization Approved", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // We only display errors if it's not a voluntary user cancellation
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        Toast.makeText(applicationContext, "Biometrics Error: $errString", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Biometric read failed. Try again.", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authorize Vault Access")
            .setSubtitle("Use biometric sensors to unlock Secure Gallery")
            .setNegativeButtonText("Use PIN")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to initialize biometric sensors", Toast.LENGTH_SHORT).show()
        }
    }
}
