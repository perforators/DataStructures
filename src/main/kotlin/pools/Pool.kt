package pools

interface Pool<out T> {
    suspend fun poll(): T?
}
