package com.example.ui.components

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.VaultViewModel
import com.example.ui.theme.VaultAccentCrimson
import com.example.ui.theme.VaultAccentGreen
import com.example.ui.theme.VaultAccentIndigo
import com.example.ui.theme.VaultDarkBg
import com.example.ui.theme.VaultDarkSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PhotoLightbox(
    viewModel: VaultViewModel
) {
    val photo by viewModel.previewPhoto.collectAsState()
    val decryptedBytes by viewModel.decryptedPreviewBytes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = photo != null,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        photo?.let { currentPhoto ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Decrypted Image Render
                if (decryptedBytes != null) {
                    val bitmap = remember(decryptedBytes) {
                        BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes!!.size)
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = currentPhoto.originalName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("lightbox_image")
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Failed to render decrypted photo.",
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = VaultAccentIndigo)
                    }
                }

                // Top Controls Overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = { viewModel.closePhotoPreview() },
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.1f), CircleShape)
                                .size(40.dp)
                                .testTag("lightbox_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Close Preview",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = currentPhoto.originalName,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Decrypted in memory safely",
                                color = VaultAccentGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Unhide / Export Button
                        IconButton(
                            onClick = {
                                if (decryptedBytes != null && !isExporting) {
                                    isExporting = true
                                    scope.launch {
                                        val success = withContext(Dispatchers.IO) {
                                            saveBytesToPublicPictures(
                                                context = context,
                                                bytes = decryptedBytes!!,
                                                displayName = "Decrypted_" + currentPhoto.originalName
                                            )
                                        }
                                        isExporting = false
                                        if (success) {
                                            Toast.makeText(context, "Photo decrypted & exported to Pictures!", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Export failed. Please try again.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .background(VaultAccentGreen.copy(alpha = 0.2f), CircleShape)
                                .size(40.dp)
                                .testTag("lightbox_unhide_btn")
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(color = VaultAccentGreen, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = "Decrypt & Export",
                                    tint = VaultAccentGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Delete Permanently Button
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier
                                .background(VaultAccentCrimson.copy(alpha = 0.2f), CircleShape)
                                .size(40.dp)
                                .testTag("lightbox_delete_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = VaultAccentCrimson,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    containerColor = VaultDarkSurface,
                    title = {
                        Text(
                            text = "Shred Photo?",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = "This will permanently shred and delete this encrypted file from storage. This action is secure and cannot be reversed.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deletePhoto(currentPhoto)
                                showDeleteConfirm = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = VaultAccentCrimson)
                        ) {
                            Text("SHRED & DELETE", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteConfirm = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Text("CANCEL")
                        }
                    }
                )
            }
        }
    }
}

// Scoped Storage compliant direct MediaStore export helper
fun saveBytesToPublicPictures(context: Context, bytes: ByteArray, displayName: String): Boolean {
    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DecryptedGallery")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }

    val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return false
    return try {
        resolver.openOutputStream(imageUri)?.use { out ->
            out.write(bytes)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(imageUri, contentValues, null, null)
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        resolver.delete(imageUri, null, null)
        false
    }
}
