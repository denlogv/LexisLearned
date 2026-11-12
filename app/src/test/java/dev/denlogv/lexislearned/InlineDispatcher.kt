package dev.denlogv.lexislearned

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable

/** Runs dispatched work at once on the calling thread, so a test sees the effect of background work as soon as the call returns. */
object InlineDispatcher : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) = block.run()
}
