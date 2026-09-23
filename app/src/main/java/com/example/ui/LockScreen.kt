package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.security.SecurityManager
import com.example.security.UnlockResult
import com.example.ui.AdminKeyDialog
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GoldenAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.delay

@Composable
fun LockScreen(
    securityManager: SecurityManager,
    onUnlocked: (isPermanent: Boolean) -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showAdminDialog by remember { mutableStateOf(false) }
    var showAdminAuthDialog by remember { mutableStateOf(false) }

    fun submitPin(pin: String) {
        if (pin.isBlank()) {
            errorMessage = "Please enter passcode"
            return
        }
        if (!securityManager.isCloudSyncActive()) {
            securityManager.refreshFromCloud()
            errorMessage = "Cloud Connection Required: App cannot run without an active Google Cloud connection."
            enteredPin = ""
            return
        }
        when (val result = securityManager.verifyCode(pin)) {
            is UnlockResult.PermanentUnlocked -> {
                errorMessage = null
                successMessage = "Master Code Accepted: Infinite Admin Active!"
                showAdminDialog = true
            }
            is UnlockResult.KeyUnlocked -> {
                errorMessage = null
                val timeText = if (result.key.isInfinite) "Infinite" else result.key.getRemainingTimeFormatted()
                successMessage = "Passkey Accepted ($timeText)!"
                onUnlocked(result.key.isInfinite)
            }
            is UnlockResult.SessionUnlocked -> {
                errorMessage = null
                successMessage = "Passcode Verified: Access Granted!"
                onUnlocked(false)
            }
            is UnlockResult.Invalid -> {
                errorMessage = result.reason
                // Auto-clear after brief delay on invalid
                enteredPin = ""
            }
        }
    }

    // Auto submit when 6 digits typed or matches master admin code
    LaunchedEffect(enteredPin) {
        val clean = enteredPin.trim()
        if (clean == SecurityManager.MASTER_PERMANENT_CODE) {
            delay(120)
            submitPin(clean)
        } else if (clean.length == 6) {
            delay(120)
            submitPin(clean)
        }
    }

    // Subtle pulsing animation for ambient logo ring
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070B16),
                        Color(0xFF0B132B),
                        Color(0xFF030712)
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glowing Cyber-Bird Crest
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                CyberCyan.copy(alpha = 0.28f),
                                Color(0xFF1E293B).copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            colors = listOf(CyberCyan, Color(0xFF6366F1), CyberCyan.copy(alpha = 0.3f))
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_bird_logo),
                    contentDescription = "App Logo",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(58.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // App Title "PW DHYAN"
            Text(
                text = "PW DHYAN",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = 3.sp,
                    fontSize = 26.sp
                ),
                textAlign = TextAlign.Center
            )

            // Cloud Security Connectivity Indicator
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (securityManager.isCloudSyncActive()) Color(0xFF0F172A).copy(alpha = 0.8f) else CrimsonAlert.copy(alpha = 0.18f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (securityManager.isCloudSyncActive()) CyberCyan.copy(alpha = 0.35f) else CrimsonAlert.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clickable { securityManager.refreshFromCloud() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (securityManager.isCloudSyncActive()) EmeraldSuccess else CrimsonAlert)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (securityManager.isCloudSyncActive()) "GOOGLE CLOUD CONNECTED" else "CLOUD DISCONNECTED • TAP TO RETRY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (securityManager.isCloudSyncActive()) CyberCyan else CrimsonAlert,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Elegant Glowing PIN Indicators (6 luxury cyber orbs/dots for 6-digit codes)
            Row(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .testTag("pin_display"),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 6) {
                    val isFilled = i < enteredPin.length
                    val orbColor by animateColorAsState(
                        targetValue = if (isFilled) CyberCyan else Color(0xFF1E293B),
                        animationSpec = spring(),
                        label = "orbColor"
                    )
                    val orbBorder by animateColorAsState(
                        targetValue = if (isFilled) CyberCyan else Color(0xFF334155),
                        animationSpec = spring(),
                        label = "orbBorder"
                    )

                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(orbColor)
                            .border(1.5.dp, orbBorder, CircleShape)
                    )
                }
            }

            // Error or Success Banner
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = errorMessage.orEmpty(),
                    color = CrimsonAlert,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp, start = 8.dp, end = 8.dp)
                )
            }

            AnimatedVisibility(
                visible = successMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = successMessage.orEmpty(),
                    color = EmeraldSuccess,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Luxury Tactile Numeric Keypad
            LuxuryNumericKeypad(
                onDigitClick = { digit ->
                    if (enteredPin.length < 10) {
                        enteredPin += digit
                        errorMessage = null
                    }
                },
                onBackspace = {
                    if (enteredPin.isNotEmpty()) {
                        enteredPin = enteredPin.dropLast(1)
                        errorMessage = null
                    }
                },
                onSubmit = {
                    submitPin(enteredPin)
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Admin Key Console Pill Button
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GoldenAccent.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldenAccent.copy(alpha = 0.45f)),
                modifier = Modifier
                    .clickable {
                        if (securityManager.isPermanentUnlocked()) {
                            showAdminDialog = true
                        } else {
                            showAdminAuthDialog = true
                        }
                    }
                    .testTag("admin_keys_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Admin Key Console",
                        tint = GoldenAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Admin Key Console (Cloud Synced)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = GoldenAccent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Signature Credit
            Text(
                text = "Crafted with ❤️ by Dhyan",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = CyberCyan.copy(alpha = 0.85f),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    fontSize = 11.5.sp
                ),
                textAlign = TextAlign.Center
            )
        }

        // Admin Verification Dialog (Requires Admin Code 240411)
        if (showAdminAuthDialog) {
            AdminVerificationDialog(
                securityManager = securityManager,
                onDismiss = { showAdminAuthDialog = false },
                onSuccess = {
                    showAdminAuthDialog = false
                    showAdminDialog = true
                }
            )
        }

        // Admin Key Dialog
        if (showAdminDialog) {
            AdminKeyDialog(
                securityManager = securityManager,
                onDismiss = { showAdminDialog = false },
                onLaunchBrowser = {
                    showAdminDialog = false
                    onUnlocked(true)
                },
                onRevokeAdmin = {
                    showAdminDialog = false
                    enteredPin = ""
                    errorMessage = "Admin mode revoked. You are now testing as a Normal User."
                }
            )
        }
    }
}

@Composable
private fun LuxuryNumericKeypad(
    onDigitClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit
) {
    val keypadData = listOf(
        listOf(KeypadKey("1", ""), KeypadKey("2", "ABC"), KeypadKey("3", "DEF")),
        listOf(KeypadKey("4", "GHI"), KeypadKey("5", "JKL"), KeypadKey("6", "MNO")),
        listOf(KeypadKey("7", "PQRS"), KeypadKey("8", "TUV"), KeypadKey("9", "WXYZ")),
        listOf(KeypadKey("back", ""), KeypadKey("0", "+"), KeypadKey("ok", ""))
    )

    Column(
        modifier = Modifier.fillMaxWidth(0.92f),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        keypadData.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { item ->
                    when (item.digit) {
                        "back" -> {
                            LuxuryKeypadButton(
                                onClick = onBackspace,
                                testTag = "key_backspace",
                                backgroundColor = Color(0xFF1E293B).copy(alpha = 0.5f),
                                borderColor = Color(0xFF334155).copy(alpha = 0.5f)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Delete",
                                    tint = TextMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        "ok" -> {
                            LuxuryKeypadButton(
                                onClick = onSubmit,
                                testTag = "unlock_button",
                                backgroundColor = CyberCyan.copy(alpha = 0.22f),
                                borderColor = CyberCyan.copy(alpha = 0.8f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Unlock",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        else -> {
                            LuxuryKeypadButton(
                                onClick = { onDigitClick(item.digit) },
                                testTag = "key_${item.digit}"
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = item.digit,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 24.sp
                                        )
                                    )
                                    if (item.subtext.isNotEmpty()) {
                                        Text(
                                            text = item.subtext,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = TextMuted.copy(alpha = 0.8f),
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 1.sp,
                                                fontSize = 9.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class KeypadKey(val digit: String, val subtext: String)

@Composable
private fun LuxuryKeypadButton(
    onClick: () -> Unit,
    testTag: String,
    backgroundColor: Color = Color(0xFF0F172A).copy(alpha = 0.7f),
    borderColor: Color = Color(0xFF334155).copy(alpha = 0.6f),
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(),
        label = "btnScale"
    )

    Box(
        modifier = Modifier
            .size(72.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (isPressed) backgroundColor.copy(alpha = 0.95f) else backgroundColor
            )
            .border(
                1.2.dp,
                if (isPressed) CyberCyan.copy(alpha = 0.8f) else borderColor,
                CircleShape
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
