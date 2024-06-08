package io.github.perforators.queues.intercept

import java.util.*

internal sealed interface InterceptorPool<T> {

    val size: Int

    fun register(interceptor: Interceptor<T>): Boolean

    fun unregister(interceptor: Interceptor<T>): Boolean

    fun intercept(value: T): Boolean

    private class Fair<T>(
        private val queue: Queue<Interceptor<T>> = LinkedList()
    ) : InterceptorPool<T> {

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

        override fun unregister(interceptor: Interceptor<T>): Boolean = queue.removeAll { it == interceptor }
    }

    private class Unfair<T>(
        private val map: MutableMap<Interceptor<T>, Int> = mutableMapOf()
    ) : InterceptorPool<T> {

        override var size: Int = 0
            private set

        override fun intercept(value: T): Boolean {
            val iterator = map.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val interceptor = entry.key
                if (interceptor.interceptSafely(value)) {
                    removeOneInterceptor(iterator, entry)
                    return true
                }
            }
            return false
        }

        private fun removeOneInterceptor(
            iterator: MutableIterator<MutableMap.MutableEntry<Interceptor<T>, Int>>,
            entry: MutableMap.MutableEntry<Interceptor<T>, Int>
        ) {
            if (entry.value == 1) {
                iterator.remove()
            } else {
                entry.setValue(entry.value - 1)
            }
            size--
        }

        override fun register(interceptor: Interceptor<T>): Boolean {
            map.merge(interceptor, 1) { old, increment -> old + increment }
            size++
            return true
        }

        override fun unregister(interceptor: Interceptor<T>): Boolean {
            val removedValue = map.remove(interceptor)
            if (removedValue != null) {
                size -= removedValue
                return true
            }
            return false
        }
    }

    companion object {
        fun <T> of(fair: Boolean): InterceptorPool<T> = if (fair) Fair() else Unfair()
    }
}

internal fun <T> InterceptorPool<T>.register(vararg interceptors: Interceptor<T>) {
    interceptors.forEach(::register)
}
