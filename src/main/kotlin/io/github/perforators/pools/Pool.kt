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
     * Retrieves and removes the element of this pool, waiting up to the
     * specified wait time if necessary for an element to become available.
     *
     * @param timeoutMillis how long to wait before giving up, in millis
     * @return the element of this pool, or null if the
     *         specified waiting time elapses before an element is available
     */
    suspend fun poll(timeoutMillis: Long): T?

    /**
     * Retrieves and removes the element of this pool, waiting if necessary
     * until an element becomes available.
     *
     * @return the element of this queue
     */
    suspend fun take(): T
}
