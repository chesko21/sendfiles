package com.chesko.sendfiles.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chesko.sendfiles.R
import com.chesko.sendfiles.network.NsdHelper
import com.chesko.sendfiles.ui.MainViewModel

@Composable
fun TvHomeScreen(
    nsdHelper: NsdHelper,
    onSendClick: () -> Unit,
    onReceiveClick: () -> Unit,
    onHistoryClick: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val recentTransfers by viewModel.recentTransfers.collectAsStateWithLifecycle()
    val isRegistered by nsdHelper.isServiceRegistered.collectAsStateWithLifecycle()
    val serviceName by nsdHelper.serviceName.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.startReceiveMode(nsdHelper)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                if (isRegistered) MaterialTheme.colorScheme.secondary 
                                else MaterialTheme.colorScheme.error, 
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.app_name), 
                        style = MaterialTheme.typography.displaySmall, 
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh, 
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isRegistered) Icons.Rounded.Wifi else Icons.Rounded.WifiOff, 
                                contentDescription = null, 
                                tint = if (isRegistered) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error, 
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                if (isRegistered) "Receiver Active" else "Engine Offline", 
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    val ipAddress = remember { com.chesko.sendfiles.util.NetworkUtils.getLocalIpAddress() ?: "No IP" }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh, 
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = ipAddress,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh, 
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = serviceName,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Surface(modifier = Modifier.size(56.dp), shape = CircleShape, color = Color(0xFF282A2C)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.AccountCircle, contentDescription = null, modifier = Modifier.size(32.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(64.dp))

        Row(modifier = Modifier.fillMaxWidth().height(280.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            TvActionCard(
                "${stringResource(R.string.btn_send)} File",
                "Pilih file dari penyimpanan untuk dikirim",
                Icons.Rounded.Upload,
                MaterialTheme.colorScheme.primary,
                Modifier.weight(1f),
                onClick = onSendClick
            )
            TvActionCard(
                "${stringResource(R.string.btn_receive)} File",
                "Masuk ke mode terima untuk menunggu kiriman",
                Icons.Rounded.Download,
                MaterialTheme.colorScheme.secondary,
                Modifier.weight(1f),
                onClick = onReceiveClick
            )
        }
        
        Spacer(modifier = Modifier.height(64.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.recent_transfers), 
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                onClick = onHistoryClick,
                color = MaterialTheme.colorScheme.surfaceContainerHigh, 
                shape = CircleShape
            ) {
                Text(
                    text = stringResource(R.string.see_all), 
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), 
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            recentTransfers.take(2).forEach { transfer ->
                TvTransferCard(
                    title = transfer.title,
                    subtitle = transfer.subtitle,
                    progress = transfer.progress,
                    status = transfer.detailRight,
                    modifier = Modifier.weight(1f),
                    isCompleted = transfer.isCompleted
                )
            }
            if (recentTransfers.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                    Text("No recent activity", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun TvNavItem(
    label: String, 
    icon: ImageVector, 
    isSelected: Boolean = false, 
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFocused -> MaterialTheme.colorScheme.primary
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else -> Color.Transparent
        },
        label = "nav_bg"
    )
    
    val contentColor by animateColorAsState(
        targetValue = when {
            isFocused -> Color.White
            isSelected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "nav_content"
    )

    val scale by animateFloatAsState(if (isFocused) 1.05f else 1f, label = "nav_scale")

    Surface(
        onClick = onClick,
        color = backgroundColor,
        shape = CircleShape,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(vertical = 4.dp)
            .scale(scale)
    ) {
        Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label, 
                color = contentColor, 
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TvActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (isFocused) 1.03f else 1f, label = "card_scale")
    
    val borderColor by animateColorAsState(
        if (isFocused) color else Color.Transparent,
        label = "card_border"
    )

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .scale(scale),
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFF1E2022),
        border = BorderStroke(if (isFocused) 3.dp else 0.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Surface(
                modifier = Modifier.size(80.dp), 
                shape = CircleShape, 
                color = if (isFocused) color else color.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon, 
                        contentDescription = null, 
                        tint = if (isFocused) Color.White else color, 
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = if (isFocused) Color.White else MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun TvTransferCard(title: String, subtitle: String, progress: Float, status: String, modifier: Modifier = Modifier, isCompleted: Boolean = false) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (isFocused) 1.02f else 1f, label = "transfer_scale")

    Surface(
        modifier = modifier
            .height(160.dp)
            .scale(scale),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        interactionSource = interactionSource,
        onClick = {},
        border = if (isFocused) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp), 
                    color = MaterialTheme.colorScheme.surfaceContainerHighest, 
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.Sync, 
                            contentDescription = null, 
                            tint = if (isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(
                        text = title, 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold, 
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle, 
                        style = MaterialTheme.typography.labelMedium, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = if (isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = if (isCompleted) "Success" else "${(progress * 100).toInt()}% Complete", 
                    style = MaterialTheme.typography.labelMedium, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = status, 
                    style = MaterialTheme.typography.labelMedium, 
                    color = if (isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
