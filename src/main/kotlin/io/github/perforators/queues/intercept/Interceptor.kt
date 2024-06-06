package io.github.perforators.queues.intercept

fun interface Interceptor<in T> {
    fun intercept(value: T): Boolean
}

fun <T> Interceptor<T>.interceptSafely(value: T): Boolean {
    return try {
        intercept(value)
    } catch (e: Exception) {
        false
    }
}
