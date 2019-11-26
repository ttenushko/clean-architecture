package com.ttenushko.cleanarchitecture.domain.usecase

import com.ttenushko.cleanarchitecture.domain.common.Cancellable
import io.reactivex.Observable
import io.reactivex.observers.DisposableObserver

abstract class RxMultiResultUseCase<P : Any, R : Any> : MultiResultUseCase<P, R> {

    override fun execute(param: P, callback: MultiResultUseCase.Callback<R>): Cancellable =
        Executor(createObservable(param), callback)

    abstract fun createObservable(param: P): Observable<R>

    private class Executor<R : Any>(
        observable: Observable<R>,
        private val callback: MultiResultUseCase.Callback<R>
    ) : Cancellable {

        override val isCancelled: Boolean
            get() = disposable.isDisposed

        private val disposable = observable.subscribeWith(object : DisposableObserver<R>() {

            override fun onNext(result: R) {
                callback.onResult(result)
            }

            override fun onComplete() {
                callback.onComplete()
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