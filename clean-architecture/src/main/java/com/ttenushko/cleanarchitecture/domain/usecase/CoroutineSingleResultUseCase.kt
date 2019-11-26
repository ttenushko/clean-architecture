package com.ttenushko.cleanarchitecture.domain.usecase

import com.ttenushko.cleanarchitecture.domain.common.Cancellable
import kotlinx.coroutines.*

abstract class CoroutineSingleResultUseCase<P : Any, R : Any>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : SingleResultUseCase<P, R> {

    @Suppress("DeferredResultUnused")
    final override fun execute(param: P, callback: SingleResultUseCase.Callback<R>): Cancellable =
        CoroutineScope(dispatcher + SupervisorJob()).let { coroutineScope ->
            coroutineScope.async {
                try {
                    val result = run(param)
                    callback.onComplete(result)
                } catch (error: Throwable) {
                    callback.onError(error)
                }
            }
            object : Cancellable {
                override val isCancelled: Boolean
                    get() = coroutineScope.coroutineContext[Job]?.isCancelled ?: false

                override fun cancel() {
                    coroutineScope.cancel()
                }
            }
        }

    protected abstract suspend fun run(param: P): R
}
