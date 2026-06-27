package com.chesko.sendfiles.ui.screens

import androidx.compose.ui.graphics.vector.ImageVector

data class FileData(
    val name: String, 
    val size: String, 
    val icon: ImageVector, 
    val colorKey: String,
    val path: String,
    val isDirectory: Boolean = false,
    val lastModified: Long = 0L,
    val isApk: Boolean = false
)
