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
        val mutex = AutoOwnerMutex()
        val condition = mutex.newCondition()

        var conditionAwaitSuccess = false
        launch {
            mutex.withLock {
                condition.await(it)
                conditionAwaitSuccess = true
            }
        }
        launch {
            mutex.withLock {
                condition.signal(it)
            }
        }
        advanceUntilIdle()

        assertEquals(true, conditionAwaitSuccess)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test(expected = IllegalArgumentException::class)
    fun `await() with a different lock owner must throw an exception`() = runTest {
        val mutex = AutoOwnerMutex()
        val condition = mutex.newCondition()

        launch {
            mutex.withLock {
                val otherOwner = Any()
                condition.await(otherOwner)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test(expected = IllegalArgumentException::class)
    fun `signal() with a different lock owner must throw an exception`() = runTest {
        val mutex = AutoOwnerMutex()
        val condition = mutex.newCondition()

        launch {
            mutex.withLock {
                val otherOwner = Any()
                condition.signal(otherOwner)
            }
        }
    }
}
