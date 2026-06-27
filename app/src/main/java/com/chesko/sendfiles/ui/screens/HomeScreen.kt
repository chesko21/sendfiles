package com.chesko.sendfiles.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chesko.sendfiles.R
import com.chesko.sendfiles.network.NsdHelper
import com.chesko.sendfiles.ui.MainViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    nsdHelper: NsdHelper,
    onSendClick: () -> Unit,
    onReceiveClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onTransferClick: (TransferItemData) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val recentTransfers by viewModel.recentTransfers.collectAsStateWithLifecycle()
    val isRegistered by nsdHelper.isServiceRegistered.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "SendFiles",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Transfer files to TV or Phone",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        item {
            SystemReadyStatus(isRegistered)
        }

        item {
            BentoGrid(
                onSendClick = onSendClick,
                onReceiveClick = onReceiveClick,
                onHistoryClick = onHistoryClick
            )
        }

        item {
            RecentTransfersHeader()
        }

        items(
            recentTransfers,
            key = { it.title + it.detailLeft + it.progress }
        ) { data ->
            RecentTransferItem(data, onClick = { onTransferClick(data) })
        }
    }
}

@Composable
fun BentoGrid(
    onSendClick: () -> Unit,
    onReceiveClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            onClick = onSendClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(40.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Column {
                    Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.btn_send), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Icon(
                    Icons.AutoMirrored.Rounded.Send,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp).align(Alignment.BottomEnd).alpha(0.15f),
                    tint = Color.White
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                onClick = onReceiveClick,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.btn_receive), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondary)
                }
            }

            Card(
                onClick = onHistoryClick,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.btn_history), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SystemReadyStatus(isReady: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(84.dp),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        if (isReady) MaterialTheme.colorScheme.secondary 
                        else MaterialTheme.colorScheme.error, 
                        CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isReady) stringResource(R.string.system_ready) else "SYSTEM NOT READY", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.primary, 
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = if (isReady) stringResource(R.string.network_active) else "Initializing engine...", 
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = if (isReady) Icons.Rounded.WifiTethering else Icons.Rounded.WifiTetheringError, 
                contentDescription = null, 
                tint = if (isReady) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun RecentTransfersHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.recent_transfers), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

data class TransferItemData(
    val title: String,
    val subtitle: String,
    val progress: Float,
    val detailLeft: String = "",
    val detailRight: String = "",
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isCompleted: Boolean = false
)

@Composable
fun RecentTransferItem(data: TransferItemData, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = data.icon, 
                            contentDescription = null, 
                            tint = if (data.isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = data.title, 
                        style = MaterialTheme.typography.bodyLarge, 
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(data.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!data.isCompleted) {
                            Text("${(data.progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
            if (!data.isCompleted) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { data.progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(data.detailLeft, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(data.detailRight, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
