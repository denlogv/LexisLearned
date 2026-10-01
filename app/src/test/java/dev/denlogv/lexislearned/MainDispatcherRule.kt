package dev.denlogv.lexislearned

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Runs `viewModelScope` work right away by replacing the main dispatcher for the duration of a test. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(UnconfinedTestDispatcher())

    override fun finished(description: Description) = Dispatchers.resetMain()
}

/**
 * Waits until the state satisfies a condition.
 *
 * @param predicate the condition.
 * @return the first matching value.
 */
suspend fun <T> StateFlow<T>.await(predicate: (T) -> Boolean): T = withTimeout(AWAIT_MS) { first(predicate) }

private const val AWAIT_MS = 10_000L
