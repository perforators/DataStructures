package io.github.perforators.pools.lifetime.internal

import kotlinx.coroutines.sync.Mutex
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal class ConditionMutex(
    locked: Boolean = false
) : Mutex by Mutex(locked) {

    private val scope = LockScope.Default(this)

    @OptIn(ExperimentalContracts::class)
    suspend inline fun <T> withLock(action: LockScope.() -> T): T {
        contract {
            callsInPlace(action, InvocationKind.EXACTLY_ONCE)
        }

        lock()
        try {
            return scope.action()
        } finally {
            if (isLocked) {
                unlock()
            }
        }
    }

    fun newCondition(): Condition = ConditionImpl(this)

    sealed interface LockScope {
        val mutex: ConditionMutex

        class Default(override val mutex: ConditionMutex) : LockScope
    }
}
