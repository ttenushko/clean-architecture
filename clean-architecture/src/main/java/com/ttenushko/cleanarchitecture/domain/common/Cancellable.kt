package com.ttenushko.cleanarchitecture.domain.common

interface Cancellable {
    val isCancelled: Boolean
    fun cancel()
}
