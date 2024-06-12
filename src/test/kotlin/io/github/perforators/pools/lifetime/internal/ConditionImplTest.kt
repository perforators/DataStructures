package io.github.perforators.pools.lifetime.internal

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

internal class ConditionImplTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `await() must suspend coroutine until call signal()`() = runTest(UnconfinedTestDispatcher()) {
        val mutex = ConditionMutex()
        val condition = mutex.newCondition()

        var conditionAwaitSuccess = false
        launch {
            mutex.withLock {
                with(condition) { await() }
                conditionAwaitSuccess = true
            }
        }
        launch {
            mutex.withLock {
                condition.signal()
            }
        }
        advanceUntilIdle()

        assertEquals(true, conditionAwaitSuccess)
    }
}
