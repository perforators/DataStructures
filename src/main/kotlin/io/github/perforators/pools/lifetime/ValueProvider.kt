package io.github.perforators.pools.lifetime

fun interface ValueProvider<out T> {
    suspend fun provide(): T
}
