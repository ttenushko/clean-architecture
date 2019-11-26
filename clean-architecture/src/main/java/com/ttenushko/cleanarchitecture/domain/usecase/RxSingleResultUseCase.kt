package com.ttenushko.cleanarchitecture.domain.usecase

import com.ttenushko.cleanarchitecture.domain.common.Cancellable
import io.reactivex.Single
import io.reactivex.observers.DisposableSingleObserver


abstract class RxSingleResultUseCase<P : Any, R : Any> : SingleResultUseCase<P, R> {

    override fun execute(param: P, callback: SingleResultUseCase.Callback<R>): Cancellable =
        Executor(createSingle(param), callback)

    abstract fun createSingle(param: P): Single<R>

    private class Executor<R : Any>(
        single: Single<R>,
        private val callback: SingleResultUseCase.Callback<R>
    ) : Cancellable {

        override val isCancelled: Boolean
            get() = disposable.isDisposed

        private val disposable = single.subscribeWith(object : DisposableSingleObserver<R>() {
            override fun onSuccess(result: R) {
                callback.onComplete(result)
            }

            override fun onError(error: Throwable) {
                callback.onError(error)
            }
        })

        override fun cancel() {
            disposable.dispose()
        }
    }
}