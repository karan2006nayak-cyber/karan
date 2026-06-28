package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VaultFolder
import com.example.data.VaultPhoto
import com.example.ui.VaultViewModel
import com.example.ui.theme.VaultAccentCrimson
import com.example.ui.theme.VaultAccentGreen
import com.example.ui.theme.VaultAccentIndigo
import com.example.ui.theme.VaultDarkBg
import com.example.ui.theme.VaultDarkSurface
import com.example.ui.theme.VaultDarkSurfaceVariant
import com.example.ui.theme.VaultGlassBorder

@Composable
fun DashboardScreen(
    viewModel: VaultViewModel,
    onLockClicked: () -> Unit
) {
    val folders by viewModel.folders.collectAsState()
    val photosInFolder by viewModel.photosInSelectedFolder.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val context = LocalContext.current

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val currentFolder = remember(selectedFolderId, folders) {
        folders.find { it.id == selectedFolderId }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                val originalName = getFileNameFromUri(context, uri) ?: "photo.jpg"
                viewModel.importPhoto(uri, originalName)
            }
        }
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = VaultDarkBg,
        floatingActionButton = {
            if (selectedFolderId != null) {
                FloatingActionButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    containerColor = VaultAccentIndigo,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_photo_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Encrypted Photo")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "SECURE GALLERY",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Shield",
                            tint = VaultAccentGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AES-256 GCM Storage",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = VaultAccentGreen
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Settings Toggle
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .background(VaultDarkSurfaceVariant, CircleShape)
                            .size(40.dp)
                            .testTag("settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Secure Manual Lock
                    IconButton(
                        onClick = onLockClicked,
                        modifier = Modifier
                            .background(VaultAccentCrimson.copy(alpha = 0.2f), CircleShape)
                            .size(40.dp)
                            .testTag("lock_vault_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Vault",
                            tint = VaultAccentCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Error Display banner
            AnimatedVisibility(visible = error != null) {
                error?.let {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = VaultAccentCrimson.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Error Info",
                                tint = VaultAccentCrimson,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = it,
                                fontSize = 13.sp,
                                color = VaultAccentCrimson,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearError() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = VaultAccentCrimson,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Central Workspace
            Crossfade(targetState = selectedFolderId == null, modifier = Modifier.weight(1f)) { isRoot ->
                if (isRoot) {
                    // Folder list grid
                    FolderListView(
                        folders = folders,
                        viewModel = viewModel,
                        onCreateFolderClick = { showCreateFolderDialog = true },
                        onFolderSelect = { folderId -> viewModel.selectFolder(folderId) }
                    )
                } else {
                    // Photos grid in specific folder
                    PhotosGridView(
                        folder = currentFolder,
                        photos = photosInFolder,
                        viewModel = viewModel,
                        isLoading = isLoading,
                        onBack = { viewModel.selectFolder(null) }
                    )
                }
            }
        }
    }

    // Dialog for creating a folder
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onCreate = { name, colorHex ->
                viewModel.createFolder(name, colorHex)
                showCreateFolderDialog = false
            }
        )
    }

    // Dialog for settings
    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
fun FolderListView(
    folders: List<VaultFolder>,
    viewModel: VaultViewModel,
    onCreateFolderClick: () -> Unit,
    onFolderSelect: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Secure Directories",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            TextButton(
                onClick = onCreateFolderClick,
                colors = ButtonDefaults.textButtonColors(contentColor = VaultAccentIndigo),
                modifier = Modifier.testTag("create_folder_btn")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Icon", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "New Folder", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (folders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Empty Folders",
                        tint = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Directories Yet",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Create a secure folder to organize and encrypt your visual memories safely.",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(folders) { folder ->
                    var showDeleteConfirm by remember { mutableStateOf(false) }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = VaultDarkSurface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, VaultGlassBorder, RoundedCornerShape(16.dp))
                            .clickable { onFolderSelect(folder.id) }
                            .testTag("folder_card_${folder.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(Color(android.graphics.Color.parseColor(folder.colorHex)).copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = "Folder Icon",
                                        tint = Color(android.graphics.Color.parseColor(folder.colorHex)),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { showDeleteConfirm = true },
                                    modifier = Modifier.size(24.dp).testTag("delete_folder_btn_${folder.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Folder",
                                        tint = VaultAccentCrimson.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = folder.name,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = "Secure Directory",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            containerColor = VaultDarkSurface,
                            title = {
                                Text(
                                    text = "Delete Directory?",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Text(
                                    text = "This will permanently delete '${folder.name}' and shred ALL encrypted files inside. This action cannot be undone.",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        viewModel.deleteFolder(folder)
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
    }
}

@Composable
fun PhotosGridView(
    folder: VaultFolder?,
    photos: List<VaultPhoto>,
    viewModel: VaultViewModel,
    isLoading: Boolean,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Back Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(VaultDarkSurface, CircleShape)
                    .size(36.dp)
                    .testTag("back_to_folders_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = folder?.name ?: "Vault Items",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${photos.size} encrypted items",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isLoading && photos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = VaultAccentIndigo)
            }
        } else if (photos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Empty Folder Photos",
                        tint = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Folder is Empty",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Click the '+' FAB at the bottom to import and securely encrypt photos from your device library.",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(photos) { photo ->
                    EncryptedImageThumbnail(
                        photo = photo,
                        viewModel = viewModel,
                        onClick = { viewModel.showPhotoPreview(photo) }
                    )
                }
            }
        }
    }
}

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    val colors = listOf("#4F46E5", "#10B981", "#EC4899", "#3B82F6", "#F59E0B", "#EF4444")
    var selectedColorHex by remember { mutableStateOf(colors[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultDarkSurface,
        title = {
            Text(
                text = "Secure Directory",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Name your folder to categorize private memories.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    placeholder = { Text("e.g. Finances, Family, Private") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = VaultAccentIndigo,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                        unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("folder_name_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Personalized Theme Accent",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (color in colors) {
                        val active = color == selectedColorHex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(color)))
                                .border(
                                    width = if (active) 3.dp else 0.dp,
                                    color = if (active) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = color }
                                .testTag("color_accent_$color")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (folderName.isNotBlank()) {
                        onCreate(folderName.trim(), selectedColorHex)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VaultAccentIndigo),
                enabled = folderName.isNotBlank(),
                modifier = Modifier.testTag("submit_create_folder_btn")
            ) {
                Text("CREATE DIRECTORY", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("CANCEL")
            }
        }
    )
}

@Composable
fun SettingsDialog(
    viewModel: VaultViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var biometricsChecked by remember { mutableStateOf(viewModel.isBiometricsEnabled) }
    var linkPinMode by remember { mutableStateOf(false) }
    var inputPinForBio by remember { mutableStateOf("") }
    var linkErrorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultDarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = VaultAccentIndigo,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Vault Controls",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                if (!linkPinMode) {
                    // Biometrics switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Biometric Unlock",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Unlock the vault with your face or fingerprint scanner securely.",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Switch(
                            checked = biometricsChecked,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    linkPinMode = true
                                    inputPinForBio = ""
                                    linkErrorMessage = null
                                } else {
                                    viewModel.toggleBiometrics(false, context)
                                    biometricsChecked = false
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = VaultAccentGreen,
                                checkedTrackColor = VaultAccentGreen.copy(alpha = 0.3f),
                                uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.testTag("biometric_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Technical security specs
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = VaultDarkBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, VaultGlassBorder, RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "CRYPTOGRAPHIC PROTOCOL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = VaultAccentIndigo,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SecuritySpecItem(title = "Encryption Standard", value = "AES-GCM 256-Bit")
                            SecuritySpecItem(title = "Key Derivation", value = "PBKDF2-HMAC-SHA256")
                            SecuritySpecItem(title = "Hash Iterations", value = "10,000 Rounds")
                            SecuritySpecItem(title = "Local Isolation", value = "App Sandbox (Secure)")
                        }
                    }
                } else {
                    // LINK PIN SCREEN FOR BIOMETRICS
                    Column {
                        Text(
                            text = "Link Biometrics",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "To securely link your device biometrics, please enter your Master PIN for validation.",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = inputPinForBio,
                            onValueChange = { inputPinForBio = it },
                            placeholder = { Text("4-Digit PIN") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = VaultAccentIndigo,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("link_pin_input")
                        )

                        if (linkErrorMessage != null) {
                            Text(
                                text = linkErrorMessage!!,
                                color = VaultAccentCrimson,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (linkPinMode) {
                Button(
                    onClick = {
                        val verified = viewModel.linkBiometricsWithPin(inputPinForBio)
                        if (verified) {
                            biometricsChecked = true
                            linkPinMode = false
                        } else {
                            linkErrorMessage = "Invalid PIN. Linking failed."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultAccentIndigo),
                    modifier = Modifier.testTag("submit_link_pin_btn")
                ) {
                    Text("VERIFY & LINK", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = VaultDarkSurfaceVariant),
                    modifier = Modifier.testTag("close_settings_btn")
                ) {
                    Text("CLOSE")
                }
            }
        },
        dismissButton = {
            if (linkPinMode) {
                TextButton(
                    onClick = { linkPinMode = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Text("CANCEL")
                }
            }
        }
    )
}

@Composable
fun SecuritySpecItem(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

// Get file name from Uri
fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}
