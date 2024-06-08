package io.github.perforators.queues.intercept

import org.junit.Test
import kotlin.test.assertEquals

internal class InterceptorPoolTest {

    @Test
    fun `interceptors in a pool must intercept values`() {
        val interceptorPools = listOf<InterceptorPool<Unit>>(
            InterceptorPool.of(fair = false),
            InterceptorPool.of(fair = true)
        )

        for (pool in interceptorPools) {
            var intercepted = false
            pool.register {
                intercepted = true
                true
            }
            pool.intercept(Unit)

            assertEquals(true, intercepted)
        }
    }

    @Test
    fun `the interceptor must be removed from the pool after the interception`() {
        val interceptorPools = listOf<InterceptorPool<Unit>>(
            InterceptorPool.of(fair = false),
            InterceptorPool.of(fair = true)
        )

        for (pool in interceptorPools) {
            pool.register { true }
            pool.intercept(Unit)

            assertEquals(0, pool.size)
        }
    }

    @Test
    fun `interceptors in a fair pool must intercept values in the order they are added to the pool`() {
        val interceptors = InterceptorPool.of<Unit>(fair = true)
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
        assertEquals(correctInterceptionOrder, interceptionOrder)
    }
}
