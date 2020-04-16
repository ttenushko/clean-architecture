package com.ttenushko.cleanarchitecture.domain.usecase

import com.ttenushko.cleanarchitecture.domain.common.Cancellable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

internal fun CoroutineScope.asCancellable(): Cancellable =
    object : Cancellable {
        override val isCancelled: Boolean
            get() = this@asCancellable.coroutineContext[Job]?.isCancelled ?: false

        override fun cancel() {
            this@asCancellable.cancel()
        }
    }