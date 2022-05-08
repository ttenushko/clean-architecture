package com.ttenushko.cleanarchitecture.utils.task

import com.ttenushko.cleanarchitecture.utils.Cancellable

public interface Task<P : Any, R : Any> {
    public fun execute(param: P, callback: Callback<R>): Cancellable

    public interface Callback<R : Any> {
        public fun onResult(result: R)
        public fun onComplete()
        public fun onError(error: Throwable)
    }
}