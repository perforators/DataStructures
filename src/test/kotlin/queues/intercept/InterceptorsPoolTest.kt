package queues.intercept

import org.junit.Test
import kotlin.test.assertEquals

internal class InterceptorsPoolTest {

    @Test
    fun `interceptors in a pool must intercept values`() {
        val interceptorsPools = listOf<InterceptorsPool<Unit>>(
            InterceptorsPool.of(fair = false),
            InterceptorsPool.of(fair = true)
        )

        for (pool in interceptorsPools) {
            var intercepted = false
            pool.register {
                intercepted = true
                true
            }
            pool.intercept(Unit)

            assertEquals(intercepted, true)
        }
    }

    @Test
    fun `the interceptor must be removed from the pool after the interception`() {
        val interceptorsPools = listOf<InterceptorsPool<Unit>>(
            InterceptorsPool.of(fair = false),
            InterceptorsPool.of(fair = true)
        )

        for (pool in interceptorsPools) {
            pool.register { true }
            pool.intercept(Unit)

            assertEquals(pool.size, 0)
        }
    }

    @Test
    fun `interceptors in a fair pool must intercept values in the order they are added to the pool`() {
        val interceptors = InterceptorsPool.of<Unit>(fair = true)
        val interceptionOrder = mutableListOf<Int>()
        interceptors.register(
            { interceptionOrder.add(0) },
            { interceptionOrder.add(1) },
            { interceptionOrder.add(2) }
        )

        repeat(interceptors.size) {
            interceptors.intercept(Unit)
        }

        val correctInterceptionOrder = listOf(0, 1, 2)
        assertEquals(interceptionOrder, correctInterceptionOrder)
    }
}
