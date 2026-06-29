package com.chesko.sendfiles.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chesko.sendfiles.data.TransferDirection
import com.chesko.sendfiles.data.TransferStatus
import com.chesko.sendfiles.network.NsdHelper
import com.chesko.sendfiles.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransfersScreen(
    onBack: (() -> Unit)? = null,
    nsdHelper: NsdHelper,
    isReceiveMode: Boolean = false,
    viewModel: MainViewModel = viewModel(),
) {
    val activeTransfers by viewModel.activeTransfers.collectAsStateWithLifecycle()
    val serviceName by nsdHelper.serviceName.collectAsStateWithLifecycle()
    val isRegistered by nsdHelper.isServiceRegistered.collectAsStateWithLifecycle()
    
    val currentTransfer = activeTransfers.firstOrNull()

    LaunchedEffect(isReceiveMode) {
        if (isReceiveMode) {
            viewModel.startReceiveMode(nsdHelper)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
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

            Text(
                text = if (isReceiveMode && (currentTransfer == null)) "Receive Mode" else "Transfers",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        if (activeTransfers.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = (if (isRegistered) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer).copy(alpha = 0.1f),
                        modifier = Modifier.size(160.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isRegistered) Icons.Rounded.TapAndPlay else Icons.Rounded.WifiOff,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = if (isRegistered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        if (isRegistered) "Ready to receive files" else if (isReceiveMode) "Engine Offline" else "No active transfers",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (isReceiveMode) {
                        Text(
                            if (isRegistered) "Visible as $serviceName" else "Please check your network connection",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        
                        if (isRegistered) {
                            val ipAddress = remember { com.chesko.sendfiles.util.NetworkUtils.getLocalIpAddress() ?: "" }
                            if (ipAddress.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "IP Address: $ipAddress",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        if (!isRegistered) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { viewModel.startReceiveMode(nsdHelper) }) {
                                Text("Restart Engine")
                            }
                        }
                    } else {
                        Text(
                            "Kirim file dari menu utama untuk melihat progres di sini",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(activeTransfers, key = { it.id }) { transfer ->
                    val progress = viewModel.progressMap[transfer.id] ?: 0f
                    TransferProgressItem(
                        transfer = transfer, 
                        progress = progress
                    ) { viewModel.retryTransfer(transfer) }
                }
            }
        }
    }
}

@Composable
fun TransferProgressItem(
    transfer: com.chesko.sendfiles.data.TransferRecord, 
    progress: Float,
    onRetry: () -> Unit = {}
) {
    val isFailed = transfer.status == TransferStatus.FAILED

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFailed) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) 
                            else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = if (isFailed) BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)) else null
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isFailed) MaterialTheme.colorScheme.error.copy(alpha = 0.1f) 
                            else MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (transfer.direction == TransferDirection.SEND) Icons.Rounded.Upload else Icons.Rounded.Download,
                            contentDescription = null,
                            tint = if (isFailed) MaterialTheme.colorScheme.error 
                                   else if (transfer.direction == TransferDirection.SEND) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        transfer.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (transfer.direction == TransferDirection.SEND) "To ${transfer.receiverName}" else "From ${transfer.senderName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (!isFailed) {
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (transfer.direction == TransferDirection.SEND) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                } else {
                    IconButton(
                        onClick = onRetry,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Retry", modifier = Modifier.size(20.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LinearProgressIndicator(
                progress = { if (isFailed) 0f else progress },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                color = if (isFailed) MaterialTheme.colorScheme.error 
                        else if (transfer.direction == TransferDirection.SEND) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (isFailed) "Transfer Gagal" 
                    else if (transfer.direction == TransferDirection.SEND) "Sending..." 
                    else "Receiving...",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (isFailed) "Retry" else "Active",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isFailed) MaterialTheme.colorScheme.error 
                            else if (transfer.direction == TransferDirection.SEND) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
