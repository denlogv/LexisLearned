package dev.denlogv.lexislearned.service

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
        val notification = notifier.build(GenerationNotice("Generating", "3 / 12 sections", 25, "Pause after section", "Pause now"))
        assertEquals("Generating", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("3 / 12 sections", notification.extras.getString(Notification.EXTRA_TEXT))
        assertEquals(25, notification.extras.getInt(Notification.EXTRA_PROGRESS))
        assertEquals(listOf("Pause after section", "Pause now"), notification.actions.map { it.title.toString() })
        assertNotEquals(notification.actions[0].actionIntent, notification.actions[1].actionIntent)
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertNotNull(notification.contentIntent)
    }

    @Test
    fun aResultCanBeSwipedAwayAndHasNoButton() {
        val notification = notifier.build(GenerationNotice("Deck ready", "12 cards", null, null, null))
        assertNull(notification.actions)
        assertEquals(0, notification.flags and Notification.FLAG_ONGOING_EVENT)
    }

    private val manager = app.getSystemService(NotificationManager::class.java)
    private val paused = GenerationNotice("Generation paused", "3 sections left", null, null, null)

    @Test
    fun aResultIsANotificationOfItsOwnSoALateProgressUpdateCannotReplaceIt() {
        manager.notify(
            NoticeNotifier.PROGRESS_ID,
            notifier.build(GenerationNotice("Generating", "1 / 3 sections", 33, "Pause after section", "Pause now")),
        )
        notifier.showResult(paused)
        val shown = manager.activeNotifications.associateBy { it.id }
        assertEquals(setOf(NoticeNotifier.PROGRESS_ID, NoticeNotifier.RESULT_ID), shown.keys)
        assertEquals("Generation paused", shown.getValue(NoticeNotifier.RESULT_ID).notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Generating", shown.getValue(NoticeNotifier.PROGRESS_ID).notification.extras.getString(Notification.EXTRA_TITLE))
    }

    @Test
    fun cancellingTheResultLeavesTheProgressAlone() {
        manager.notify(
            NoticeNotifier.PROGRESS_ID,
            notifier.build(GenerationNotice("Generating", "1 / 3 sections", 33, "Pause after section", "Pause now")),
        )
        notifier.showResult(paused)
        notifier.cancelResult()
        assertEquals(listOf(NoticeNotifier.PROGRESS_ID), manager.activeNotifications.map { it.id })
    }

    @Test
    fun cancellingTakesBothNoticesAwayEvenAfterTheServiceIsGone() {
        manager.notify(
            NoticeNotifier.PROGRESS_ID,
            notifier.build(GenerationNotice("Generating", "1 / 3 sections", 33, "Pause after section", "Pause now")),
        )
        notifier.showResult(paused)
        assertEquals(2, manager.activeNotifications.size)
        notifier.cancel()
        assertTrue(manager.activeNotifications.isEmpty())
    }

    @Test
    fun theChannelsAreCreated() {
        val manager = app.getSystemService(NotificationManager::class.java)
        assertEquals(setOf("generation", "generation_result"), manager.notificationChannels.map { it.id }.toSet())
    }
}
