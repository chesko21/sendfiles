package com.chesko.sendfiles.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
    viewModel: MainViewModel = viewModel()
) {
    val recentTransfers by viewModel.recentTransfers.collectAsStateWithLifecycle()

    Row(modifier = Modifier.fillMaxSize().background(Color(0xFF121416))) {

        Column(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .background(Color(0xFF1A1C1E))
                .padding(vertical = 48.dp, horizontal = 24.dp)
        ) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 48.dp)
            )
            
            TvNavItem(stringResource(R.string.nav_home), Icons.Rounded.Home, isSelected = true)
            TvNavItem("File Browser", Icons.Rounded.Folder)
            TvNavItem("Transfer Status", Icons.Rounded.Speed)
            TvNavItem("Discovery", Icons.Rounded.Radar)
            
            Spacer(modifier = Modifier.weight(1f))
            
            TvNavItem("Settings", Icons.Rounded.Settings)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(48.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.secondary, CircleShape))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Living Room TV", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(color = Color(0xFF1E2022), shape = RoundedCornerShape(8.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("192.168.1.142", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.storage_available), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                        Text("42.8 GB / 64 GB", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    Surface(modifier = Modifier.size(56.dp), shape = CircleShape, color = Color(0xFF282A2C)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.AccountCircle, contentDescription = null, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(64.dp))

            Row(modifier = Modifier.fillMaxWidth().height(280.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                TvActionCard(
                    "${stringResource(R.string.btn_send)} File",
                    "Push media and documents to other devices on your network",
                    Icons.Rounded.Upload,
                    MaterialTheme.colorScheme.primary,
                    Modifier.weight(1f),
                    onClick = onSendClick
                )
                TvActionCard(
                    "${stringResource(R.string.btn_receive)} File",
                    "Make this TV discoverable to receive incoming transfers",
                    Icons.Rounded.Download,
                    MaterialTheme.colorScheme.secondary,
                    Modifier.weight(1f),
                    onClick = { }
                )
            }
            
            Spacer(modifier = Modifier.height(64.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.recent_transfers), style = MaterialTheme.typography.titleLarge)
                Surface(color = Color(0xFF282A2C), shape = CircleShape) {
                    Text(stringResource(R.string.see_all), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
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
}

@Composable
fun TvNavItem(label: String, icon: ImageVector, isSelected: Boolean = false) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = CircleShape,
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(vertical = 4.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
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
    Surface(
        modifier = modifier.fillMaxHeight(),
        onClick = onClick,
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFF1E2022)
    ) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Surface(modifier = Modifier.size(80.dp), shape = CircleShape, color = color.copy(alpha = 0.1f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(40.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun TvTransferCard(title: String, subtitle: String, progress: Float, status: String, modifier: Modifier = Modifier, isCompleted: Boolean = false) {
    Surface(
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF1E2022)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF282A2C), modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(if (isCompleted) Icons.Rounded.Image else Icons.Rounded.Movie, contentDescription = null, tint = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = if (isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.secondary,
                trackColor = Color(0xFF282A2C),
                strokeCap = StrokeCap.Round
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (isCompleted) "Successfully Sent" else "${(progress * 100).toInt()}% Complete", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(status, style = MaterialTheme.typography.labelSmall, color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
