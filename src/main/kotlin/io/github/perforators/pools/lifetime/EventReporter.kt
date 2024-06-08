package io.github.perforators.pools.lifetime

interface EventReporter<in T> {
    fun onAdd(value: T) = Unit
    fun onClean(value: T) = Unit
    fun onPoll(value: T) = Unit
    fun onChangeSize(value: Int) = Unit
    fun onError(e: Throwable) = Unit
}
