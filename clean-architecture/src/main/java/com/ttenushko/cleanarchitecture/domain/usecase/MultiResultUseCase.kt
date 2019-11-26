package com.ttenushko.cleanarchitecture.domain.usecase

import com.ttenushko.cleanarchitecture.domain.common.Cancellable


interface MultiResultUseCase<P : Any, R : Any> {

    fun execute(param: P, callback: Callback<R>): Cancellable

    interface Callback<R : Any> {
        fun onResult(result: R)
        fun onError(error: Throwable)
        fun onComplete()
    }
}