package queues.intercept

import org.junit.Test

internal class InterceptorsPoolTest {

    @Test
    fun `fair interceptors pool should return interceptors in the order of addition`() {
        val interceptors = InterceptorsPool.of<Any>(fair = true)
        val firstInterceptor = Interceptor<Any> { true }
        val secondInterceptor = Interceptor<Any> { true }
        val thirdInterceptor = Interceptor<Any> { true }

        interceptors.register(firstInterceptor, secondInterceptor, thirdInterceptor)

        val result = buildList {
            while (interceptors.isNotEmpty()) {
                add(interceptors.poll()!!)
            }
        }
        assert(result.size == 3)
        assert(result[0] == firstInterceptor)
        assert(result[1] == secondInterceptor)
        assert(result[2] == thirdInterceptor)
    }
}
