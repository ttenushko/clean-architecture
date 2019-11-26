package com.ttenushko.cleanarchitecture.utils.task

import com.ttenushko.cleanarchitecture.domain.common.Cancellable

interface MultiResultTask<R : Any, T> : Cancellable {
    val isComplete: Boolean
    val isSucceed: Boolean
    val error: Throwable

    fun addListener(listener: Listener<R, T>)
    fun removeListener(listener: Listener<R, T>)

    interface Listener<R : Any, T> {
        fun onResult(task: MultiResultTask<R, T>, result: R, tag: T)
        fun onComplete(task: MultiResultTask<R, T>, tag: T)
        fun onError(task: MultiResultTask<R, T>, error: Throwable, tag: T)
    }
}