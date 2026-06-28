package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VaultPhoto
import com.example.ui.VaultViewModel
import com.example.ui.theme.VaultAccentIndigo
import com.example.ui.theme.VaultDarkSurfaceVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun EncryptedImageThumbnail(
    photo: VaultPhoto,
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var decryptFailed by remember { mutableStateOf(false) }

    LaunchedEffect(photo) {
        isLoading = true
        decryptFailed = false
        withContext(Dispatchers.IO) {
            try {
                val bytes = viewModel.decryptPhotoBytes(photo)
                if (bytes != null) {
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        imageBitmap = bitmap.asImageBitmap()
                    } else {
                        decryptFailed = true
                    }
                } else {
                    decryptFailed = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                decryptFailed = true
            } finally {
                isLoading = false
            }
        }
    }

    // Helper for beautiful file size label
    val formattedSize = remember(photo.fileSize) {
        val kb = photo.fileSize / 1024.0
        if (kb > 1024.0) {
            String.format("%.1f MB", kb / 1024.0)
        } else {
            String.format("%.1f KB", kb)
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(VaultDarkSurfaceVariant)
            .clickable(enabled = !isLoading && !decryptFailed, onClick = onClick)
            .testTag("photo_thumbnail_${photo.id}"),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = Pair(isLoading, decryptFailed), animationSpec = tween(300)) { (loading, failed) ->
            when {
                loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = VaultAccentIndigo,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                failed -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Decryption Failed",
                            tint = Color.Red.copy(alpha = 0.6f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                imageBitmap != null -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = imageBitmap!!,
                            contentDescription = photo.originalName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Glassy lock badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Encrypted",
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }

                        // Size indicator on bottom
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = formattedSize,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
