package io.github.perforators.pools

interface Pool<out T> {

    /**
     * Retrieves and removes the element of this pool,
     * or returns null if this pool is empty.
     *
     * @return the element of this pool, or null if this pool is empty
     */
    suspend fun poll(): T?

    /**
     * Retrieves and removes the element of this pool, waiting if necessary
     * until an element becomes available.
     *
     * @return the element of this queue
     */
    suspend fun take(): T
}
