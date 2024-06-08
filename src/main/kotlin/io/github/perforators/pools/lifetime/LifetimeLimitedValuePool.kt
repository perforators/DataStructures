package io.github.perforators.pools.lifetime

import io.github.perforators.pools.Pool
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.github.perforators.queues.intercept.InterceptQueue
import io.github.perforators.queues.intercept.Interceptor
import java.util.*
import kotlin.coroutines.resume

class LifetimeLimitedValuePool<T>(
    capacity: Int,
    private val lifetimeInMillis: Long,
    private val valueProvider: ValueProvider<T>,
    private val reportDispatcher: CoroutineDispatcher = Dispatchers.Default,
    workDispatcher: CoroutineDispatcher = Dispatchers.Default
) : Pool<T> {

    private val scope = CoroutineScope(workDispatcher + SupervisorJob())

    private val pool = InterceptQueue<Entry<T>>(ArrayDeque(capacity))
    private val poolMutex = Mutex()

    private val eventReporters: MutableSet<EventReporter<T>> = mutableSetOf()
    private val reporterMutex = Mutex()

    init {
        repeat(capacity) { fetchNewValue() }
        runCleaningLoop()
    }

    private fun fetchNewValue() {
        scope.launch {
            try {
                offer(valueProvider.provide())
            } catch (e: Exception) {
                report { onError(e) }
            }
        }
    }

    private suspend fun offer(value: T) = poolMutex.withLock {
        pool.offer(Entry(value))
        report {
            onAdd(value)
            onChangeSize(pool.size)
        }
    }

    private suspend inline fun report(crossinline block: EventReporter<T>.() -> Unit) {
        withContext(reportDispatcher) {
            reporterMutex.withLock {
                eventReporters.forEach(block)
            }
        }
    }

    private fun runCleaningLoop() {
        scope.launch {
            while (isActive) {
                val numberDeletedValues = clean()
                repeat(numberDeletedValues) { fetchNewValue() }
                delay(CLEANING_LOOP_DELAY)
            }
        }
    }

    private suspend fun clean() = poolMutex.withLock {
        var numberDeletedValues = 0
        while (pool.isNotEmpty() && !pool.peek().isAlive(lifetimeInMillis)) {
            val entry = pool.poll()
            report {
                onClean(entry.value)
                onChangeSize(pool.size)
            }
            numberDeletedValues++
        }
        numberDeletedValues
    }

    override suspend fun poll(): T? = poolMutex.withLock {
        pool.poll()?.value?.also { value ->
            report {
                onPoll(value)
                onChangeSize(pool.size)
            }
        }
    }?.also { fetchNewValue() }

    override suspend fun take(): T = suspendCancellableCoroutine {
        val interceptor = Interceptor<Entry<T>> { entry ->
            it.resume(entry.value)
            true
        }
        pool.addInterceptor(interceptor)
        it.invokeOnCancellation {
            pool.removeInterceptor(interceptor)
        }
    }

    override suspend fun poll(timeoutMillis: Long): T? {
        if (timeoutMillis <= 0) return poll()
        return withTimeoutOrNull(timeoutMillis) {
            take()
        }
    }

    fun registerEventReporter(eventReporter: EventReporter<T>) {
        scope.launch {
            reporterMutex.withLock {
                eventReporters.add(eventReporter)
            }
        }
    }

    fun unregisterEventReporter(eventReporter: EventReporter<T>) {
        scope.launch {
            reporterMutex.withLock {
                eventReporters.remove(eventReporter)
            }
        }
    }

    fun cancel() {
        scope.cancel()
    }

    private class Entry<T>(val value: T, private val creationTime: Long = System.currentTimeMillis()) {
        fun isAlive(lifeTime: Long): Boolean {
            return System.currentTimeMillis() - creationTime <= lifeTime
        }
    }

    companion object {
        private const val CLEANING_LOOP_DELAY = 1000L
    }
}
