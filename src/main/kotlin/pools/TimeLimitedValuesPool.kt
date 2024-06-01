package pools

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

class TimeLimitedValuesPool<T>(
    capacity: Int,
    private val lifetimeInMillis: Long,
    private val valueProvider: ValueProvider<T>,
    private val reportDispatcher: CoroutineDispatcher = Dispatchers.Default
) : Pool<T> {

    private val pool: Deque<Entry<T>> = ArrayDeque(capacity)
    private val poolMutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Volatile
    private var eventReporter: EventReporter<T> = EventReporter.NONE

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
        pool.offerLast(Entry(value))
        report {
            onAdd(value)
            onChangeSize(pool.size)
        }
    }

    private suspend inline fun report(crossinline block: EventReporter<T>.() -> Unit) {
        withContext(reportDispatcher) {
            eventReporter.block()
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
        while (pool.isNotEmpty() && !pool.peekFirst().isAlive(lifetimeInMillis)) {
            val entry = pool.removeFirst()
            report {
                onRemove(entry.value)
                onChangeSize(pool.size)
            }
            numberDeletedValues++
        }
        numberDeletedValues
    }

    override suspend fun poll(): T? = poolMutex.withLock {
        pool.pollFirst()?.value?.also { value ->
            report {
                onPoll(value)
                onChangeSize(pool.size)
            }
        }
    }?.also { fetchNewValue() }

    fun registerEventReporter(eventReporter: EventReporter<T>) {
        this.eventReporter = eventReporter
    }

    fun resetEventReporter() {
        this.eventReporter = EventReporter.NONE
    }

    fun cancel() {
        scope.cancel()
    }

    class Entry<T>(val value: T, private val creationTime: Long = System.currentTimeMillis()) {
        fun isAlive(lifeTime: Long): Boolean {
            return System.currentTimeMillis() - creationTime <= lifeTime
        }
    }

    fun interface ValueProvider<out T> {
        suspend fun provide(): T
    }

    interface EventReporter<in T> {
        fun onAdd(value: T) = Unit
        fun onRemove(value: T) = Unit
        fun onPoll(value: T) = Unit
        fun onChangeSize(value: Int) = Unit
        fun onError(e: Throwable) = Unit

        companion object {
            val NONE = object : EventReporter<Any?> {  }
        }
    }

    companion object {
        private const val CLEANING_LOOP_DELAY = 1000L
    }
}
