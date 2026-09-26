package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.notification.NotificationHelper
import com.example.security.AccessKey
import com.example.security.AppMaintenanceInfo
import com.example.security.BroadcastAnnouncement
import com.example.security.SecurityManager
import com.example.security.UserSession
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldenAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.delay

@Composable
fun AdminKeyDialog(
    securityManager: SecurityManager,
    onDismiss: () -> Unit,
    onLaunchBrowser: () -> Unit,
    onRevokeAdmin: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Passkeys, 1 = User Devices, 2 = Announcements
    var keysList by remember { mutableStateOf(securityManager.getAllKeys()) }
    var userSessions by remember { mutableStateOf<List<UserSession>>(emptyList()) }
    var showCreateForm by remember { mutableStateOf(false) }
    var customLabel by remember { mutableStateOf("") }
    var customCode by remember { mutableStateOf("") }
    var isInfiniteSelected by remember { mutableStateOf(false) }
    var copiedKeyId by remember { mutableStateOf<String?>(null) }
    var isSyncing by remember { mutableStateOf(false) }
    var announcementText by remember { mutableStateOf("") }
    var activeAnnouncement by remember { mutableStateOf<BroadcastAnnouncement?>(null) }
    var isPublishingAnnouncement by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableIntStateOf(0) } // 0 = All, 1 = Active, 2 = Expired
    var maintenanceInfo by remember { mutableStateOf(AppMaintenanceInfo()) }
    var maintenanceMessageInput by remember { mutableStateOf("") }
    var isUpdatingMaintenance by remember { mutableStateOf(false) }
    val notificationHelper = remember { NotificationHelper(context) }
    val coroutineScope = rememberCoroutineScope()
    var customNotificationInput by remember { mutableStateOf("") }
    var showPurgeConfirmDialog by remember { mutableStateOf(false) }

    fun refreshKeys() {
        keysList = securityManager.getAllKeys()
    }

    val filteredKeys = remember(keysList, searchQuery, filterType) {
        keysList.filter { key ->
            val matchesSearch = searchQuery.isBlank() ||
                key.code.contains(searchQuery.trim()) ||
                key.label.contains(searchQuery.trim(), ignoreCase = true)
            val matchesFilter = when (filterType) {
                1 -> !key.isExpired && !key.isUsed
                2 -> key.isExpired
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    // Real-time Firestore listeners for Keys, User Sessions, Announcements & Maintenance Mode
    DisposableEffect(securityManager) {
        val keysListener: (List<AccessKey>) -> Unit = { updated ->
            keysList = updated
        }
        securityManager.addKeysUpdateListener(keysListener)

        val sessionsRegistration = securityManager.listenToUserSessions { sessions ->
            userSessions = sessions
        }

        val announcementRegistration = securityManager.listenToAnnouncements { announcement ->
            activeAnnouncement = announcement
        }

        val maintenanceRegistration = securityManager.listenToMaintenance { info ->
            maintenanceInfo = info
            if (maintenanceMessageInput.isBlank()) {
                maintenanceMessageInput = info.message
            }
        }

        onDispose {
            securityManager.removeKeysUpdateListener(keysListener)
            sessionsRegistration?.remove()
            announcementRegistration?.remove()
            maintenanceRegistration?.remove()
        }
    }

    // Periodic countdown updater
    LaunchedEffect(Unit) {
        while (true) {
            delay(10000)
            refreshKeys()
        }
    }

    fun copyToClipboard(key: AccessKey) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Passkey", key.code)
        clipboard.setPrimaryClip(clip)
        copiedKeyId = key.id
        val mode = if (key.isInfinite) "Infinite Admin Access" else "24 hours (1 device only)"
        Toast.makeText(context, "Passkey ${key.code} copied! ($mode)", Toast.LENGTH_SHORT).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(26.dp))
                .border(
                    1.5.dp,
                    Brush.linearGradient(
                        listOf(CyberCyan.copy(alpha = 0.8f), GoldenAccent.copy(alpha = 0.6f), CyberCyan.copy(alpha = 0.2f))
                    ),
                    RoundedCornerShape(26.dp)
                )
                .testTag("admin_key_dialog"),
            color = DarkSurface.copy(alpha = 0.98f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header Bar with Cloud Sync Status & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GoldenAccent.copy(alpha = 0.2f))
                                .border(1.dp, GoldenAccent.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Admin Console",
                                tint = GoldenAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ADMIN MANAGEMENT",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp,
                                    color = TextPrimary
                                )
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (securityManager.isCloudSyncActive()) EmeraldSuccess else GoldenAccent)
                                )
                                Text(
                                    text = if (securityManager.isCloudSyncActive()) "Firestore Live Cloud Sync" else "Local Offline Mode",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (securityManager.isCloudSyncActive()) CyberCyan else GoldenAccent,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("PW Dhyan Direct APK Link", SecurityManager.DIRECT_APK_DOWNLOAD_URL)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Direct APK Link copied! Share with anyone to download without ZIP.", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Direct APK Link",
                                tint = CyberCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                isSyncing = true
                                securityManager.refreshFromCloud { success ->
                                    isSyncing = false
                                    refreshKeys()
                                    val msg = if (success) "Synced with Firestore" else "Using local data"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync Cloud",
                                tint = if (isSyncing) GoldenAccent else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Luxury Tab Bar (Passkeys vs User Devices vs Notices)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkBackground,
                    contentColor = CyberCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = CyberCyan,
                            height = 2.dp
                        )
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "Keys (${keysList.size})",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.5.sp,
                                    color = if (selectedTab == 0) CyberCyan else TextMuted
                                )
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "Devices (${userSessions.size})",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.5.sp,
                                    color = if (selectedTab == 1) GoldenAccent else TextMuted
                                )
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(14.dp))
                                Text(
                                    text = if (activeAnnouncement?.isActive == true) "Notice 🔴" else "Notice",
                                    fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.5.sp,
                                    color = if (selectedTab == 2) CyberCyan else TextMuted
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // TAB 0: PASSKEYS MANAGEMENT
                if (selectedTab == 0) {
                    // Action Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val newKey = securityManager.createKey() // 6 digits by default!
                                refreshKeys()
                                copyToClipboard(newKey)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF030712), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+ 1 Key",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color(0xFF030712),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Button(
                            onClick = {
                                val batch = securityManager.createBatchKeys(10)
                                refreshKeys()
                                val codesText = batch.mapIndexed { i, k -> "${i + 1}. ${k.code}" }.joinToString("\n")
                                val fullMessage = "🔑 PW DHYAN ACCESS PASSKEYS (24-Hour Access):\n$codesText\n\nEnter any 6-digit code above on the lock screen."
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("PW Dhyan Batch Keys", fullMessage)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "10 passkeys generated & copied to clipboard!", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.height(42.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldenAccent),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF030712), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Batch 10",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color(0xFF030712),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Button(
                            onClick = { showCreateForm = !showCreateForm },
                            modifier = Modifier.height(42.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showCreateForm) GoldenAccent.copy(alpha = 0.25f) else DarkSurfaceVariant
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (showCreateForm) GoldenAccent else CyberCyan.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(
                                text = if (showCreateForm) "Close" else "Custom",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (showCreateForm) GoldenAccent else TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    // Custom Key Creator Form
                    AnimatedVisibility(visible = showCreateForm) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkBackground),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldenAccent.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Create Passkey (1-Key 1-Device Locked)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = GoldenAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = customCode,
                                        onValueChange = { if (it.length <= 8) customCode = it },
                                        label = { Text("Code (6 digits)", fontSize = 11.sp) },
                                        placeholder = { Text("Auto 6-digit") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = GoldenAccent,
                                            unfocusedBorderColor = DarkSurfaceVariant,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        )
                                    )

                                    OutlinedTextField(
                                        value = customLabel,
                                        onValueChange = { customLabel = it },
                                        label = { Text("Student Name / Label", fontSize = 11.sp) },
                                        placeholder = { Text("e.g. Student 1") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.3f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CyberCyan,
                                            unfocusedBorderColor = DarkSurfaceVariant,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (!isInfiniteSelected) CyberCyan.copy(alpha = 0.2f) else DarkSurfaceVariant,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (!isInfiniteSelected) CyberCyan else Color.Transparent),
                                        modifier = Modifier.weight(1f).clickable { isInfiniteSelected = false }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Default.Timer, contentDescription = null, tint = if (!isInfiniteSelected) CyberCyan else TextMuted, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("24h (1-Device)", style = MaterialTheme.typography.labelSmall.copy(color = if (!isInfiniteSelected) CyberCyan else TextMuted, fontWeight = FontWeight.Bold))
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isInfiniteSelected) GoldenAccent.copy(alpha = 0.2f) else DarkSurfaceVariant,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isInfiniteSelected) GoldenAccent else Color.Transparent),
                                        modifier = Modifier.weight(1f).clickable { isInfiniteSelected = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Default.AllInclusive, contentDescription = null, tint = if (isInfiniteSelected) GoldenAccent else TextMuted, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Infinite Admin", style = MaterialTheme.typography.labelSmall.copy(color = if (isInfiniteSelected) GoldenAccent else TextMuted, fontWeight = FontWeight.Bold))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        val newKey = securityManager.createKey(
                                            customCode = customCode.ifBlank { null },
                                            label = customLabel,
                                            isInfinite = isInfiniteSelected
                                        )
                                        refreshKeys()
                                        copyToClipboard(newKey)
                                        customCode = ""
                                        customLabel = ""
                                        showCreateForm = false
                                    },
                                    modifier = Modifier.fillMaxWidth().height(38.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isInfiniteSelected) GoldenAccent else CyberCyan),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = if (isInfiniteSelected) "Create Infinite Admin Key" else "Generate 6-Digit Key",
                                        style = MaterialTheme.typography.labelLarge.copy(color = Color(0xFF030712), fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search & Filter Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search passkey code or student name...", fontSize = 11.5.sp, color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = CyberCyan, modifier = Modifier.size(16.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp).clickable { searchQuery = "" }
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter Chips & Quick Share Active Keys
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val activeCount = keysList.count { !it.isExpired && !it.isUsed }
                        val expiredCount = keysList.count { it.isExpired }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (filterType == 0) CyberCyan.copy(alpha = 0.25f) else DarkBackground,
                                border = BorderStroke(1.dp, if (filterType == 0) CyberCyan else DarkSurfaceVariant),
                                modifier = Modifier.clickable { filterType = 0 }
                            ) {
                                Text(
                                    text = "All (${keysList.size})",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (filterType == 0) CyberCyan else TextMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (filterType == 1) EmeraldSuccess.copy(alpha = 0.25f) else DarkBackground,
                                border = BorderStroke(1.dp, if (filterType == 1) EmeraldSuccess else DarkSurfaceVariant),
                                modifier = Modifier.clickable { filterType = 1 }
                            ) {
                                Text(
                                    text = "Active ($activeCount)",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (filterType == 1) EmeraldSuccess else TextMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }

                            if (expiredCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (filterType == 2) CrimsonAlert.copy(alpha = 0.25f) else DarkBackground,
                                    border = BorderStroke(1.dp, if (filterType == 2) CrimsonAlert else DarkSurfaceVariant),
                                    modifier = Modifier.clickable { filterType = 2 }
                                ) {
                                    Text(
                                        text = "Expired ($expiredCount)",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (filterType == 2) CrimsonAlert else TextMuted,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }

                        // Share / Export Active Keys
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (activeCount > 0) {
                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("PW Dhyan Active Passkeys", securityManager.getAllActiveKeysFormatted())
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "$activeCount active passkeys copied for WhatsApp/Telegram!", Toast.LENGTH_LONG).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldenAccent.copy(alpha = 0.2f)),
                                    border = BorderStroke(1.dp, GoldenAccent.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(26.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = GoldenAccent, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Share Active", style = MaterialTheme.typography.labelSmall.copy(color = GoldenAccent, fontWeight = FontWeight.Bold, fontSize = 9.5.sp))
                                }
                            }

                            if (expiredCount > 0) {
                                Text(
                                    text = "Clear Expired",
                                    modifier = Modifier
                                        .clickable {
                                            val count = securityManager.clearExpiredKeys()
                                            refreshKeys()
                                            Toast.makeText(context, "$count expired keys cleared", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.SemiBold, fontSize = 9.5.sp)
                                )
                            }

                            if (keysList.isNotEmpty()) {
                                Text(
                                    text = "Purge All",
                                    modifier = Modifier
                                        .clickable {
                                            showPurgeConfirmDialog = true
                                        }
                                        .padding(4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(color = CrimsonAlert, fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Keys List
                    if (filteredKeys.isEmpty()) {
                        EmptyListPlaceholder(
                            if (keysList.isEmpty()) "No Passkeys Created" else "No Matching Keys Found",
                            if (keysList.isEmpty()) "Tap '+ 1 Key' or 'Batch 10' above to generate access codes." else "No keys match '$searchQuery'. Clear search to see all."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(filteredKeys, key = { it.id }) { key ->
                                KeyCard(
                                    key = key,
                                    isCopied = copiedKeyId == key.id,
                                    onCopy = { copyToClipboard(key) },
                                    onDelete = {
                                        securityManager.removeKey(key.id)
                                        refreshKeys()
                                        Toast.makeText(context, "Key ${key.code} deleted", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                } else if (selectedTab == 1) {
                    // TAB 1: USER DEVICES & LOGINS MONITOR
                    var deviceSearchQuery by remember { mutableStateOf("") }
                    val filteredUserSessions = remember(userSessions, deviceSearchQuery) {
                        if (deviceSearchQuery.isBlank()) userSessions
                        else userSessions.filter {
                            it.deviceModel.contains(deviceSearchQuery, ignoreCase = true) ||
                            it.deviceId.contains(deviceSearchQuery, ignoreCase = true) ||
                            it.label.contains(deviceSearchQuery, ignoreCase = true) ||
                            it.passkey.contains(deviceSearchQuery, ignoreCase = true)
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        Text(
                            text = "Live Logged-In User Devices (Firestore Synced)",
                            style = MaterialTheme.typography.labelMedium.copy(color = GoldenAccent, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Every phone that uses a passkey is tracked here. You can revoke access anytime.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp),
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )

                        if (userSessions.isNotEmpty()) {
                            OutlinedTextField(
                                value = deviceSearchQuery,
                                onValueChange = { deviceSearchQuery = it },
                                placeholder = { Text("Search by device, model, passkey...", fontSize = 12.sp, color = TextMuted) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldenAccent, modifier = Modifier.size(16.dp)) },
                                trailingIcon = {
                                    if (deviceSearchQuery.isNotBlank()) {
                                        IconButton(onClick = { deviceSearchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldenAccent,
                                    unfocusedBorderColor = DarkSurfaceVariant,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = DarkBackground,
                                    unfocusedContainerColor = DarkBackground
                                )
                            )
                        }

                        if (userSessions.isEmpty()) {
                            EmptyListPlaceholder("No User Devices Logged In Yet", "When students or users enter their passkey, their phone model, Android ID, and login timestamp will appear here live.")
                        } else if (filteredUserSessions.isEmpty()) {
                            EmptyListPlaceholder("No Matching Devices", "No device found matching \"$deviceSearchQuery\".")
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(filteredUserSessions, key = { it.deviceId }) { session ->
                                    UserDeviceCard(
                                        session = session,
                                        onGrantAccess = {
                                            securityManager.grantDeviceAccess(session.deviceId, 6f, false, session.label) {
                                                Toast.makeText(context, "Granted 6 hours to ${session.deviceModel}!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onTakeBackAccess = {
                                            securityManager.revokeAccessNoBan(session.deviceId) {
                                                Toast.makeText(context, "Access taken back from ${session.deviceModel} (No ban)!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onRevoke = {
                                            securityManager.revokeDevice(session.deviceId) {
                                                Toast.makeText(context, "Device ${session.deviceModel} banned!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onSuspend = {
                                            securityManager.suspendDevice(session.deviceId, 5) {
                                                Toast.makeText(context, "Device ${session.deviceModel} placed on 5m timeout!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onUnsuspend = {
                                            securityManager.unsuspendDevice(session.deviceId) {
                                                Toast.makeText(context, "Device ${session.deviceModel} timeout cleared!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // TAB 2: LIVE BROADCAST NOTICES
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Text(
                            text = "Live Broadcast Announcements",
                            style = MaterialTheme.typography.labelMedium.copy(color = CyberCyan, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Notices broadcast here appear instantly at the top of all active student screens.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp),
                            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                        )

                        // Current Broadcast Status Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (activeAnnouncement?.isActive == true) DarkBackground else DarkBackground.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (activeAnnouncement?.isActive == true) CyberCyan.copy(alpha = 0.7f) else DarkSurfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (activeAnnouncement?.isActive == true) CrimsonAlert else TextMuted)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (activeAnnouncement?.isActive == true) "CURRENT LIVE BROADCAST" else "NO ACTIVE BROADCAST",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (activeAnnouncement?.isActive == true) CrimsonAlert else TextMuted,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }

                                    if (activeAnnouncement?.isActive == true) {
                                        Button(
                                            onClick = {
                                                securityManager.clearAnnouncement {
                                                    Toast.makeText(context, "Announcement cleared!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert.copy(alpha = 0.2f)),
                                            border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(28.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text("Clear Notice", style = MaterialTheme.typography.labelSmall.copy(color = CrimsonAlert, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                                        }
                                    }
                                }

                                if (activeAnnouncement?.isActive == true) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "\"${activeAnnouncement?.message}\"",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "By: ${activeAnnouncement?.author} • Published to all students",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Publish a notice below to alert all students in real time.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Publish New Announcement Form
                        Text(
                            text = "Send New Announcement",
                            style = MaterialTheme.typography.labelMedium.copy(color = GoldenAccent, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = announcementText,
                            onValueChange = { announcementText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            placeholder = {
                                Text("e.g. Physics Chapter 3 DPP & Notes uploaded! Live class starts at 6 PM.", color = TextMuted.copy(alpha = 0.6f), fontSize = 12.sp)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = DarkSurfaceVariant,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkBackground,
                                unfocusedContainerColor = DarkBackground
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (announcementText.isNotBlank()) {
                                    isPublishingAnnouncement = true
                                    securityManager.publishAnnouncement(announcementText.trim()) { success ->
                                        isPublishingAnnouncement = false
                                        if (success) {
                                            announcementText = ""
                                            Toast.makeText(context, "Announcement broadcasted live to all devices!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Failed to broadcast. Check connection.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            enabled = announcementText.isNotBlank() && !isPublishingAnnouncement,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFF030712), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPublishingAnnouncement) "Broadcasting..." else "Broadcast to All Students",
                                style = MaterialTheme.typography.labelLarge.copy(color = Color(0xFF030712), fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Custom Notifications & Motivation Spam
                        Text(
                            text = "Push Notification Alerts",
                            style = MaterialTheme.typography.labelMedium.copy(color = GoldenAccent, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Send custom alerts or a burst of motivational reminders to keep students on track.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkBackground),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, DarkSurfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                OutlinedTextField(
                                    value = customNotificationInput,
                                    onValueChange = { customNotificationInput = it },
                                    placeholder = { Text("e.g. Padh lo beta! Exam nazdeek hai!", fontSize = 11.sp, color = TextMuted) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GoldenAccent,
                                        unfocusedBorderColor = DarkSurfaceVariant,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedContainerColor = DarkSurface,
                                        unfocusedContainerColor = DarkSurface
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (customNotificationInput.isNotBlank()) {
                                                notificationHelper.sendCustomNotification("PW DHYAN", customNotificationInput.trim())
                                                Toast.makeText(context, "Notification sent!", Toast.LENGTH_SHORT).show()
                                                customNotificationInput = ""
                                            }
                                        },
                                        enabled = customNotificationInput.isNotBlank(),
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Push Custom", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF030712), fontWeight = FontWeight.Bold))
                                    }

                                    Button(
                                        onClick = {
                                            notificationHelper.sendBatchMotivationSpam(coroutineScope, count = 10)
                                            Toast.makeText(context, "Blasting 10 Motivation Notifications! 🔥", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1.3f).height(36.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldenAccent),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("🔥 Spam 10 Alerts", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF030712), fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Emergency Maintenance Mode / App Lockdown
                        Text(
                            text = "Emergency Maintenance Lockdown",
                            style = MaterialTheme.typography.labelMedium.copy(color = CrimsonAlert, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "When active, normal student access is paused. Master Admin passkey always bypasses.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (maintenanceInfo.isActive) CrimsonAlert.copy(alpha = 0.15f) else DarkBackground
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (maintenanceInfo.isActive) CrimsonAlert else DarkSurfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (maintenanceInfo.isActive) CrimsonAlert else EmeraldSuccess)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (maintenanceInfo.isActive) "LOCKDOWN ACTIVE" else "STUDENTS ACTIVE",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (maintenanceInfo.isActive) CrimsonAlert else EmeraldSuccess,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            isUpdatingMaintenance = true
                                            securityManager.setMaintenanceMode(
                                                isActive = !maintenanceInfo.isActive,
                                                message = maintenanceMessageInput
                                            ) {
                                                isUpdatingMaintenance = false
                                            }
                                        },
                                        enabled = !isUpdatingMaintenance,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (maintenanceInfo.isActive) EmeraldSuccess else CrimsonAlert
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp)
                                    ) {
                                        Text(
                                            text = if (maintenanceInfo.isActive) "Disable Lockdown" else "Enable Lockdown",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = maintenanceMessageInput,
                                    onValueChange = { maintenanceMessageInput = it },
                                    label = { Text("Lockdown Message for Students", fontSize = 10.sp) },
                                    placeholder = { Text("e.g. Updating servers. Classes resume shortly!") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GoldenAccent,
                                        unfocusedBorderColor = DarkSurfaceVariant,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedContainerColor = DarkBackground,
                                        unfocusedContainerColor = DarkBackground
                                    )
                                )
                            }
                        }
                    }
                }

                // Burst Motivation Notification Action Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = GoldenAccent.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, GoldenAccent.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚡ Burst Motivation Alerts",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GoldenAccent,
                                    fontSize = 12.sp
                                )
                            )
                            Text(
                                text = "Send a rapid burst of 5 high-priority study motivation alerts to student devices.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                securityManager.sendPushNotification(
                                    title = "🔥 WAKE UP & STUDY!",
                                    message = "Selection is consistency! Complete your lecture target right now! 📚",
                                    targetType = "ALL",
                                    targetValue = "ALL",
                                    isBurst = true,
                                    burstCount = 5
                                )
                                com.example.notification.NotificationHelper(context)
                                    .sendBatchMotivationSpam(coroutineScope, 5)
                                Toast.makeText(context, "5x Burst Notification Fired!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldenAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Text(
                                text = "5x Burst",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF030712),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Revoke Admin Access to Test as Normal User Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CrimsonAlert.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Test as Normal User",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 12.sp
                                )
                            )
                            Text(
                                text = "Revoke Admin access on this phone to test passkeys, 5-min alerts & lock screen.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                securityManager.revokeAdminAccess()
                                Toast.makeText(
                                    context,
                                    "Admin revoked! You are now a Normal User. Re-enter Admin Passkey anytime to regain Admin.",
                                    Toast.LENGTH_LONG
                                ).show()
                                onDismiss()
                                onRevokeAdmin()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Revoke Admin",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onLaunchBrowser,
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Open Portal", style = MaterialTheme.typography.labelLarge.copy(color = Color(0xFF030712), fontWeight = FontWeight.Bold))
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close", style = MaterialTheme.typography.labelLarge.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold))
                    }
                }
            }
        }
    }

    if (showPurgeConfirmDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPurgeConfirmDialog = false },
            title = {
                Text("Purge ALL Passkeys?", color = CrimsonAlert, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to delete all ${keysList.size} passkeys from Google Cloud Firestore?\n\nThis will instantly remove all keys and eliminate database lag. This action cannot be undone.",
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = securityManager.purgeAllKeys { purged ->
                            refreshKeys()
                            Toast.makeText(context, "Purged $purged passkeys!", Toast.LENGTH_SHORT).show()
                        }
                        showPurgeConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert)
                ) {
                    Text("Delete Everything", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showPurgeConfirmDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = DarkBackground,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun KeyCard(
    key: AccessKey,
    isCopied: Boolean,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = when {
        key.isInfinite -> GoldenAccent.copy(alpha = 0.6f)
        key.isExpired -> CrimsonAlert.copy(alpha = 0.4f)
        else -> CyberCyan.copy(alpha = 0.35f)
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("key_card_${key.code}"),
        colors = CardDefaults.cardColors(containerColor = DarkBackground),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        key.isInfinite -> GoldenAccent.copy(alpha = 0.18f)
                        key.isExpired -> CrimsonAlert.copy(alpha = 0.12f)
                        else -> CyberCyan.copy(alpha = 0.15f)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when {
                            key.isInfinite -> GoldenAccent.copy(alpha = 0.6f)
                            key.isExpired -> CrimsonAlert.copy(alpha = 0.3f)
                            else -> CyberCyan.copy(alpha = 0.5f)
                        }
                    )
                ) {
                    Text(
                        text = key.code,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = when {
                                key.isInfinite -> GoldenAccent
                                key.isExpired -> CrimsonAlert
                                else -> CyberCyan
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp,
                            fontSize = 15.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = key.label,
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        )
                        if (key.isInfinite) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(shape = RoundedCornerShape(4.dp), color = GoldenAccent.copy(alpha = 0.2f)) {
                                Text("INFINITE", modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), style = MaterialTheme.typography.labelSmall.copy(color = GoldenAccent, fontWeight = FontWeight.ExtraBold, fontSize = 8.sp))
                            }
                        }
                    }

                    // 1-Key 1-Device Lock Status
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (key.isUsed && !key.deviceModel.isNullOrBlank()) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = GoldenAccent, modifier = Modifier.size(11.dp))
                            Text(
                                text = "Claimed: ${key.deviceModel}",
                                style = MaterialTheme.typography.labelSmall.copy(color = GoldenAccent, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
                            )
                        } else {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(11.dp))
                            Text(
                                text = "1-Device Lock (Unclaimed)",
                                style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan.copy(alpha = 0.8f), fontSize = 10.sp)
                            )
                        }
                    }

                    // Remaining time & creation date
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = key.getRemainingTimeFormatted(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (key.isExpired) CrimsonAlert else EmeraldSuccess,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.5.sp
                            )
                        )
                        Text(
                            text = "• ${key.getCreatedTimeFormatted()}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                        )
                    }
                }
            }

            // Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCopy, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = if (isCopied) EmeraldSuccess else CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = CrimsonAlert.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UserDeviceCard(
    session: UserSession,
    onGrantAccess: () -> Unit,
    onTakeBackAccess: () -> Unit,
    onRevoke: () -> Unit,
    onSuspend: () -> Unit,
    onUnsuspend: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkBackground),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when {
                session.isRevoked -> CrimsonAlert.copy(alpha = 0.5f)
                session.isSuspended() -> GoldenAccent.copy(alpha = 0.5f)
                else -> CyberCyan.copy(alpha = 0.35f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    session.isRevoked -> CrimsonAlert.copy(alpha = 0.15f)
                                    session.isSuspended() -> GoldenAccent.copy(alpha = 0.15f)
                                    else -> CyberCyan.copy(alpha = 0.15f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = when {
                                session.isRevoked -> CrimsonAlert
                                session.isSuspended() -> GoldenAccent
                                else -> CyberCyan
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = session.deviceModel,
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val isOnlineNow = session.isCurrentlyOnline
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when {
                                    session.isRevoked -> CrimsonAlert.copy(alpha = 0.2f)
                                    session.accessRevoked -> GoldenAccent.copy(alpha = 0.2f)
                                    session.isSuspended() -> GoldenAccent.copy(alpha = 0.2f)
                                    isOnlineNow -> EmeraldSuccess.copy(alpha = 0.2f)
                                    else -> TextMuted.copy(alpha = 0.15f)
                                }
                            ) {
                                Text(
                                    text = when {
                                        session.isRevoked -> "REVOKED"
                                        session.accessRevoked -> "KEY REVOKED"
                                        session.isSuspended() -> "TIMEOUT (${session.getRemainingSuspensionFormatted()})"
                                        isOnlineNow -> "ONLINE"
                                        else -> "OFFLINE"
                                    },
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = when {
                                            session.isRevoked -> CrimsonAlert
                                            session.accessRevoked -> GoldenAccent
                                            session.isSuspended() -> GoldenAccent
                                            isOnlineNow -> EmeraldSuccess
                                            else -> TextMuted
                                        },
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 8.5.sp
                                    )
                                )
                            }
                        }

                        Text(
                            text = "Time in App: ${session.getActiveDurationFormatted()} • ID: ${session.deviceId.take(10)}...",
                            style = MaterialTheme.typography.labelSmall.copy(color = GoldenAccent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        )

                        Text(
                            text = "Key: ${session.passkey} (${session.label}) • Logged in: ${session.getFormattedLoginTime()}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.5.sp)
                        )

                        if (!session.currentLecture.isNullOrBlank()) {
                            Text(
                                text = "🎬 Watching: ${session.currentLecture} (${session.currentProgressPercent}%)",
                                style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            )
                        } else if (!session.currentPageTitle.isNullOrBlank() || !session.currentUrl.isNullOrBlank()) {
                            Text(
                                text = "🌐 Viewing: ${session.currentPageTitle ?: "Webpage"}",
                                style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                        if (!session.currentUrl.isNullOrBlank()) {
                            Text(
                                text = "🔗 ${session.currentUrl}",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 8.5.sp),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (!session.isRevoked) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 0. Grant 6 Hours (Instantly unlocks student phone)
                    Button(
                        onClick = onGrantAccess,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess.copy(alpha = 0.18f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Grant 6h", style = MaterialTheme.typography.labelSmall.copy(color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // 1. Take Back Access (No Ban - Locks student screen immediately)
                    Button(
                        onClick = onTakeBackAccess,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.18f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Take Back Key", style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    if (session.isSuspended()) {
                        Button(
                            onClick = onUnsuspend,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldenAccent.copy(alpha = 0.2f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldenAccent.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Unsuspend", style = MaterialTheme.typography.labelSmall.copy(color = GoldenAccent, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                        }
                    } else {
                        Button(
                            onClick = onSuspend,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldenAccent.copy(alpha = 0.15f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldenAccent.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("5m Timeout", style = MaterialTheme.typography.labelSmall.copy(color = GoldenAccent, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = onRevoke,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert.copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Ban Device", style = MaterialTheme.typography.labelSmall.copy(color = CrimsonAlert, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyListPlaceholder(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DarkBackground.copy(alpha = 0.5f))
            .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(20.dp)
        ) {
            Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = TextMuted.copy(alpha = 0.4f), modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.titleSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall.copy(color = TextMuted.copy(alpha = 0.7f), fontSize = 11.sp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
