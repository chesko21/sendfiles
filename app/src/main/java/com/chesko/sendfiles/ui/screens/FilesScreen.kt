package com.chesko.sendfiles.ui.screens

import android.annotation.SuppressLint
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.ViewQuilt
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import com.chesko.sendfiles.R
import com.chesko.sendfiles.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    onNext: () -> Unit,
    onBack: (() -> Unit)? = null,
    onRequestStoragePermission: () -> Unit,
    viewModel: MainViewModel = viewModel(),
) {
    val files by viewModel.filteredFiles.collectAsStateWithLifecycle()
    val selectedFiles by viewModel.selectedFiles.collectAsStateWithLifecycle()
    val currentCategory by viewModel.currentCategory.collectAsStateWithLifecycle()
    val currentPath by viewModel.currentPath.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showViewMenu by remember { mutableStateOf(value = false) }
    var selectedFileForMenu by remember { mutableStateOf<FileData?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val isTv = remember { context.packageManager.hasSystemFeature("android.software.leanback") }
    
    val backRequester = remember { FocusRequester() }
    val searchRequester = remember { FocusRequester() }
    val firstCategoryRequester = remember { FocusRequester() }
    val contentRequester = remember { FocusRequester() }
    val refreshRequester = remember { FocusRequester() }
    
    val rootPath = remember { Environment.getExternalStorageDirectory().absolutePath }
    val isRoot = currentPath == rootPath
    
    var isSearchActive by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = true) {
        if (isSearchActive) {
            isSearchActive = false
            focusManager.clearFocus()
        } else if (!viewModel.navigateBack()) {
            onBack?.invoke()
        }
    }

    LaunchedEffect(currentPath) {
        if (isTv && !isRoot) {
            try { backRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    LaunchedEffect(Unit) {
        onRequestStoragePermission()
        if (currentCategory == "All") {
            viewModel.loadDeviceFiles()
        } else {
            viewModel.setCategory(currentCategory)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isFocused by interactionSource.collectIsFocusedAsState()
                    
                    Surface(
                        onClick = onBack,
                        shape = CircleShape,
                        color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                        interactionSource = interactionSource,
                        modifier = Modifier.size(48.dp).scale(if (isFocused) 1.2f else 1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack, 
                                contentDescription = "Back",
                                tint = if (isFocused) Color.White else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (currentCategory == "All") "Internal Storage" else currentCategory,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (currentCategory == "All") {
                            if (isRoot) "Root" else currentPath.substringAfterLast('/')
                        } else {
                            "Semua ${currentCategory.lowercase()}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            val searchInteractionSource = remember { MutableInteractionSource() }
            val isSearchFocused by searchInteractionSource.collectIsFocusedAsState()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    onClick = { isSearchActive = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .scale(if (isSearchFocused) 1.02f else 1f)
                        .focusRequester(searchRequester)
                        .onFocusChanged { if (!it.isFocused) isSearchActive = false }
                        .focusProperties { 
                            down = firstCategoryRequester 
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSearchFocused) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(
                        if (isSearchFocused) 2.dp else 1.dp, 
                        if (isSearchFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { if (isSearchActive) viewModel.setSearchQuery(it) },
                        readOnly = !isSearchActive,
                        modifier = Modifier.fillMaxSize(),
                        interactionSource = searchInteractionSource,
                        placeholder = { 
                            Text(
                                text = stringResource(R.string.search_hint), 
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Rounded.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                }

                Box {
                    val viewInteractionSource = remember { MutableInteractionSource() }
                    val isViewFocused by viewInteractionSource.collectIsFocusedAsState()
                    
                    Surface(
                        onClick = { showViewMenu = true },
                        interactionSource = viewInteractionSource,
                        shape = CircleShape,
                        color = if (isViewFocused) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        border = if (isViewFocused) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .size(48.dp)
                            .scale(if (isViewFocused) 1.1f else 1f)
                            .focusProperties { 
                                up = searchRequester
                                down = firstCategoryRequester 
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                when(viewMode) {
                                    MainViewModel.ViewMode.LIST -> Icons.AutoMirrored.Rounded.List
                                    MainViewModel.ViewMode.DETAILS -> Icons.Rounded.ViewHeadline
                                    MainViewModel.ViewMode.SMALL_ICONS -> Icons.Rounded.GridView
                                    MainViewModel.ViewMode.MEDIUM_ICONS -> Icons.Rounded.Apps
                                    MainViewModel.ViewMode.TILES -> Icons.AutoMirrored.Rounded.ViewQuilt
                                },
                                contentDescription = "View Mode",
                                tint = if (isViewFocused) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showViewMenu,
                        onDismissRequest = { showViewMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("List") },
                            onClick = { viewModel.setViewMode(MainViewModel.ViewMode.LIST); showViewMenu = false },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.List, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Details") },
                            onClick = { viewModel.setViewMode(MainViewModel.ViewMode.DETAILS); showViewMenu = false },
                            leadingIcon = { Icon(Icons.Rounded.ViewHeadline, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Small Icons") },
                            onClick = { viewModel.setViewMode(MainViewModel.ViewMode.SMALL_ICONS); showViewMenu = false },
                            leadingIcon = { Icon(Icons.Rounded.GridView, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Medium Icons") },
                            onClick = { viewModel.setViewMode(MainViewModel.ViewMode.MEDIUM_ICONS); showViewMenu = false },
                            leadingIcon = { Icon(Icons.Rounded.Apps, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Tiles") },
                            onClick = { viewModel.setViewMode(MainViewModel.ViewMode.TILES); showViewMenu = false },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.ViewQuilt, null) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf("All", "Images", "Videos", "Apps", "Music", "Documents")
                categories.forEachIndexed { index, category ->
                    CategoryTab(
                        label = category, 
                        isSelected = currentCategory == category,
                        modifier = Modifier
                            .then(if (index == 0) Modifier.focusRequester(firstCategoryRequester) else Modifier)
                            .focusProperties {
                                up = searchRequester
                                down = if (currentCategory == "All" && (!isRoot)) backRequester else contentRequester
                            },
                        onClick = { 
                            viewModel.setCategory(category)
                            if (category == "All") {
                                viewModel.loadDeviceFiles()
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            if (currentCategory == "All" && !isRoot) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val backInteractionSource = remember { MutableInteractionSource() }
                    val isBackFocused by backInteractionSource.collectIsFocusedAsState()
                    
                    Surface(
                        onClick = { viewModel.navigateBack() },
                        interactionSource = backInteractionSource,
                        shape = RoundedCornerShape(12.dp),
                        color = if (isBackFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier
                            .height(36.dp)
                            .scale(if (isBackFocused) 1.1f else 1f)
                            .focusRequester(backRequester)
                            .focusProperties {
                                up = firstCategoryRequester
                                down = contentRequester
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.ArrowUpward,
                                contentDescription = "Up",
                                tint = if (isBackFocused) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Go back",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isBackFocused) Color.White else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier
                .weight(1f)
                .focusRequester(contentRequester)
                .focusProperties {
                    up = if (currentCategory == "All" && !isRoot) backRequester else firstCategoryRequester
                }
            ) {
                if (files.isEmpty() && !isLoading) {
                    @SuppressLint("NewApi")
                    val hasAllFilesAccess = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        Environment.isExternalStorageManager()
                    } else true

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val actionInteractionSource = remember { MutableInteractionSource() }
                        val isActionFocused by actionInteractionSource.collectIsFocusedAsState()

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (hasAllFilesAccess) Icons.Rounded.FolderOpen else Icons.Rounded.GppMaybe, 
                                contentDescription = null, 
                                modifier = Modifier.size(64.dp), 
                                tint = if (hasAllFilesAccess) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                if (hasAllFilesAccess) "No files found" else "Izin Akses File Diperlukan", 
                                color = if (hasAllFilesAccess) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            if (!hasAllFilesAccess) {
                                Text(
                                    "Aplikasi butuh izin ini untuk mengirim file APK dan folder",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Button(
                                onClick = { 
                                    if (hasAllFilesAccess) viewModel.loadDeviceFiles() 
                                    else onRequestStoragePermission()
                                },
                                interactionSource = actionInteractionSource,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isActionFocused) MaterialTheme.colorScheme.primary 
                                                    else MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = if (isActionFocused) MaterialTheme.colorScheme.onPrimary 
                                                   else MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                                    .scale(if (isActionFocused) 1.1f else 1f)
                                    .height(52.dp)
                                    .padding(horizontal = 16.dp)
                                    .focusRequester(refreshRequester)
                                    .focusProperties {
                                        up = if (currentCategory == "All" && !isRoot) backRequester else firstCategoryRequester
                                    }
                                    .border(
                                        if (isActionFocused) 2.dp else 0.dp,
                                        MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(100)
                                    ),
                                shape = RoundedCornerShape(100)
                            ) {
                                Icon(
                                    if (hasAllFilesAccess) Icons.Rounded.Refresh else Icons.Rounded.Settings, 
                                    null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (hasAllFilesAccess) "Refresh" else "Berikan Izin Akses File",
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                } else {
                    FileContent(
                        viewMode = viewMode,
                        files = files,
                        selectedFiles = selectedFiles,
                        onToggleSelection = { viewModel.toggleFileSelection(it) },
                        onLongClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedFileForMenu = it 
                        }
                    )
                }
            }

            if (isTv && selectedFiles.isNotEmpty()) {
                val totalCount = selectedFiles.size + viewModel.pendingUris.size
                val nextInteractionSource = remember { MutableInteractionSource() }
                val isNextFocused by nextInteractionSource.collectIsFocusedAsState()

                Button(
                    onClick = onNext,
                    interactionSource = nextInteractionSource,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isNextFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .scale(if (isNextFocused) 1.05f else 1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Lanjut ($totalCount file)")
                }
            }
        }

        if (selectedFileForMenu != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedFileForMenu = null },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                FileActionMenu(
                    file = selectedFileForMenu!!,
                    onAction = { action ->
                        when(action) {
                            FileAction.OPEN -> viewModel.openFile(selectedFileForMenu!!)
                            FileAction.INSTALL -> viewModel.installApk(selectedFileForMenu!!)
                            FileAction.SHARE -> viewModel.shareFile(selectedFileForMenu!!)
                            FileAction.DELETE -> viewModel.deleteFile(selectedFileForMenu!!)
                        }
                        selectedFileForMenu = null
                    }
                )
            }
        }

        val totalCount = selectedFiles.size + viewModel.pendingUris.size
        if (!isTv && totalCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                val nextInteractionSource = remember { MutableInteractionSource() }
                val isNextFocused by nextInteractionSource.collectIsFocusedAsState()

                FloatingActionButton(
                    onClick = onNext,
                    interactionSource = nextInteractionSource,
                    containerColor = if (isNextFocused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    contentColor = if (isNextFocused) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp).scale(if (isNextFocused) 1.2f else 1f)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Next")
                }
                Surface(
                    color = MaterialTheme.colorScheme.error,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = totalCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FileContent(
    viewMode: MainViewModel.ViewMode,
    files: List<FileData>,
    selectedFiles: Set<FileData>,
    onToggleSelection: (FileData) -> Unit,
    onLongClick: (FileData) -> Unit
) {
    when (viewMode) {
        MainViewModel.ViewMode.LIST -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                lazyItems(files, key = { it.path }) { file ->
                    FileListItemCompact(
                        file = file,
                        isSelected = selectedFiles.contains(file),
                        onClick = { onToggleSelection(file) },
                        onLongClick = { onLongClick(file) }
                    )
                }
            }
        }
        MainViewModel.ViewMode.DETAILS -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                lazyItems(files, key = { it.path }) { file ->
                    FileListItem(
                        file = file,
                        isSelected = selectedFiles.contains(file),
                        onClick = { onToggleSelection(file) },
                        onLongClick = { onLongClick(file) }
                    )
                }
            }
        }
        MainViewModel.ViewMode.SMALL_ICONS -> {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                gridItems(files, key = { it.path }) { file ->
                    FileGridItemSmall(
                        file = file,
                        isSelected = selectedFiles.contains(file),
                        onClick = { onToggleSelection(file) },
                        onLongClick = { onLongClick(file) }
                    )
                }
            }
        }
        MainViewModel.ViewMode.MEDIUM_ICONS -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                gridItems(files, key = { it.path }) { file ->
                    FileGridItem(
                        file = file,
                        isSelected = selectedFiles.contains(file),
                        onClick = { onToggleSelection(file) },
                        onLongClick = { onLongClick(file) }
                    )
                }
            }
        }
        MainViewModel.ViewMode.TILES -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                gridItems(files, key = { it.path }) { file ->
                    FileTileItem(
                        file = file,
                        isSelected = selectedFiles.contains(file),
                        onClick = { onToggleSelection(file) },
                        onLongClick = { onLongClick(file) }
                    )
                }
            }
        }
    }
}

@Composable
fun FileIcon(file: FileData, modifier: Modifier = Modifier, isSelected: Boolean = false) {
    val context = LocalContext.current
    val iconColor = when(file.colorKey) {
        "primary" -> MaterialTheme.colorScheme.primary
        "secondary" -> MaterialTheme.colorScheme.secondary
        "tertiary" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    if (file.isApk) {
        var apkIcon by remember(file.path) { mutableStateOf<android.graphics.drawable.Drawable?>(null) }
        
        LaunchedEffect(file.path) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val pm = context.packageManager
                    val info = pm.getPackageArchiveInfo(file.path, 0)
                    info?.applicationInfo?.let { appInfo ->
                        appInfo.sourceDir = file.path
                        appInfo.publicSourceDir = file.path
                        apkIcon = pm.getApplicationIcon(appInfo)
                    }
                } catch (_: Exception) {
                }
            }
        }
        
        if (apkIcon != null) {
            AsyncImage(
                model = apkIcon,
                contentDescription = null,
                modifier = modifier.clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
        } else {
            Icon(
                imageVector = file.icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else iconColor,
                modifier = modifier
            )
        }
    } else {
        Icon(
            imageVector = if (file.isDirectory) Icons.Rounded.Folder else file.icon,
            contentDescription = null,
            tint = if (file.isDirectory) MaterialTheme.colorScheme.primary 
                    else if (isSelected) MaterialTheme.colorScheme.primary 
                    else iconColor,
            modifier = modifier
        )
    }
}

@Composable
fun FileListItemCompact(file: FileData, isSelected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Surface(
        color = when {
            isFocused -> MaterialTheme.colorScheme.primaryContainer
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else -> Color.Transparent
        },
        shape = RoundedCornerShape(8.dp),
        border = if (isFocused) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                interactionSource = interactionSource,
                indication = null
            )
            .scale(if (isFocused) 1.02f else 1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileIcon(
                file = file,
                modifier = Modifier.size(24.dp),
                isSelected = isSelected || isFocused
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                file.name, 
                style = MaterialTheme.typography.bodyMedium, 
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                color = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            if (isSelected) {
                Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun FileGridItemSmall(file: FileData, isSelected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                interactionSource = interactionSource,
                indication = null
            )
            .scale(if (isFocused) 1.05f else 1f)
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when {
                        isFocused -> MaterialTheme.colorScheme.primaryContainer
                        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else -> Color.Transparent
                    }
                )
                .border(
                    if (isFocused) 2.dp else 1.dp, 
                    if (isFocused) MaterialTheme.colorScheme.primary 
                    else if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else Color.Transparent, 
                    RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
                .height(100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                FileIcon(file = file, modifier = Modifier.size(40.dp), isSelected = isSelected || isFocused)
            }
            Text(
                file.name,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp,
                color = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun FileTileItem(file: FileData, isSelected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when {
            isFocused -> MaterialTheme.colorScheme.primaryContainer
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            if (isFocused) 3.dp else 1.dp, 
            if (isFocused) MaterialTheme.colorScheme.primary 
            else if (isSelected) MaterialTheme.colorScheme.primary 
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                interactionSource = interactionSource,
                indication = null
            )
            .scale(if (isFocused) 1.02f else 1f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    FileIcon(file = file, modifier = Modifier.size(28.dp), isSelected = isSelected || isFocused)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.name, 
                    style = MaterialTheme.typography.bodyLarge, 
                    fontWeight = FontWeight.Bold, 
                    maxLines = 1,
                    color = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text("${file.size} • ${file.path.substringAfterLast('.')}", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun CategoryTab(
    label: String, 
    isSelected: Boolean, 
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        color = when {
            isFocused -> MaterialTheme.colorScheme.primaryContainer
            isSelected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        border = if (isFocused) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier
            .height(40.dp)
            .scale(if (isFocused) 1.05f else 1f)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isFocused -> MaterialTheme.colorScheme.onPrimaryContainer
                    isSelected -> Color.White
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
fun FileListItem(file: FileData, isSelected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val dateString = remember(file.lastModified) { dateFormat.format(Date(file.lastModified)) }

    Surface(
        color = when {
            isFocused -> MaterialTheme.colorScheme.primaryContainer
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else -> Color.Transparent
        },
        shape = RoundedCornerShape(12.dp),
        border = if (isFocused) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                interactionSource = interactionSource,
                indication = null
            )
            .scale(if (isFocused) 1.02f else 1f)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        when {
                            isFocused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                val isImage = remember(file.name) {
                    val ext = file.name.substringAfterLast('.', "").lowercase()
                    ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
                }
                val isVideo = remember(file.name) {
                    val ext = file.name.substringAfterLast('.', "").lowercase()
                    ext in listOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm")
                }
                
                if (isImage || isVideo) {
                    Box(contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = file.path,
                            contentDescription = file.name,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        if (isVideo) {
                            Icon(
                                Icons.Rounded.PlayCircle,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                } else {
                    FileIcon(file = file, modifier = Modifier.size(28.dp), isSelected = isSelected || isFocused)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name, 
                    style = MaterialTheme.typography.bodyLarge, 
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateString, 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(3.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = file.size, 
                        style = MaterialTheme.typography.labelSmall, 
                        color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

enum class FileAction { OPEN, INSTALL, SHARE, DELETE }

@Composable
fun FileActionMenu(file: FileData, onAction: (FileAction) -> Unit) {
    val isApk = remember(file.name) { file.name.endsWith(".apk", ignoreCase = true) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    ) {
        Text(
            text = file.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isApk) {
                CompactActionItem(
                    icon = Icons.Rounded.InstallMobile,
                    label = "Install",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onAction(FileAction.INSTALL) }
                )
            } else {
                CompactActionItem(
                    icon = Icons.AutoMirrored.Rounded.OpenInNew,
                    label = "Open",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onAction(FileAction.OPEN) }
                )
            }
            
            CompactActionItem(
                icon = Icons.Rounded.Share,
                label = "Share",
                color = MaterialTheme.colorScheme.secondary,
                onClick = { onAction(FileAction.SHARE) }
            )
            
            CompactActionItem(
                icon = Icons.Rounded.Delete,
                label = "Delete",
                color = MaterialTheme.colorScheme.error,
                onClick = { onAction(FileAction.DELETE) }
            )
        }
    }
}

@Composable
fun CompactActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null
            )
            .background(if (isFocused) color.copy(alpha = 0.15f) else Color.Transparent)
            .border(if (isFocused) 2.dp else 0.dp, color, RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 16.dp)
            .scale(if (isFocused) 1.1f else 1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = if (isFocused) color else color.copy(alpha = 0.12f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon, 
                    contentDescription = null, 
                    tint = if (isFocused) Color.White else color, 
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isFocused) color else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FileGridItem(file: FileData, isSelected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val isImage = remember(file.name) {
        val ext = file.name.substringAfterLast('.', "").lowercase()
        ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
    }
    val isVideo = remember(file.name) {
        val ext = file.name.substringAfterLast('.', "").lowercase()
        ext in listOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                interactionSource = interactionSource,
                indication = null
            )
            .scale(if (isFocused) 1.05f else 1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isFocused -> MaterialTheme.colorScheme.primaryContainer
                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                else -> MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        border = BorderStroke(
            width = if (isFocused) 3.dp else 1.dp, 
            color = if (isFocused) MaterialTheme.colorScheme.primary 
                    else if (isSelected) MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isImage || isVideo) {
                AsyncImage(
                    model = file.path,
                    contentDescription = file.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            when {
                                isFocused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else -> Color.Black.copy(alpha = 0.1f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVideo) {
                        Icon(
                            Icons.Rounded.PlayCircle,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                if (!file.isDirectory) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.TopEnd)
                            .background(
                                if (isSelected || isFocused) MaterialTheme.colorScheme.primary 
                                else if (isImage) Color.Black.copy(alpha = 0.4f)
                                else Color.Transparent, 
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                1.5.dp, 
                                if (isSelected || isFocused) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.outline, 
                                RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected || isFocused) {
                            Icon(
                                if (isSelected) Icons.Rounded.Check else Icons.Rounded.AdsClick, 
                                contentDescription = null, 
                                modifier = Modifier.size(14.dp), 
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (!isImage) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isFocused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                FileIcon(file = file, modifier = Modifier.size(32.dp), isSelected = isSelected || isFocused)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    
                    Surface(
                        color = if (isImage) Color.Black.copy(alpha = 0.6f) else Color.Transparent,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            file.name, 
                            style = MaterialTheme.typography.bodyMedium, 
                            fontWeight = FontWeight.Bold, 
                            maxLines = 1, 
                            overflow = TextOverflow.Ellipsis,
                            color = if (isImage || isFocused) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
