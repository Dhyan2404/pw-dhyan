package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.download.DownloadedFilesManager
import com.example.download.DownloadedItem
import com.example.security.PortalConfig
import com.example.security.PortalMaintenanceInfo
import com.example.security.SecurityManager
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GoldenAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun AppSettingsDialog(
    securityManager: SecurityManager,
    currentPortal: SecurityManager.Portal,
    portalConfig: SecurityManager.PortalConfig = securityManager.getPortalConfig(),
    onPortalSelected: (SecurityManager.Portal) -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenAdmin: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, GlassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = GoldenAccent.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = "Settings",
                                    tint = GoldenAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "App Settings",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Choose Portal Server & Storage",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Section 1: Portal Server Switcher
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "CHOOSE SERVER PORTAL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )

                    // Server 1: Server Sun ☀️
                    val spMaint = portalConfig.isMaintenance(SecurityManager.Portal.STUDYPARCHAM)
                    val spBlocked = portalConfig.isBlocked(SecurityManager.Portal.STUDYPARCHAM)
                    PortalServerCard(
                        title = "Server Sun ☀️",
                        subtitle = if (spMaint) portalConfig.getMaintenanceMessage(SecurityManager.Portal.STUDYPARCHAM) else "Primary High-Speed Cloud Node",
                        url = "Encrypted Cloud Core (Node 1)",
                        isSelected = currentPortal == SecurityManager.Portal.STUDYPARCHAM,
                        accentColor = CyberCyan,
                        isMaintenance = spMaint,
                        isBlocked = spBlocked,
                        onClick = {
                            if (spBlocked) {
                                Toast.makeText(context, "Server Sun is currently disabled by Admin.", Toast.LENGTH_SHORT).show()
                            } else {
                                onPortalSelected(SecurityManager.Portal.STUDYPARCHAM)
                            }
                        }
                    )

                    // Server 2: Server Moon 🌙
                    val pwMaint = portalConfig.isMaintenance(SecurityManager.Portal.PWTHOR)
                    val pwBlocked = portalConfig.isBlocked(SecurityManager.Portal.PWTHOR)
                    PortalServerCard(
                        title = "Server Moon 🌙",
                        subtitle = if (pwMaint) portalConfig.getMaintenanceMessage(SecurityManager.Portal.PWTHOR) else "Ultra-Stream Mirror Node",
                        url = "Encrypted Cloud Mirror (Node 2)",
                        isSelected = currentPortal == SecurityManager.Portal.PWTHOR,
                        accentColor = GoldenAccent,
                        isMaintenance = pwMaint,
                        isBlocked = pwBlocked,
                        onClick = {
                            if (pwBlocked) {
                                Toast.makeText(context, "Server Moon is currently disabled by Admin.", Toast.LENGTH_SHORT).show()
                            } else {
                                onPortalSelected(SecurityManager.Portal.PWTHOR)
                            }
                        }
                    )
                }

                // Section 2: Downloads & Offline Files
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "DOWNLOADS & MEDIA",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DarkSurfaceVariant.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOpenDownloads()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldSuccess.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = "My Downloads",
                                            tint = EmeraldSuccess,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "My Downloads",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        text = "Access saved PDFs, notes & files",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextMuted
                                        )
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Open",
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Section 3: App Administration & Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Direct APK link share
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("PW Dhyan APK Link", SecurityManager.DIRECT_APK_DOWNLOAD_URL)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Direct APK Link copied!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = CyberCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Share APK",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    // Admin Panel button
                    Button(
                        onClick = {
                            onOpenAdmin()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldenAccent.copy(alpha = 0.20f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoldenAccent.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Admin",
                                tint = GoldenAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Admin",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = GoldenAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PortalServerCard(
    title: String,
    subtitle: String,
    url: String,
    isSelected: Boolean,
    accentColor: Color,
    isMaintenance: Boolean = false,
    isBlocked: Boolean = false,
    onClick: () -> Unit
) {
    val borderColor = when {
        isBlocked -> CrimsonAlert.copy(alpha = 0.6f)
        isMaintenance -> GoldenAccent.copy(alpha = 0.6f)
        isSelected -> accentColor.copy(alpha = 0.8f)
        else -> GlassBorder
    }
    val cardBg = when {
        isBlocked -> DarkSurfaceVariant.copy(alpha = 0.35f)
        isMaintenance -> GoldenAccent.copy(alpha = 0.08f)
        isSelected -> accentColor.copy(alpha = 0.12f)
        else -> DarkSurfaceVariant.copy(alpha = 0.50f)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(if (isSelected || isMaintenance || isBlocked) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = if (isBlocked) TextMuted else if (isSelected) accentColor else TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (isBlocked) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CrimsonAlert.copy(alpha = 0.22f),
                            border = BorderStroke(0.5.dp, CrimsonAlert.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "BLOCKED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CrimsonAlert,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp
                                ),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isMaintenance) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GoldenAccent.copy(alpha = 0.22f),
                            border = BorderStroke(0.5.dp, GoldenAccent.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "MAINTENANCE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = GoldenAccent,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp
                                ),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isSelected) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accentColor.copy(alpha = 0.22f),
                            border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = accentColor,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp
                                ),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isMaintenance) GoldenAccent else TextMuted,
                        fontSize = 11.sp
                    )
                )
                Text(
                    text = url,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            RadioButton(
                selected = isSelected && !isBlocked,
                onClick = { onClick() },
                colors = RadioButtonDefaults.colors(
                    selectedColor = accentColor,
                    unselectedColor = TextMuted.copy(alpha = 0.4f)
                ),
                enabled = !isBlocked
            )
        }
    }
}

@Composable
fun DownloadedFilesDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var filesList by remember { mutableStateOf(emptyList<DownloadedItem>()) }
    var isLoading by remember { mutableStateOf(true) }

    fun refreshFiles() {
        isLoading = true
        filesList = DownloadedFilesManager.getDownloadedFiles()
        isLoading = false
    }

    LaunchedEffect(Unit) {
        refreshFiles()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface.copy(alpha = 0.98f),
            border = BorderStroke(1.dp, GlassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldSuccess.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Downloads",
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Downloaded Files",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Stored in Mobile Download Folder (${filesList.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted
                                )
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { refreshFiles() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // File List or Empty State
                if (filesList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Empty",
                                tint = TextMuted.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No downloaded study files found",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextMuted,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "Click 'View' or 'Notes' on lectures to download PDFs",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filesList) { item ->
                            DownloadedItemRow(
                                item = item,
                                onOpen = {
                                    DownloadedFilesManager.openFile(context, item.file)
                                },
                                onShare = {
                                    DownloadedFilesManager.shareFile(context, item.file)
                                }
                            )
                        }
                    }
                }

                // Footer button: Open System Downloads Folder
                Button(
                    onClick = {
                        DownloadedFilesManager.openSystemDownloadsFolder(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Folder",
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Open Mobile Downloads Folder",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadedItemRow(
    item: DownloadedItem,
    onOpen: () -> Unit,
    onShare: () -> Unit
) {
    val isPdf = item.extension == "pdf"
    val isVideo = item.extension in listOf("mp4", "mkv", "webm")
    val fileIcon = if (isPdf) Icons.Default.PictureAsPdf else if (isVideo) Icons.Default.VideoFile else Icons.Default.Folder
    val iconColor = if (isPdf) CrimsonAlert else if (isVideo) CyberCyan else GoldenAccent

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, GlassBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = iconColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = fileIcon,
                            contentDescription = item.extension,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = item.sizeFormatted,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                        )
                        Text(
                            text = item.dateFormatted,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = TextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }

                IconButton(
                    onClick = onOpen,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open",
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
