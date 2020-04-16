package com.ttenushko.cleanarchitecture.domain.usecase

import com.ttenushko.cleanarchitecture.domain.common.Cancellable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class CoroutineSingleResultUseCase<P : Any, R : Any>(
    private val dispatcher: CoroutineDispatcher
) : SingleResultUseCase<P, R> {

    final override fun execute(param: P, callback: SingleResultUseCase.Callback<R>): Cancellable =
        CoroutineScope(dispatcher + Job()).let { coroutineScope ->
            coroutineScope.launch {
                try {
                    val result = run(param)
                    callback.onComplete(result)
                } catch (error: Throwable) {
                    callback.onError(error)
                }
            }
            coroutineScope.asCancellable()
        }

    protected abstract suspend fun run(param: P): R
}
