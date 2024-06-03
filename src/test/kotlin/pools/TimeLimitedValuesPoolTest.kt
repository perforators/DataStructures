package pools

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test

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

        assert(value == 1)
    }
}