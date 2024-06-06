package io.github.perforators.queues.intercept

import java.util.*

internal interface InterceptorsPool<T> {

    val size: Int

    fun register(interceptor: Interceptor<T>): Boolean

    fun unregister(interceptor: Interceptor<T>): Boolean

    fun intercept(value: T): Boolean

    class Fair<T>(
        private val queue: Queue<Interceptor<T>> = LinkedList()
    ) : InterceptorsPool<T> {

        override val size: Int get() = queue.size

        override fun intercept(value: T): Boolean {
            repeat(size) {
                val interceptor = queue.poll()
                if (interceptor.interceptSafely(value)) {
                    return true
                } else {
                    register(interceptor)
                }
            }
            return false
        }

        override fun register(interceptor: Interceptor<T>): Boolean = queue.offer(interceptor)

        override fun unregister(interceptor: Interceptor<T>): Boolean = queue.remove(interceptor)
    }

    class Unfair<T>(
        private val set: MutableSet<Interceptor<T>> = mutableSetOf()
    ) : InterceptorsPool<T> {

        override val size: Int get() = set.size

        override fun intercept(value: T): Boolean {
            val iterator = set.iterator()
            while (iterator.hasNext()) {
                val interceptor = iterator.next()
                if (interceptor.interceptSafely(value)) {
                    iterator.remove()
                    return true
                }
            }
            return false
        }

        override fun register(interceptor: Interceptor<T>): Boolean = set.add(interceptor)

        override fun unregister(interceptor: Interceptor<T>): Boolean = set.remove(interceptor)
    }

    companion object {
        fun <T> of(fair: Boolean): InterceptorsPool<T> = if (fair) Fair() else Unfair()
    }
}

internal fun <T> InterceptorsPool<T>.register(vararg interceptors: Interceptor<T>) {
    interceptors.forEach(::register)
}
