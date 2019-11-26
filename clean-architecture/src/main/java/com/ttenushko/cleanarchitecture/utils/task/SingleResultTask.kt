package com.ttenushko.cleanarchitecture.utils.task

import com.ttenushko.cleanarchitecture.domain.common.Cancellable

interface SingleResultTask<R : Any, T> : Cancellable {
    val isComplete: Boolean
    val isSucceed: Boolean
    val result: R
    val error: Throwable

    fun addListener(listener: Listener<R, T>)
    fun removeListener(listener: Listener<R, T>)

    interface Listener<R : Any, T> {
        fun onComplete(task: SingleResultTask<R, T>, result: R, tag: T)
        fun onError(task: SingleResultTask<R, T>, error: Throwable, tag: T)
    }
}