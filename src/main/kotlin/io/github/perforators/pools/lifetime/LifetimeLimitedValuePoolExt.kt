package io.github.perforators.pools.lifetime

inline fun <T, R> LifetimeLimitedValuePool<T>.use(action: LifetimeLimitedValuePool<T>.() -> R): R {
    return try {
        action()
    } finally {
        cancel()
    }
}
