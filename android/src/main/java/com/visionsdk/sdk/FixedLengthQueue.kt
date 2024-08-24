package io.packagex.visionsdk

internal class FixedLengthQueue<T>(val capacity: Int): Iterable<T> {

    private val innerQueue = ArrayDeque<T>(capacity)

    fun push(t: T) {
        if (innerQueue.size >= capacity) {
            pop()
        }
        innerQueue.addFirst(t)
    }

    fun pop() = innerQueue.removeLast()

    fun popOrNull() = innerQueue.removeLastOrNull()

    fun first() = innerQueue.first()

    fun firstOrNull() = innerQueue.firstOrNull()

    fun last() = innerQueue.last()

    fun lastOrNull() = innerQueue.lastOrNull()

    fun isFull() = innerQueue.size == capacity

    fun toList() = innerQueue.toList()

    fun clear() { innerQueue.clear() }

    val size: Int get() = innerQueue.size

    override fun iterator(): Iterator<T> {

        return object : Iterator<T> {

            var index = 0

            override fun hasNext(): Boolean {
                return index < capacity
            }

            override fun next(): T {
                return innerQueue[index++]
            }
        }
    }
}