package dev.denlogv.lexislearned.ui

/** The screens of the app and how to address them in navigation. */
object Routes {
    /** The list of decks. */
    const val LIBRARY = "library"

    /** The settings overview. */
    const val SETTINGS = "settings"

    /** The study session settings. */
    const val SETTINGS_STUDY = "settings/study"

    /** The settings of deck generation from EPUBs: provider, key, model and language. */
    const val SETTINGS_GENERATION = "settings/generation"

    /** The level and card prompt settings. */
    const val SETTINGS_PROMPT = "settings/prompt"

    /** The "deck from EPUB" screen, for a new book. */
    const val EPUB = "epub"

    /** The "deck from EPUB" screen, for picking the sections of a paused generation. */
    const val EPUB_CONTINUE = "epub/continue"

    /** Pattern of the book screen. */
    const val DECK = "deck/{deckId}"

    /** Pattern of the part screen. */
    const val PART = "part/{partId}"

    /** Pattern of the chapter screen. */
    const val CHAPTER = "chapter/{chapterId}"

    /** Pattern of the study screen. */
    const val STUDY = "study/{deckId}/{chapterId}/{partId}"

    /** Stands for "no chapter" or "no part" in the study route. */
    const val NONE = -1L

    /**
     * The book screen.
     *
     * @param deckId the deck's database id.
     * @return the route.
     */
    fun deck(deckId: Long): String = "deck/$deckId"

    /**
     * The part screen.
     *
     * @param partId the part's database id.
     * @return the route.
     */
    fun part(partId: Long): String = "part/$partId"

    /**
     * The chapter screen.
     *
     * @param chapterId the chapter's database id.
     * @return the route.
     */
    fun chapter(chapterId: Long): String = "chapter/$chapterId"

    /**
     * A study session over a book, a part or a chapter.
     *
     * @param deckId the deck's database id.
     * @param chapterId the chapter to study, or [NONE] for all.
     * @param partId the part to study, or [NONE] for all.
     * @return the route.
     */
    fun study(deckId: Long, chapterId: Long = NONE, partId: Long = NONE): String = "study/$deckId/$chapterId/$partId"
}
