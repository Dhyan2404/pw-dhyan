package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.security.AppUpdateInfo
import com.example.ui.theme.*
import com.example.update.UpdateDownloadState
import com.example.update.UpdateManager

/**
 * Dedicated, non-dismissible screen displayed when an admin mandates
 * an app update or the current installed version is obsolete.
 */
@Composable
fun ForceUpdateScreen(
    updateInfo: AppUpdateInfo,
    updateManager: UpdateManager
) {
    val downloadState by updateManager.downloadState.collectAsState()

    // Block back navigation
    BackHandler(enabled = true) {
        // Non-dismissible
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon with glowing ring
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(CyberCyan.copy(alpha = 0.3f), DarkSurface)
                        )
                    )
                    .border(2.dp, CyberCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (updateInfo.isMandatory(BuildConfig.VERSION_CODE)) {
                    "MANDATORY UPDATE REQUIRED"
                } else {
                    "NEW UPDATE AVAILABLE"
                },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = TextPrimary
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "PW DHYAN v${updateInfo.latestVersionName.ifBlank { "1.0.${updateInfo.latestVersionCode}" }} is ready to install.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextMuted
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Version info cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(GlassBorder, GlassBorder)))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Current Version", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CyberCyan.copy(alpha = 0.5f), CyberCyan.copy(alpha = 0.5f))))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Latest Version", style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("v${updateInfo.latestVersionName.ifBlank { "1.0.${updateInfo.latestVersionCode}" }}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = CyberCyan))
                    }
                }
            }

            if (updateInfo.releaseNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(GlassBorder, GlassBorder)))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "What's New:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = GoldenAccent)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = updateInfo.releaseNotes,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary.copy(alpha = 0.9f), lineHeight = 18.sp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            when (val state = downloadState) {
                is UpdateDownloadState.Idle -> {
                    Button(
                        onClick = { updateManager.startDownload(updateInfo) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = DarkBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download & Install Update", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }

                is UpdateDownloadState.Downloading -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = CyberCyan,
                            trackColor = DarkSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val percent = (state.progress * 100).toInt()
                        val mbRead = String.format("%.1f", state.bytesRead / (1024f * 1024f))
                        val mbTotal = if (state.totalBytes > 0) String.format("%.1f MB", state.totalBytes / (1024f * 1024f)) else "..."
                        Text(
                            text = "Downloading: $percent% ($mbRead MB / $mbTotal)",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    }
                }

                is UpdateDownloadState.ReadyToInstall -> {
                    Button(
                        onClick = { updateManager.installApk(state.apkFile) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = DarkBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Install APK Now", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }

                is UpdateDownloadState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall.copy(color = CrimsonAlert),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { updateManager.startDownload(updateInfo) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = TextPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry Download", color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
