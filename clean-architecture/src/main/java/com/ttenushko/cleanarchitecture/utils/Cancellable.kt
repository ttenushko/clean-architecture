package com.ttenushko.cleanarchitecture.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

public interface Cancellable {
    public val isCancelled: Boolean
    public fun cancel()
}

internal fun CoroutineScope.asCancellable(): Cancellable =
    object : Cancellable {
        override val isCancelled: Boolean
            get() = this@asCancellable.coroutineContext[Job]?.isCancelled ?: false

        override fun cancel() {
            this@asCancellable.cancel()
        }
    }