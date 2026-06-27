package com.chesko.sendfiles.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.chesko.sendfiles.R
import com.chesko.sendfiles.network.NsdHelper
import com.chesko.sendfiles.ui.screens.DevicesScreen
import com.chesko.sendfiles.ui.screens.FilesScreen
import com.chesko.sendfiles.ui.screens.HistoryScreen
import com.chesko.sendfiles.ui.screens.HomeScreen
import com.chesko.sendfiles.ui.screens.TransfersScreen
import com.chesko.sendfiles.ui.screens.TvHomeScreen
import com.chesko.sendfiles.util.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    nsdHelper: NsdHelper,
    onRequestStoragePermission: () -> Unit = {}
) {
    val context = LocalContext.current
    val isTv = remember {
        context.packageManager.hasSystemFeature("android.software.leanback")
    }

    if (isTv) {
        val viewModel: MainViewModel = viewModel()
        val pickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: android.net.Uri? ->
            uri?.let {
                viewModel.setPendingUri(it)
            }
        }
        TvHomeScreen(
            nsdHelper = nsdHelper,
            onSendClick = { pickerLauncher.launch(Constants.MIME_TYPE_ALL) }
        )
    } else {
        MobileApp(nsdHelper, onRequestStoragePermission)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileApp(
    nsdHelper: NsdHelper,
    onRequestStoragePermission: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel()
    val backStack = rememberNavBackStack(Route.Home)
    val currentRoute = backStack.lastOrNull() ?: Route.Home

    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
        } else {
            showExitDialog = true
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            confirmButton = {
                TextButton(onClick = { (context as? Activity)?.finish() }) {
                    Text("Keluar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            text = {
                Text(
                    "Keluar dari SendFiles?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(28.dp)
        )
    }

    LaunchedEffect(viewModel.pendingUris) {
        if (viewModel.pendingUris.isNotEmpty()) {
            if (currentRoute !is Route.Devices) {
                backStack.add(Route.Devices)
            }
        }
    }

    @Suppress("DEPRECATION")
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val navSuiteType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)

    NavigationSuiteScaffold(
        layoutType = navSuiteType,
        containerColor = MaterialTheme.colorScheme.background,
        navigationSuiteItems = {
            item(
                selected = currentRoute is Route.Home,
                onClick = {
                    if (currentRoute !is Route.Home) {
                        backStack.add(Route.Home)
                        while (backStack.size > 1) backStack.removeAt(0)
                    }
                },
                icon = { Icon(Icons.Rounded.Home, contentDescription = stringResource(R.string.nav_home)) },
                label = { Text(stringResource(R.string.nav_home)) }
            )
            item(
                selected = currentRoute is Route.Files,
                onClick = {
                    if (currentRoute !is Route.Files) {
                        backStack.add(Route.Files)
                        while (backStack.size > 1) backStack.removeAt(0)
                    }
                },
                icon = { Icon(Icons.Rounded.FolderOpen, contentDescription = stringResource(R.string.nav_files)) },
                label = { Text(stringResource(R.string.nav_files)) }
            )
            item(
                selected = currentRoute is Route.Transfers,
                onClick = {
                    if (currentRoute !is Route.Transfers) {
                        backStack.add(Route.Transfers)
                        while (backStack.size > 1) backStack.removeAt(0)
                    }
                },
                icon = { Icon(Icons.Rounded.SwapHorizontalCircle, contentDescription = stringResource(R.string.nav_transfer)) },
                label = { Text(stringResource(R.string.nav_transfer)) }
            )
            item(
                selected = currentRoute is Route.Devices,
                onClick = {
                    if (currentRoute !is Route.Devices) {
                        backStack.add(Route.Devices)
                        while (backStack.size > 1) backStack.removeAt(0)
                    }
                },
                icon = { Icon(Icons.Rounded.Devices, contentDescription = stringResource(R.string.nav_devices)) },
                label = { Text(stringResource(R.string.nav_devices)) }
            )
        }
    ) {
        Scaffold { padding ->
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.padding(padding).fillMaxSize(),
                onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }
            ) { key ->
                when (key) {
                    is Route.Home -> {
                        NavEntry(key) {
                            HomeScreen(
                                nsdHelper = nsdHelper,
                                onSendClick = { 
                                    backStack.add(Route.Files)
                                    while (backStack.size > 1) backStack.removeAt(0)
                                },
                                onReceiveClick = { backStack.add(Route.Transfers) },
                                onHistoryClick = { backStack.add(Route.History) },
                                onTransferClick = { backStack.add(Route.Transfers) }
                            )
                        }
                    }
                    is Route.History -> {
                        NavEntry(key) {
                            val history by viewModel.history.collectAsState()
                            HistoryScreen(history = history)
                        }
                    }
                    is Route.Files -> {
                        NavEntry(key) {
                            FilesScreen(
                                onNext = { backStack.add(Route.Devices) },
                                onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) },
                                onRequestStoragePermission = onRequestStoragePermission
                            )
                        }
                    }
                    is Route.Transfers -> {
                        NavEntry(key) {
                            TransfersScreen(
                                onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) },
                                nsdHelper = nsdHelper
                            )
                        }
                    }
                    is Route.Devices -> {
                        NavEntry(key) {
                            DevicesScreen(
                                nsdHelper = nsdHelper,
                                onDeviceClick = { device ->
                                    viewModel.sendSelectedFiles(device)
                                    backStack.add(Route.Transfers)
                                    while (backStack.size > 1) backStack.removeAt(0)
                                },
                                onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }
                            )
                        }
                    }
                    else -> error("Unknown route")
                }
            }
        }
    }
}
