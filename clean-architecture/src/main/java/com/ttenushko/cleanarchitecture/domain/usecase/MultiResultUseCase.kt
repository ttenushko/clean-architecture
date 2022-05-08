package com.ttenushko.cleanarchitecture.domain.usecase

import com.ttenushko.cleanarchitecture.utils.Cancellable

public interface MultiResultUseCase<P : Any, R : Any> {

    public fun execute(param: P, callback: Callback<R>): Cancellable

    public interface Callback<R : Any> {
        public fun onResult(result: R)
        public fun onError(error: Throwable)
        public fun onComplete()
    }
}