package com.ttenushko.cleanarchitecture.utils.task

import java.util.*
import java.util.concurrent.atomic.AtomicInteger

internal class ValueQueueDrain<T>(private val consumer: (T) -> Unit) {
    private val queue: Queue<T> = LinkedList<T>()
    private val wip = AtomicInteger()

    fun drain(value: T) {
        queue.offer(value)
        if (0 == wip.getAndIncrement()) {
            do {
                wip.set(1)
                var v = queue.poll()
                while (null != v) {
                    consumer(v)
                    v = queue.poll()
                }
            } while (0 != wip.decrementAndGet())
        }
    }
}