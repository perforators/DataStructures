package io.github.perforators.pools.lifetime.internal

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.LockFreeLinkedListHead
import kotlinx.coroutines.internal.LockFreeLinkedListNode
import kotlinx.coroutines.sync.Mutex
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal class ConditionMutex(
    locked: Boolean = false
) : Mutex by Mutex(locked) {

    private val reusableScopes = LockFreeLinkedListHead()

    @OptIn(ExperimentalContracts::class, InternalCoroutinesApi::class)
    suspend inline fun <T> withLock(action: LockScope.() -> T): T {
        contract {
            callsInPlace(action, InvocationKind.EXACTLY_ONCE)
        }

        val scope = (reusableScopes.removeFirstOrNull() as? Node) ?: Node(this)
        lock(scope)
        try {
            return scope.action()
        } finally {
            if (holdsLock(scope)) {
                unlock(scope)
            }
            reusableScopes.addLast(scope)
        }
    }

    fun newCondition(): Condition = ConditionImpl(this)

    @OptIn(InternalCoroutinesApi::class)
    internal class Node(
        override val owner: ConditionMutex
    ) : LockScope, LockFreeLinkedListNode()
}
