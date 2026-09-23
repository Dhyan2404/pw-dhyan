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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
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
import com.example.security.AccessKey
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
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Passkeys, 1 = User Devices
    var keysList by remember { mutableStateOf(securityManager.getAllKeys()) }
    var userSessions by remember { mutableStateOf<List<UserSession>>(emptyList()) }
    var showCreateForm by remember { mutableStateOf(false) }
    var customLabel by remember { mutableStateOf("") }
    var customCode by remember { mutableStateOf("") }
    var isInfiniteSelected by remember { mutableStateOf(false) }
    var copiedKeyId by remember { mutableStateOf<String?>(null) }
    var isSyncing by remember { mutableStateOf(false) }

    fun refreshKeys() {
        keysList = securityManager.getAllKeys()
    }

    // Real-time Firestore listeners for Keys and User Sessions
    DisposableEffect(securityManager) {
        val keysListener: (List<AccessKey>) -> Unit = { updated ->
            keysList = updated
        }
        securityManager.addKeysUpdateListener(keysListener)

        val sessionsRegistration = securityManager.listenToUserSessions { sessions ->
            userSessions = sessions
        }

        onDispose {
            securityManager.removeKeysUpdateListener(keysListener)
            sessionsRegistration?.remove()
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

                // Luxury Tab Bar (Passkeys vs User Devices)
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
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "Passkeys (${keysList.size})",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = if (selectedTab == 0) CyberCyan else TextMuted
                                )
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "User Devices (${userSessions.size})",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = if (selectedTab == 1) GoldenAccent else TextMuted
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF030712), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "+ 6-Digit Key",
                                style = MaterialTheme.typography.labelLarge.copy(
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
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (showCreateForm) "Close" else "Custom Key",
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

                    // Keys Header Stats & Clean
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val activeCount = keysList.count { !it.isExpired }
                        val expiredCount = keysList.size - activeCount

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldSuccess.copy(alpha = 0.16f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "$activeCount Active",
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                                )
                            }

                            if (expiredCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = CrimsonAlert.copy(alpha = 0.16f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "$expiredCount Expired",
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(color = CrimsonAlert, fontWeight = FontWeight.Bold)
                                    )
                                }
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
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Keys List
                    if (keysList.isEmpty()) {
                        EmptyListPlaceholder("No Passkeys Created", "Tap '+ 6-Digit Key' above to generate access codes.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(keysList, key = { it.id }) { key ->
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
                } else {
                    // TAB 1: USER DEVICES & LOGINS MONITOR
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

                        if (userSessions.isEmpty()) {
                            EmptyListPlaceholder("No User Devices Logged In Yet", "When students or users enter their passkey, their phone model, Android ID, and login timestamp will appear here live.")
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(userSessions, key = { it.deviceId }) { session ->
                                    UserDeviceCard(
                                        session = session,
                                        onRevoke = {
                                            securityManager.revokeDevice(session.deviceId) {
                                                Toast.makeText(context, "Device ${session.deviceModel} access revoked!", Toast.LENGTH_SHORT).show()
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
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                                    "Admin revoked! You are now a Normal User. Re-enter 240411 anytime to regain Admin.",
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
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when {
                                    session.isRevoked -> CrimsonAlert.copy(alpha = 0.2f)
                                    session.isSuspended() -> GoldenAccent.copy(alpha = 0.2f)
                                    else -> EmeraldSuccess.copy(alpha = 0.2f)
                                }
                            ) {
                                Text(
                                    text = when {
                                        session.isRevoked -> "REVOKED"
                                        session.isSuspended() -> "TIMEOUT (${session.getRemainingSuspensionFormatted()})"
                                        else -> "ACTIVE"
                                    },
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = when {
                                            session.isRevoked -> CrimsonAlert
                                            session.isSuspended() -> GoldenAccent
                                            else -> EmeraldSuccess
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
                        Text("Kick", style = MaterialTheme.typography.labelSmall.copy(color = CrimsonAlert, fontWeight = FontWeight.Bold, fontSize = 10.sp))
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
