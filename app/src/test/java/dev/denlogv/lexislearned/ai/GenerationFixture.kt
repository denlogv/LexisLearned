package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.await
import dev.denlogv.lexislearned.data.DeckLibrary
import dev.denlogv.lexislearned.data.DeckStorage
import dev.denlogv.lexislearned.data.Provider
import dev.denlogv.lexislearned.data.memoryDb
import dev.denlogv.lexislearned.data.testSettings
import dev.denlogv.lexislearned.loremEpub
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel

/**
 * A [GenerationManager] wired to an in-memory database and a fake model, with what the tests of its behaviour share: the book,
 * the replies and a model that can be held back to observe a run halfway.
 */
class GenerationFixture {
    /** The settings the manager reads. */
    val settings = testSettings()
    private val db = memoryDb()

    /** Where the manager stores decks. */
    val storage = DeckStorage(db)

    /** Reads the stored decks. */
    val library = DeckLibrary(db)

    /** Reply for every request that is not a title request; replace it to change what the model says. */
    var answer: (String, String) -> String = { _, user -> if (user.startsWith("Give")) "{}" else CARDS_REPLY }

    /** The clients that were created, as provider, key and model. */
    val created = mutableListOf<Triple<Provider, String, String>>()
    private var gated: GatedLlm? = null

    /** The manager under test. */
    val manager = GenerationManager(CoroutineScope(Dispatchers.Unconfined), storage, settings) { prefs, key ->
        created += Triple(prefs.provider, key, prefs.model)
        gated ?: ScriptedLlm(answer)
    }

    /** A model that answers chapter requests only when released. */
    inner class GatedLlm : LlmClient {
        /** Receives a signal each time a chapter request begins. */
        val started = Channel<Unit>(Channel.UNLIMITED)
        private val permits = Channel<Unit>(Channel.UNLIMITED)

        override suspend fun complete(system: String, user: String): String {
            if (user.startsWith("Give")) return "{}"
            started.send(Unit)
            permits.receive()
            return if (user.contains("Chapter Two")) SECOND_REPLY else CARDS_REPLY
        }

        /** Lets one waiting or future chapter request through. */
        suspend fun release() = permits.send(Unit)
    }

    /**
     * Saves a key and makes the manager use a model that is held back.
     *
     * @return the model, to release requests.
     */
    fun startGated(): GatedLlm {
        settings.setApiKey(Provider.ANTHROPIC, "sk-test")
        return GatedLlm().also { gated = it }
    }

    /**
     * Loads the generated test book into the manager.
     *
     * @return the book, once the manager is ready to review it.
     */
    suspend fun ready() = (manager.state.also { manager.load(loremEpub()) }.await { it is GenState.Ready } as GenState.Ready).book

    /** Canned replies. */
    companion object {
        /** Two cards, for the first chapter. */
        const val CARDS_REPLY = """{"chapter_title_b":"Глава","cards":[{"a":"alpha","b":"альфа"},{"a":"beta","b":"бета"}]}"""

        /** One card, for the second chapter. */
        const val SECOND_REPLY = """{"chapter_title_b":"Вторая","cards":[{"a":"gamma","b":"гамма"}]}"""
    }
}
