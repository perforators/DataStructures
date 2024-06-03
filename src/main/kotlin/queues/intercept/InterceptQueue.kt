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
        if (interceptors.isEmpty()) return@synchronized false
        val failedInterceptors = mutableListOf<Interceptor<T>>()
        while (true) {
            val interceptor = interceptors.poll() ?: break
            val successfullyInterception = try {
                interceptor.intercept(value)
            } catch (e: Exception) {
                false
            }
            if (successfullyInterception) {
                failedInterceptors.forEach(interceptors::register)
                return@synchronized true
            } else {
                failedInterceptors.add(interceptor)
            }
        }
        failedInterceptors.forEach(interceptors::register)
        false
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
