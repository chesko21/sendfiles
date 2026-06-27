package com.chesko.sendfiles.ui

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chesko.sendfiles.data.AppDatabase
import com.chesko.sendfiles.data.TransferDirection
import com.chesko.sendfiles.data.TransferRecord
import com.chesko.sendfiles.data.TransferStatus
import com.chesko.sendfiles.network.Device
import com.chesko.sendfiles.network.TransferEngine
import com.chesko.sendfiles.ui.screens.FileData
import com.chesko.sendfiles.ui.screens.TransferItemData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val transferDao = database.transferDao()
    private val transferEngine = TransferEngine(application)

    private val _transferEvents = MutableSharedFlow<Unit>()
    val transferEvents: SharedFlow<Unit> = _transferEvents.asSharedFlow()

    val history: StateFlow<List<TransferRecord>> = transferDao.getAllTransfers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTransfers: StateFlow<List<TransferRecord>> = history.map { list ->
        list.filter { it.status == TransferStatus.IN_PROGRESS || it.status == TransferStatus.PENDING }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val progressMap = mutableStateMapOf<Long, Float>()
    
    private val _deviceFiles = MutableStateFlow<List<FileData>>(emptyList())
    val deviceFiles: StateFlow<List<FileData>> = _deviceFiles.asStateFlow()

    private val _selectedFiles = MutableStateFlow<Set<FileData>>(emptySet())
    val selectedFiles: StateFlow<Set<FileData>> = _selectedFiles.asStateFlow()

    private val _currentCategory = MutableStateFlow("All")
    val currentCategory: StateFlow<String> = _currentCategory.asStateFlow()

    private val _currentPath = MutableStateFlow(Environment.getExternalStorageDirectory().absolutePath)
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    enum class ViewMode { LIST, TILES, SMALL_ICONS, MEDIUM_ICONS, DETAILS }
    private val _viewMode = MutableStateFlow(ViewMode.DETAILS)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadDeviceFiles(path: String = _currentPath.value) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val directory = File(path)
            if (directory.exists() && directory.isDirectory) {
                _currentPath.value = path
                val files = directory.listFiles()?.map { file ->
                    FileData(
                        name = file.name,
                        size = if (file.isDirectory) "${file.listFiles()?.size ?: 0} items" else formatFileSize(file.length()),
                        icon = if (file.isDirectory) Icons.Rounded.Folder else getIconForFile(file.name),
                        colorKey = if (file.isDirectory) "primary" else getColorKeyForFile(file.name),
                        path = file.absolutePath,
                        isDirectory = file.isDirectory,
                        lastModified = file.lastModified(),
                        isApk = file.name.endsWith(".apk", ignoreCase = true)
                    )
                }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
                _deviceFiles.value = files
            }
            _isLoading.value = false
        }
    }

    fun navigateBack(): Boolean {
        if (_currentCategory.value != "All") {
            setCategory("All")
            return true
        }

        val currentFile = File(_currentPath.value)
        val parent = currentFile.parentFile
        val root = Environment.getExternalStorageDirectory()

        return if (parent != null && currentFile.absolutePath != root.absolutePath) {
            loadDeviceFiles(parent.absolutePath)
            true
        } else {
            false
        }
    }

    fun setCategory(category: String) {
        _currentCategory.value = category
        if (category == "All") {
            loadDeviceFiles(_currentPath.value)
        } else {
            loadFilesByCategory(category)
        }
    }

    private fun loadFilesByCategory(category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            when (category) {
                "Images" -> queryMediaStore(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                "Videos" -> queryMediaStore(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                "Music" -> queryMediaStore(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                "Apps" -> {
                    val installedApps = getInstalledAppsList()
                    val apkFiles = getRawApkFiles()
                    _deviceFiles.value = (apkFiles + installedApps).sortedBy { it.name.lowercase() }
                }
                "Documents" -> queryMediaStore(MediaStore.Files.getContentUri("external"), isDocuments = true)
            }
            _isLoading.value = false
        }
    }

    private fun getRawApkFiles(): List<FileData> {
        val files = mutableListOf<FileData>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )
        val selection = "${MediaStore.Files.FileColumns.DATA} LIKE ?"
        val selectionArgs = arrayOf("%.apk")

        getApplication<Application>().contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataCol) ?: continue
                val name = cursor.getString(nameCol) ?: File(path).name
                files.add(FileData(
                    name = name,
                    size = formatFileSize(cursor.getLong(sizeCol)),
                    icon = getIconForFile(name),
                    colorKey = getColorKeyForFile(name),
                    path = path,
                    isDirectory = false,
                    lastModified = cursor.getLong(dateCol) * 1000,
                    isApk = name.endsWith(".apk", ignoreCase = true)
                ))
            }
        }
        return files
    }

    private fun getInstalledAppsList(): List<FileData> {
        val apps = mutableListOf<FileData>()
        val pm = getApplication<Application>().packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (app in packages) {
            if (pm.getLaunchIntentForPackage(app.packageName) != null) {
                val file = File(app.sourceDir)
                apps.add(FileData(
                    name = pm.getApplicationLabel(app).toString(),
                    size = formatFileSize(file.length()),
                    icon = Icons.Rounded.SettingsApplications,
                    colorKey = "secondary",
                    path = app.sourceDir,
                    isDirectory = false,
                    lastModified = file.lastModified(),
                    isApk = true
                ))
            }
        }
        return apps
    }

    private fun queryMediaStore(uri: Uri, isDocuments: Boolean = false) {
        val files = mutableListOf<FileData>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        val selection = if (isDocuments) {
            val exts = listOf("pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx", "zip", "rar", "7z", "apk")
            exts.joinToString(" OR ") { "${MediaStore.Files.FileColumns.DATA} LIKE ?" }
        } else null

        val selectionArgs = if (isDocuments) {
            listOf("pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx", "zip", "rar", "7z", "apk")
                .map { "%.${it}" }.toTypedArray()
        } else null

        getApplication<Application>().contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataCol) ?: continue
                val name = cursor.getString(nameCol) ?: File(path).name
                files.add(FileData(
                    name = name,
                    size = formatFileSize(cursor.getLong(sizeCol)),
                    icon = getIconForFile(name),
                    colorKey = getColorKeyForFile(name),
                    path = path,
                    isDirectory = false,
                    lastModified = cursor.getLong(dateCol) * 1000,
                    isApk = name.endsWith(".apk", ignoreCase = true)
                ))
            }
        }
        _deviceFiles.value = files
    }

    val filteredFiles: StateFlow<List<FileData>> = combine(_deviceFiles, _currentCategory, _searchQuery) { files, category, query ->
       val baseFiles = if (category == "All") {
            files 
        } else {
            files.filter { file ->
                if (file.isDirectory) return@filter false
                val ext = file.name.substringAfterLast('.', "").lowercase()
                when (category) {
                    "Videos" -> ext in listOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm")
                    "Music" -> ext in listOf("mp3", "wav", "flac", "m4a", "ogg", "aac")
                    "Apps" -> ext == "apk"
                    "Images" -> ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
                    "Documents" -> ext in listOf("pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx", "zip", "rar", "7z", "apk")
                    else -> true
                }
            }
        }
        
        if (query.isBlank()) {
            baseFiles
        } else {
            baseFiles.filter { it.name.contains(query, ignoreCase = true) }
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFileSelection(file: FileData) {
        if (file.isDirectory) {
            loadDeviceFiles(file.path)
        } else {
            _selectedFiles.update { current ->
                if (current.contains(file)) current - file else current + file
            }
        }
    }

    fun clearSelection() {
        _selectedFiles.value = emptySet()
    }

    fun clearHistory() {
        viewModelScope.launch {
            transferDao.clearHistory()
        }
    }

    fun sendSelectedFiles(device: Device) {
        val filesToSend = _selectedFiles.value.toList()
        val urisToSend = pendingUris.toList()
        
        clearSelection()
        clearPendingUris()
        
        viewModelScope.launch {
            urisToSend.forEach { uri ->
                transferEngine.sendFile(device, uri) { progress ->
                    progressMap[0L] = progress
                }
            }

            filesToSend.forEach { fileData ->
                val file = File(fileData.path)
                if (file.exists()) {
                    val uri = getUriForFile(file)
                    transferEngine.sendFile(device, uri) { progress ->
                        progressMap[0L] = progress
                    }
                }
            }
        }
    }

    private fun getColorKeyForFile(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "mp4", "mkv", "avi", "mov" -> "primary"
            "jpg", "jpeg", "png", "webp", "gif" -> "secondary"
            "pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx" -> "tertiary"
            "zip", "rar", "7z" -> "primary"
            "apk" -> "secondary"
            else -> "outline"
        }
    }

    val recentTransfers: StateFlow<List<TransferItemData>> = history.map { list ->
        list.take(3).map { record ->
            val progress = progressMap[record.id] ?: if (record.status == TransferStatus.COMPLETED) 1f else 0f
            TransferItemData(
                title = record.fileName,
                subtitle = when (record.direction) {
                    TransferDirection.SEND -> "To ${record.receiverName}"
                    TransferDirection.RECEIVE -> "From ${record.senderName}"
                },
                progress = progress,
                detailLeft = formatFileSize(record.fileSize),
                detailRight = record.status.name,
                icon = getIconForFile(record.fileName),
                isCompleted = record.status == TransferStatus.COMPLETED
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(java.util.Locale.US, "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    private fun getIconForFile(fileName: String): androidx.compose.ui.graphics.vector.ImageVector {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "mp4", "mkv", "avi", "mov" -> Icons.Rounded.VideoFile
            "jpg", "jpeg", "png", "webp", "gif" -> Icons.Rounded.Image
            "pdf", "doc", "docx", "txt" -> Icons.AutoMirrored.Rounded.Article
            "xls", "xlsx" -> Icons.Rounded.Analytics
            "ppt", "pptx" -> Icons.Rounded.Slideshow
            "zip", "rar", "7z" -> Icons.Rounded.FolderZip
            "apk" -> Icons.Rounded.SettingsApplications
            else -> Icons.Rounded.FilePresent
        }
    }

    private val _pendingUris = mutableStateListOf<Uri>()
    val pendingUris: List<Uri> get() = _pendingUris
    
    fun setPendingUri(uri: Uri) {
        _pendingUris.clear()
        _pendingUris.add(uri)
    }

    fun setPendingUris(uris: List<Uri>) {
        _pendingUris.clear()
        _pendingUris.addAll(uris)
    }
    
    fun clearPendingUris() {
        _pendingUris.clear()
    }

    fun openFile(fileData: FileData) {
        val file = File(fileData.path)
        if (!file.exists()) return
        
        val uri = getUriForFile(file)
        val mimeType = getMimeType(file.absolutePath)
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
        }
    }

    fun installApk(fileData: FileData) {
        val file = File(fileData.path)
        if (!file.exists()) return

        val uri = getUriForFile(file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {

        }
    }

    fun shareFile(fileData: FileData) {
        val file = File(fileData.path)
        if (!file.exists()) return

        val uri = getUriForFile(file)
        val mimeType = getMimeType(file.absolutePath)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        val chooser = Intent.createChooser(intent, "Share File").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            getApplication<Application>().startActivity(chooser)
        } catch (e: Exception) {

        }
    }

    fun deleteFile(fileData: FileData) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(fileData.path)
            if (file.exists() && file.delete()) {
                loadDeviceFiles()
            }
        }
    }

    private fun getUriForFile(file: File): Uri {
        return try {
            FileProvider.getUriForFile(
                getApplication(),
                "${getApplication<Application>().packageName}.provider",
                file
            )
        } catch (e: Exception) {
            Uri.fromFile(file)
        }
    }

    private fun getMimeType(url: String): String? {
        val extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(File(url)).toString())
        return if (extension != null && extension.isNotEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
        } else {
            val manualExt = url.substringAfterLast('.', "").lowercase()
            if (manualExt.isNotEmpty()) {
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(manualExt) ?: "*/*"
            } else {
                "*/*"
            }
        }
    }

    init {
        loadDeviceFiles()
        viewModelScope.launch {
            transferEngine.startServer(8888) { progress ->
                viewModelScope.launch { _transferEvents.emit(Unit) }
            }
        }
    }

    fun sendFile(device: Device, uri: Uri) {
        viewModelScope.launch {
            transferEngine.sendFile(device, uri) { progress ->
            }
        }
    }
}
