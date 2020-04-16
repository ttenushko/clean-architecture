package com.ttenushko.cleanarchitecture.domain.usecase

import com.ttenushko.cleanarchitecture.domain.common.Cancellable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
abstract class FlowMultiResultUseCase<P : Any, R : Any>(
    private val dispatcher: CoroutineDispatcher
) : MultiResultUseCase<P, R> {

    final override fun execute(param: P, callback: MultiResultUseCase.Callback<R>): Cancellable =
        CoroutineScope(dispatcher + Job()).let { coroutineScope ->
            coroutineScope.launch {
                try {
                    createFlow(param)
                        .collect { callback.onResult(it) }
                    callback.onComplete()
                } catch (error: Throwable) {
                    callback.onError(error)
                }
            }
            coroutineScope.asCancellable()
        }

    protected abstract fun createFlow(param: P): Flow<R>
}
