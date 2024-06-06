package io.github.perforators.pools

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

internal class TimeLimitedValuesPoolTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `take() must wait for the value to appear and return it`() = runTest {
        val pool = TimeLimitedValuesPool(
            capacity = 1,
            lifetimeInMillis = Long.MAX_VALUE,
            valueProvider = {
                delay(1000)
                1
            }
        )

        val value = pool.take()

        assertEquals(1, value)
    }

    @Test
    fun `poll(timeout) must wait for the value to appear before the timeout expires and return it`() = runBlocking {
        val pool = TimeLimitedValuesPool(
            capacity = 1,
            lifetimeInMillis = Long.MAX_VALUE,
            valueProvider = {
                delay(1000)
                1
            }
        )

        val value = pool.poll(2000)

        assertEquals(1, value)
    }

    @Test
    fun `poll(timeout) must return null if the timeout has expired`() = runBlocking {
        val pool = TimeLimitedValuesPool(
            capacity = 1,
            lifetimeInMillis = Long.MAX_VALUE,
            valueProvider = {
                delay(2000)
                1
            }
        )

        val value = pool.poll(1000)

        assertEquals(null, value)
    }
}
