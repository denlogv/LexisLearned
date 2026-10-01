package dev.denlogv.lexislearned.ui.deck

import dev.denlogv.lexislearned.MainDispatcherRule
import dev.denlogv.lexislearned.await
import dev.denlogv.lexislearned.data.DeckRepository
import dev.denlogv.lexislearned.data.memoryDb
import dev.denlogv.lexislearned.data.testSettings
import dev.denlogv.lexislearned.domain.Direction
import dev.denlogv.lexislearned.domain.Grade
import dev.denlogv.lexislearned.domain.PartRef
import dev.denlogv.lexislearned.domain.StudyMode
import dev.denlogv.lexislearned.sampleDeck
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeckViewModelsTest {
    @get:Rule val main = MainDispatcherRule()

    private val db = memoryDb()
    private val repo = DeckRepository(db)
    private val settings = testSettings()

    private suspend fun importParted(): Long {
        val part = PartRef("p", "Part")
        val deck = sampleDeck().let { d -> d.copy(chapters = d.chapters.mapIndexed { i, c -> if (i > 0) c.copy(part = part) else c }) }
        return repo.import(deck)
    }

    @Test
    fun bookScreenListsEntriesAndResets() = runBlocking {
        val id = importParted()
        val vm = DeckViewModel(repo, settings, id)
        assertEquals("Lorem", vm.deck.await { it != null }?.title)
        val entries = vm.entries.await { it.size == 2 }
        assertEquals(listOf(true, true), listOf(entries[0] is DeckEntry.ChapterRow, entries[1] is DeckEntry.PartRow))
        assertEquals(3, vm.chapters.await { it.size == 3 }.size)
        repo.finishSession(db.cardDao().cards(id).first(), Grade.GOOD, true)
        vm.resetBook().join()
        assertEquals(0, db.cardDao().cards(id).count { it.sessionsDone > 0 })
    }

    @Test
    fun partScreenShowsOnlyItsChapters() = runBlocking {
        val id = importParted()
        val partId = db.structureDao().partsList(id).single().id
        val vm = PartViewModel(repo, settings, partId)
        assertEquals("Part", vm.part.await { it != null }?.title)
        assertEquals("Lorem", vm.deck.await { it != null }?.title)
        assertEquals(2, vm.chapters.await { it.isNotEmpty() }.size)
        repo.finishSession(db.cardDao().cards(id).last(), Grade.GOOD, true)
        vm.resetPart().join()
        assertEquals(0, db.cardDao().cards(id).count { it.sessionsDone > 0 })
    }

    @Test
    fun chapterScreenShowsCardsAndResetsOne() = runBlocking {
        val id = importParted()
        val chapterId = db.structureDao().chapters(id).first().id
        val vm = ChapterViewModel(repo, settings, chapterId)
        assertEquals("Chapter 1", vm.chapter.await { it != null }?.title)
        assertEquals("Lorem", vm.deck.await { it != null }?.title)
        assertEquals(4, vm.summary.await { it != null }?.total)
        val cards = vm.cards.await { it.size == 4 }
        repo.finishSession(cards[0], Grade.GOOD, true)
        repo.finishSession(cards[1], Grade.GOOD, true)
        vm.resetCard(cards[0].id).join()
        assertEquals(1, db.cardDao().cards(id).count { it.sessionsDone > 0 })
        vm.resetChapter().join()
        assertEquals(0, db.cardDao().cards(id).count { it.sessionsDone > 0 })
    }

    @Test
    fun studyOptionsGoThroughTheSettings() {
        val vm = ChapterViewModel(repo, settings, 1)
        vm.setDirection(Direction.REVERSE)
        vm.toggleMode(StudyMode.TYPE)
        assertEquals(Direction.REVERSE, vm.prefs.value.direction)
        assertNotNull(vm.prefs.value.modes.firstOrNull { it == StudyMode.LEARN })
        assertEquals(false, StudyMode.TYPE in vm.prefs.value.modes)
    }
}
