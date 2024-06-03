package queues.intercept

fun interface Interceptor<in T> {
    fun intercept(value: T): Boolean
}
