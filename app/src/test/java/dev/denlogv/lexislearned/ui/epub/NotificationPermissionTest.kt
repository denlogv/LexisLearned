package dev.denlogv.lexislearned.ui.epub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionTest {
    @Test
    fun isAskedOnlyOnAndroid13OrLaterWhenNotGrantedYet() {
        assertTrue(shouldAskForNotifications(sdk = 33, granted = false))
        assertTrue(shouldAskForNotifications(sdk = 35, granted = false))
        assertFalse(shouldAskForNotifications(sdk = 33, granted = true))
        assertFalse(shouldAskForNotifications(sdk = 32, granted = false)) // earlier versions need no permission
    }
}
