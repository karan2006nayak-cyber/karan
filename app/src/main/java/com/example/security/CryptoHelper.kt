package com.example.security

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoHelper {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val IV_LENGTH = 12 // 12 bytes is standard for AES-GCM
    private const val TAG_LENGTH = 128 // 128 bits is standard authentication tag length

    fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    fun deriveKey(pin: String, salt: ByteArray): SecretKeySpec {
        val keySpec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
        val tmp = factory.generateSecret(keySpec)
        return SecretKeySpec(tmp.encoded, ALGORITHM)
    }

    fun encrypt(data: ByteArray, secretKey: SecretKeySpec): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val random = SecureRandom()
        val iv = ByteArray(IV_LENGTH)
        random.nextBytes(iv)
        
        val parameterSpec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)
        
        val ciphertext = cipher.doFinal(data)
        
        // Pack IV and Ciphertext together: [IV (12 bytes)][Ciphertext]
        val result = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(ciphertext, 0, result, iv.size, ciphertext.size)
        return result
    }

    fun decrypt(encryptedData: ByteArray, secretKey: SecretKeySpec): ByteArray {
        if (encryptedData.size < IV_LENGTH) {
            throw IllegalArgumentException("Encrypted data is too short")
        }
        
        val iv = ByteArray(IV_LENGTH)
        System.arraycopy(encryptedData, 0, iv, 0, iv.size)
        
        val ciphertextLength = encryptedData.size - IV_LENGTH
        val ciphertext = ByteArray(ciphertextLength)
        System.arraycopy(encryptedData, iv.size, ciphertext, 0, ciphertextLength)
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)
        
        return cipher.doFinal(ciphertext)
    }
}
