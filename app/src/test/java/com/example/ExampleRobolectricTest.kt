package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.AccessKey
import com.example.security.SecurityManager
import com.example.security.UnlockResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PW DHYAN", appName)
  }

  @Test
  fun `admin key creation, verification, and removal`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val securityManager = SecurityManager(context)

    // Create a 24-hour key
    val key = securityManager.createKey(customCode = "7890", label = "Test Key")
    assertEquals("7890", key.code)
    assertEquals("Test Key", key.label)
    assertFalse(key.isExpired)

    // Verify key works
    val verifyResult = securityManager.verifyCode("7890")
    assertTrue(verifyResult is UnlockResult.KeyUnlocked)

    // Remove key
    val removed = securityManager.removeKey(key.id)
    assertTrue(removed)

    // Verify key no longer unlocks
    val postRemoveResult = securityManager.verifyCode("7890")
    assertTrue(postRemoveResult is UnlockResult.Invalid)
  }
}
