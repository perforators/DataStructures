package queues.intercept

import annotations.ThreadSafe
import java.util.*

class InterceptQueue<T>(
    private val target: Queue<T>,
    fair: Boolean = true
) : Queue<T> by target {

    private val interceptors: InterceptorsPool<T> = InterceptorsPool.of(fair)
    private val lock = Any()

    override fun add(element: T): Boolean {
        return intercept(element) || target.add(element)
    }

    override fun offer(e: T): Boolean {
        return intercept(e) || target.offer(e)
    }

    private fun intercept(value: T) = synchronized(lock) {
        interceptors.intercept(value)
    }

    @ThreadSafe
    fun addInterceptor(interceptor: Interceptor<T>) {
        synchronized(lock) {
            interceptors.register(interceptor)
        }
    }

    @ThreadSafe
    fun removeInterceptor(interceptor: Interceptor<T>) {
        synchronized(lock) {
            interceptors.unregister(interceptor)
        }
    }
}
