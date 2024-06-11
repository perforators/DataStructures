package io.github.perforators.pools.lifetime.internal

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

internal class AutoOwnerMutex(
    private val actual: Mutex = Mutex()
) : Mutex by actual {

    private val ids = AtomicInteger(0)
    private val ownerPool = ConcurrentLinkedQueue(
        buildList {
            repeat(DEFAULT_POOL_CAPACITY) {
                add(Owner(ids.getAndIncrement()))
            }
        }
    )

    suspend inline fun <T> withLock(action: (owner: Any) -> T): T = ownerPool.use { owner ->
        withLock(owner) { action(owner) }
    }

    private inline fun <T> ConcurrentLinkedQueue<Owner>.use(action: (Owner) -> T): T {
        val owner = poll() ?: Owner(ids.getAndIncrement())
        return try {
            action(owner)
        } finally {
            offer(owner)
        }
    }

    class Owner(private val id: Int) {
        override fun toString(): String {
            return "Owner[$id]"
        }
    }

    companion object {
        private const val DEFAULT_POOL_CAPACITY = 10
    }
}
