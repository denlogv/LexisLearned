package dev.denlogv.lexislearned.ai

/** Keeps the app's process alive while a generation runs, so that switching to another app does not get it killed. */
fun interface KeepAlive {
    /** Called each time a generation starts or is resumed; it must return at once and may be called while one is held already. */
    fun hold()

    /** What to use when nothing needs to be kept alive, such as in tests. */
    companion object {
        /** Keeps nothing alive. */
        val None = KeepAlive {}
    }
}
