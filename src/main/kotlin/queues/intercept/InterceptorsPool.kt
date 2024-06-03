package queues.intercept

import java.util.*

internal interface InterceptorsPool<T> {
    val size: Int
    fun register(interceptor: Interceptor<T>): Boolean
    fun unregister(interceptor: Interceptor<T>): Boolean
    fun poll(): Interceptor<T>?

    class Fair<T>(
        private val queue: Queue<Interceptor<T>> = LinkedList()
    ) : InterceptorsPool<T> {
        override val size: Int get() = queue.size
        override fun poll(): Interceptor<T>? = queue.poll()
        override fun register(interceptor: Interceptor<T>): Boolean = queue.offer(interceptor)
        override fun unregister(interceptor: Interceptor<T>): Boolean = queue.remove(interceptor)
    }

    class Unfair<T>(
        private val set: MutableSet<Interceptor<T>> = mutableSetOf()
    ) : InterceptorsPool<T> {
        override val size: Int get() = set.size
        override fun poll(): Interceptor<T>? {
            val interceptor = set.firstOrNull() ?: return null
            return interceptor.also { set.remove(it) }
        }
        override fun register(interceptor: Interceptor<T>): Boolean = set.add(interceptor)
        override fun unregister(interceptor: Interceptor<T>): Boolean = set.remove(interceptor)
    }

    companion object {
        fun <T> of(fair: Boolean): InterceptorsPool<T> = if (fair) Fair() else Unfair()
    }
}

internal fun <T> InterceptorsPool<T>.isEmpty() = size == 0

internal fun <T> InterceptorsPool<T>.isNotEmpty() = !isEmpty()

internal fun <T> InterceptorsPool<T>.register(vararg interceptors: Interceptor<T>) {
    interceptors.forEach(::register)
}
