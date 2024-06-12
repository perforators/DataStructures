package io.github.perforators.pools.lifetime.internal


internal sealed interface LockScope {
    val owner: ConditionMutex
}
