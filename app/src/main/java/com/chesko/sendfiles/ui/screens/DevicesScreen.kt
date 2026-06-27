package com.chesko.sendfiles.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chesko.sendfiles.network.Device

@Composable
fun DevicesScreen(
    nsdHelper: com.chesko.sendfiles.network.NsdHelper,
    onDeviceClick: (Device) -> Unit,
    onBack: () -> Unit
) {
    val devices by nsdHelper.discoveredDevices.collectAsState()
    val discoveryState by nsdHelper.discoveryState.collectAsState()

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
            Text(
                text = "Select Device",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            "Available on your network",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (devices.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = (if (discoveryState == com.chesko.sendfiles.network.NsdHelper.DiscoveryState.SEARCHING) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer).copy(alpha = 0.1f),
                        modifier = Modifier.size(140.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (discoveryState == com.chesko.sendfiles.network.NsdHelper.DiscoveryState.SEARCHING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(100.dp),
                                    color = MaterialTheme.colorScheme.secondary,
                                    strokeWidth = 2.dp,
                                    trackColor = Color.Transparent
                                )
                            }
                            Icon(
                                if (discoveryState == com.chesko.sendfiles.network.NsdHelper.DiscoveryState.SEARCHING) Icons.Rounded.Radar else Icons.Rounded.Wifi,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = if (discoveryState == com.chesko.sendfiles.network.NsdHelper.DiscoveryState.SEARCHING) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = when(discoveryState) {
                            com.chesko.sendfiles.network.NsdHelper.DiscoveryState.SEARCHING -> "Searching..."
                            com.chesko.sendfiles.network.NsdHelper.DiscoveryState.FINISHED -> "No devices found"
                            com.chesko.sendfiles.network.NsdHelper.DiscoveryState.ERROR -> "Search failed"
                            else -> ""
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Make sure the other device has \"Receive\" open",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    if (discoveryState != com.chesko.sendfiles.network.NsdHelper.DiscoveryState.SEARCHING) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { nsdHelper.restartDiscovery() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry Search")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(devices.toList()) { device ->
                    DeviceCard(device = device, onClick = { onDeviceClick(device) })
                }
            }
        }
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Wifi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Connected to the same WiFi",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DeviceCard(device: Device, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = device.host.hostAddress ?: "Unknown",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Rounded.Radar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
