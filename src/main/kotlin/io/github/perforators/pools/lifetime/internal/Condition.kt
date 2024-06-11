package io.github.perforators.pools.lifetime.internal

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.LockFreeLinkedListHead
import kotlinx.coroutines.internal.LockFreeLinkedListNode
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlin.coroutines.resume

internal interface Condition {
    suspend fun await(owner: Any)
    suspend fun signal(owner: Any)
}

internal fun Mutex.newCondition(): Condition {
    return ConditionImpl(this)
}

internal class ConditionImpl(
    private val mutex: Mutex
) : Condition {

    private val waiters = LockFreeLinkedListHead()

    @OptIn(InternalCoroutinesApi::class)
    override suspend fun await(owner: Any) {
        require(mutex.holdsLock(owner)) { "$mutex must holds by owner = $owner" }
        withRelock(owner) {
            suspendCancellableCoroutine { continuation ->
                val waiter = Waiter(continuation)
                waiters.addLast(waiter)
                mutex.unlock(owner)
                continuation.invokeOnCancellation {
                    waiter.remove()
                }
            }
        }
    }

    @OptIn(InternalCoroutinesApi::class)
    override suspend fun signal(owner: Any) {
        require(mutex.holdsLock(owner)) { "$mutex must holds by owner = $owner" }
        withRelock(owner) {
            val waiter = (waiters.removeFirstOrNull() as? Waiter) ?: return
            mutex.unlock(owner)
            waiter.continuation.resume(Unit)
        }
    }
    
    private suspend inline fun withRelock(owner: Any, action: () -> Unit) {
        try {
            action()
        } finally {
            if (!mutex.holdsLock(owner)) {
                mutex.lock(owner)
            }
        }
    }

    @OptIn(InternalCoroutinesApi::class)
    private class Waiter(
        val continuation: CancellableContinuation<Unit>
    ) : LockFreeLinkedListNode()
}
