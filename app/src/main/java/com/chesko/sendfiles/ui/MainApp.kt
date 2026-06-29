package com.chesko.sendfiles.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
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
import com.chesko.sendfiles.ui.screens.TvNavItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    nsdHelper: NsdHelper,
    onRequestStoragePermission: () -> Unit = {},
) {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.startReceiveMode(nsdHelper)
    }

    val isTv = remember {
        context.packageManager.hasSystemFeature("android.software.leanback")
    }

    if (isTv) {
        val backStack = rememberNavBackStack(Route.Home)
        val currentRoute = backStack.lastOrNull() ?: Route.Home
        
        val homeRequester = remember { FocusRequester() }
        val filesRequester = remember { FocusRequester() }
        val transfersRequester = remember { FocusRequester() }
        val historyRequester = remember { FocusRequester() }
        val settingsRequester = remember { FocusRequester() }
        val contentRequester = remember { FocusRequester() }
        
        var isSidebarFocused by remember { mutableStateOf(value = false) }
        val activeTransfers by viewModel.activeTransfers.collectAsState()
        
        val showSidebar = currentRoute is Route.Home
        
        LaunchedEffect(activeTransfers) {
            if (activeTransfers.isNotEmpty() && (currentRoute !is Route.Transfers)) {
                backStack.add(Route.Transfers())
            }
        }

        BackHandler(enabled = backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
        }

        LaunchedEffect(currentRoute) {
            if (isSidebarFocused && showSidebar) {
                homeRequester.requestFocus()
            }
        }

        Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (showSidebar) {
                Column(
                    modifier = Modifier
                        .width(140.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .onFocusEvent { isSidebarFocused = it.hasFocus }
                        .focusProperties { 
                            right = contentRequester 
                        }
                        .padding(vertical = 32.dp, horizontal = 12.dp)
                ) {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 32.dp, start = 8.dp)
                    )
                    
                    TvNavItem(
                        label = stringResource(R.string.nav_home), 
                        icon = Icons.Rounded.Home, 
                        isSelected = true,
                        modifier = Modifier.focusRequester(homeRequester)
                    ) { 
                        // Already on home if sidebar is shown
                    }
                    TvNavItem(
                        label = stringResource(R.string.nav_files), 
                        icon = Icons.Rounded.Folder, 
                        isSelected = false,
                        modifier = Modifier.focusRequester(filesRequester)
                    ) { 
                        backStack.add(Route.Files) 
                    }
                    TvNavItem(
                        label = stringResource(R.string.nav_transfer), 
                        icon = Icons.Rounded.Speed, 
                        isSelected = false,
                        modifier = Modifier.focusRequester(transfersRequester)
                    ) { 
                        backStack.add(Route.Transfers()) 
                    }
                    TvNavItem(
                        label = stringResource(R.string.btn_history), 
                        icon = Icons.Rounded.History, 
                        isSelected = false,
                        modifier = Modifier.focusRequester(historyRequester)
                    ) { 
                        backStack.add(Route.History) 
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    TvNavItem(
                        label = stringResource(R.string.nav_settings), 
                        icon = Icons.Rounded.Settings, 
                        isSelected = false,
                        modifier = Modifier.focusRequester(settingsRequester)
                    ) { 
                        backStack.add(Route.Settings) 
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(contentRequester)
                    .focusProperties {
                        if (showSidebar) {
                            left = homeRequester
                        }
                    }
            ) {
                NavDisplay(
                    backStack = backStack,
                    modifier = Modifier.fillMaxSize(),
                    onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }
                ) { key ->
                    when (key) {
                        is Route.Home -> {
                            NavEntry(key) {
                                TvHomeScreen(
                                    nsdHelper = nsdHelper,
                                    onSendClick = { backStack.add(Route.Files) },
                                    onReceiveClick = { backStack.add(Route.Transfers(isReceiveMode = true)) },
                                    onHistoryClick = { backStack.add(Route.History) },
                                    viewModel = viewModel
                                )
                            }
                        }
                        is Route.Files -> {
                            NavEntry(key) {
                                FilesScreen(
                                    onNext = { backStack.add(Route.Devices) },
                                    onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) },
                                    onRequestStoragePermission = onRequestStoragePermission,
                                    viewModel = viewModel
                                )
                            }
                        }
                        is Route.Transfers -> {
                            NavEntry(key) {
                                TransfersScreen(
                                    onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) },
                                    nsdHelper = nsdHelper,
                                    isReceiveMode = key.isReceiveMode,
                                    viewModel = viewModel
                                )
                            }
                        }
                        is Route.History -> {
                            NavEntry(key) {
                                val history by viewModel.history.collectAsState()
                                HistoryScreen(
                                    history = history, 
                                    onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) },
                                    viewModel = viewModel
                                )
                            }
                        }
                        is Route.Settings -> {
                            NavEntry(key) {
                                Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val interactionSource = remember { MutableInteractionSource() }
                                        val isFocused by interactionSource.collectIsFocusedAsState()
                                        
                                        Surface(
                                            onClick = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) },
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
                                        Text(
                                            text = "Settings", 
                                            style = MaterialTheme.typography.titleLarge, 
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                        Text("Settings Screen", color = Color.White)
                                    }
                                }
                            }
                        }
                        is Route.Devices -> {
                            NavEntry(key) {
                                DevicesScreen(
                                    nsdHelper = nsdHelper,
                                    onDeviceClick = { device: com.chesko.sendfiles.network.Device ->
                                        viewModel.sendSelectedFiles(device)
                                        backStack.add(Route.Transfers(isReceiveMode = false))
                                        while (backStack.size > 1) backStack.removeAt(0)
                                    }
                                ) { 
                                    if (backStack.size > 1) backStack.removeAt(backStack.size - 1) 
                                }
                            }
                        }
                        else -> error("Unknown route")
                    }
                }
            }
        }
    } else {
        MobileApp(nsdHelper, viewModel, onRequestStoragePermission)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileApp(
    nsdHelper: NsdHelper,
    viewModel: MainViewModel,
    onRequestStoragePermission: () -> Unit,
) {
    val context = LocalContext.current
    val backStack = rememberNavBackStack(Route.Home)
    val currentRoute = backStack.lastOrNull() ?: Route.Home

    var showExitDialog by remember { mutableStateOf(value = false) }

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

    val activeTransfers by viewModel.activeTransfers.collectAsState()
    LaunchedEffect(activeTransfers) {
        if (activeTransfers.isNotEmpty() && (currentRoute !is Route.Transfers)) {
            backStack.add(Route.Transfers())
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
                        backStack.add(Route.Transfers())
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
                                onReceiveClick = { backStack.add(Route.Transfers(isReceiveMode = true)) },
                                onHistoryClick = { backStack.add(Route.History) },
                                onTransferClick = { backStack.add(Route.Transfers(isReceiveMode = false)) },
                                viewModel = viewModel
                            )
                        }
                    }
                    is Route.History -> {
                        NavEntry(key) {
                            val history by viewModel.history.collectAsState()
                            HistoryScreen(
                                history = history,
                                onBack = null,
                                viewModel = viewModel
                            )
                        }
                    }
                    is Route.Files -> {
                        NavEntry(key) {
                            FilesScreen(
                                onNext = { backStack.add(Route.Devices) },
                                onBack = null,
                                onRequestStoragePermission = onRequestStoragePermission,
                                viewModel = viewModel
                            )
                        }
                    }
                    is Route.Transfers -> {
                        NavEntry(key) {
                            TransfersScreen(
                                onBack = null,
                                nsdHelper = nsdHelper,
                                isReceiveMode = key.isReceiveMode,
                                viewModel = viewModel
                            )
                        }
                    }
                    is Route.Devices -> {
                        NavEntry(key) {
                            DevicesScreen(
                                nsdHelper = nsdHelper,
                                onDeviceClick = { device: com.chesko.sendfiles.network.Device ->
                                    viewModel.sendSelectedFiles(device)
                                    backStack.add(Route.Transfers(isReceiveMode = false))
                                    while (backStack.size > 1) backStack.removeAt(0)
                                },
                                onBack = null
                            )
                        }
                    }
                    else -> error("Unknown route")
                }
            }
        }
    }
}
