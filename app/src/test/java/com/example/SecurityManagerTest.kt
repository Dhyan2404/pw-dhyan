package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.SecurityManager
import com.example.security.UnlockResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SecurityManagerTest {

    private lateinit var securityManager: SecurityManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        securityManager = SecurityManager(context)
        securityManager.setPermanentUnlocked(false)
    }

    @Test
    fun testPermanentUnlockCode2404() {
        assertFalse(securityManager.isPermanentUnlocked())
        val result = securityManager.verifyCode("2404")
        assertTrue(result is UnlockResult.PermanentUnlocked)
        assertTrue(securityManager.isPermanentUnlocked())
    }

    @Test
    fun testCurrentPhoneTimeUnlock() {
        val now = Date()
        val format24 = SimpleDateFormat("HHmm", Locale.getDefault()).format(now)
        val result = securityManager.verifyCode(format24)
        assertTrue("Expected SessionUnlocked for $format24", result is UnlockResult.SessionUnlocked)
    }

    @Test
    fun testInvalidCodeRejected() {
        val result = securityManager.verifyCode("999999")
        assertTrue(result is UnlockResult.Invalid)
    }

    @Test
    fun testUrlRestriction() {
        // Allowed URLs
        assertTrue(securityManager.isUrlAllowed("https://pw.studyparcham.in/#home-view"))
        assertTrue(securityManager.isUrlAllowed("https://pw.studyparcham.in/player?videoId=123"))
        assertTrue(securityManager.isUrlAllowed("https://pw.studyparcham.in/batch/detail/456"))
        assertTrue(securityManager.isUrlAllowed("http://pw.studyparcham.in/login"))

        // Disallowed external URLs
        assertFalse(securityManager.isUrlAllowed("https://google.com"))
        assertFalse(securityManager.isUrlAllowed("https://youtube.com"))
        assertFalse(securityManager.isUrlAllowed("https://malicious-site.com"))
        assertFalse(securityManager.isUrlAllowed("https://otherparcham.in"))
    }
}
