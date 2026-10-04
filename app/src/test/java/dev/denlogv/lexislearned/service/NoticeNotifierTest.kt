package dev.denlogv.lexislearned.service

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NoticeNotifierTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val notifier = NoticeNotifier(app)

    @Test
    fun anOngoingNoticeHasAProgressBarAndAPauseButton() {
        val notification = notifier.build(GenerationNotice("Generating", "3 / 12 sections", 25, "Pause"))
        assertEquals("Generating", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("3 / 12 sections", notification.extras.getString(Notification.EXTRA_TEXT))
        assertEquals(25, notification.extras.getInt(Notification.EXTRA_PROGRESS))
        assertEquals("Pause", notification.actions.single().title.toString())
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertNotNull(notification.contentIntent)
    }

    @Test
    fun aResultCanBeSwipedAwayAndHasNoButton() {
        val notification = notifier.build(GenerationNotice("Deck ready", "12 cards", null, null))
        assertNull(notification.actions)
        assertEquals(0, notification.flags and Notification.FLAG_ONGOING_EVENT)
    }

    @Test
    fun theChannelsAreCreated() {
        val manager = app.getSystemService(NotificationManager::class.java)
        assertEquals(setOf("generation", "generation_result"), manager.notificationChannels.map { it.id }.toSet())
    }
}
