package io.github.perforators.pools

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import org.junit.Test
import kotlin.test.assertEquals

internal class LifetimeLimitedValuePoolTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `poll() must immediately return the value if poll is not empty`() = runTest(UnconfinedTestDispatcher()) {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val pool = LifetimeLimitedValuePool(
            capacity = 1,
            lifetimeInMillis = Long.MAX_VALUE,
            valueProvider = { 1 },
            workDispatcher = dispatcher
        )

        pool.use {
            val value = poll()

            assertEquals(1, value)
        }
    }

    @Test
    fun `poll() must immediately return null if poll is empty`() = runBlocking {
        val pool = LifetimeLimitedValuePool(
            capacity = 1,
            lifetimeInMillis = Long.MAX_VALUE,
            valueProvider = {
                delay(1000)
                1
            }
        )

        pool.use {
            val value = poll()

            assertEquals(null, value)
        }
    }

    @Test
    fun `take() must wait for the value to appear and return it`() = runBlocking {
        val pool = LifetimeLimitedValuePool(
            capacity = 1,
            lifetimeInMillis = Long.MAX_VALUE,
            valueProvider = {
                delay(1000)
                1
            }
        )

        pool.use {
            val value = take()

            assertEquals(1, value)
        }
    }

    @Test
    fun `poll(timeout) must wait for the value to appear before the timeout expires and return it`() = runBlocking {
        val pool = LifetimeLimitedValuePool(
            capacity = 1,
            lifetimeInMillis = Long.MAX_VALUE,
            valueProvider = {
                delay(1000)
                1
            }
        )

        pool.use {
            val value = poll(2000)

            assertEquals(1, value)
        }
    }

    @Test
    fun `poll(timeout) must return null if the timeout has expired`() = runBlocking {
        val pool = LifetimeLimitedValuePool(
            capacity = 1,
            lifetimeInMillis = Long.MAX_VALUE,
            valueProvider = {
                delay(2000)
                1
            }
        )

        pool.use {
            val value = poll(1000)

            assertEquals(null, value)
        }
    }
}
