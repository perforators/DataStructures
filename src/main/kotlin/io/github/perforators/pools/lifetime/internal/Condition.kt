package io.github.perforators.pools.lifetime.internal

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.LockFreeLinkedListHead
import kotlinx.coroutines.internal.LockFreeLinkedListNode
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlin.coroutines.resume

internal interface Condition {

    /**
     * Causes the current coroutine to suspend until it is signalled or cancelled.
     *
     * **Important:** When returning from this method due to the cancellation of the coroutine,
     * there is a chance that the lock may be in an unlocked state.
     */
    suspend fun ConditionMutex.LockScope.await()

    /**
     * Wakes up one waiting coroutine.
     *
     * If any coroutines are waiting on this condition then one is selected for waking up.
     * That coroutine must then re-acquire the lock before returning from [await].
     */
    fun signal()
}

@OptIn(InternalCoroutinesApi::class)
internal class ConditionImpl(
    private val owner: Mutex
) : Condition {

    private val waiters = LockFreeLinkedListHead()

    override suspend fun ConditionMutex.LockScope.await() {
        require(owner === mutex) {
            "await() must be call in the scope of the mutex, that owns the condition."
        }
        suspendCancellableCoroutine { continuation ->
            val waiter = Waiter(continuation)
            waiters.addLast(waiter)
            continuation.invokeOnCancellation {
                waiter.remove()
            }
            owner.unlock()
        }
        owner.lock()
    }

    override fun signal() {
        val waiter = (waiters.removeFirstOrNull() as? Waiter) ?: return
        waiter.resume()
    }

    private class Waiter(
        private val continuation: CancellableContinuation<Unit>
    ) : LockFreeLinkedListNode() {
        fun resume() {
            continuation.resume(Unit)
        }
    }
}
