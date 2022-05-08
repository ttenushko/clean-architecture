package com.ttenushko.cleanarchitecture.utils

import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

public class CloseHandler(private val handler: () -> Unit) : Closeable {

    private val _isClosed = AtomicBoolean(false)
    public val isClosed: Boolean
        get() = _isClosed.get()

    public fun checkNotClosed() {
        if (_isClosed.get()) {
            throw IllegalStateException("This instance is closed")
        }
    }

    override fun close() {
        if (!_isClosed.getAndSet(true)) {
            handler.invoke()
        }
    }
}