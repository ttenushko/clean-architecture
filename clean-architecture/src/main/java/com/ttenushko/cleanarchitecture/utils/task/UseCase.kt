package com.ttenushko.cleanarchitecture.utils.task

import com.ttenushko.cleanarchitecture.domain.usecase.MultiResultUseCase
import com.ttenushko.cleanarchitecture.domain.usecase.SingleResultUseCase
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

@Suppress("EXPERIMENTAL_API_USAGE")
fun <P : Any, R : Any, T> multiResultUseCaseTaskProvider(useCaseProvider: (P, T) -> MultiResultUseCase<P, R>): MultiResultTaskProvider<P, R, T> =
    object : MultiResultTaskProvider<P, R, T> {
        override fun provide(param: P, tag: T): MultiResultTask<R> =
            object : MultiResultTask<R> {
                override fun execute(): Flow<R> =
                    useCaseProvider(param, tag).asFlow(param)
            }
    }

@Suppress("EXPERIMENTAL_API_USAGE")
fun <P : Any, R : Any, T> singleResultUseCaseTaskProvider(useCaseProvider: (P, T) -> SingleResultUseCase<P, R>): SingleResultTaskProvider<P, R, T> =
    object : SingleResultTaskProvider<P, R, T> {
        override fun provide(param: P, tag: T): SingleResultTask<R> =
            object : SingleResultTask<R> {
                override suspend fun execute(): R =
                    useCaseProvider(param, tag).asSuspend(param)
            }
    }

@Suppress("EXPERIMENTAL_API_USAGE")
fun <P : Any, R : Any> MultiResultUseCase<P, R>.asFlow(param: P): Flow<R> =
    callbackFlow {
        val cancellable =
            this@asFlow.execute(param, object : MultiResultUseCase.Callback<R> {
                override fun onResult(result: R) {
                    try {
                        sendBlocking(result)
                    } catch (error: Exception) {
                        // ignore
                    }
                }

                override fun onError(error: Throwable) {
                    cancel("Error occurred", error)
                }

                override fun onComplete() {
                    channel.close()
                }
            })
        awaitClose { cancellable.cancel() }
    }

@Suppress("EXPERIMENTAL_API_USAGE")
suspend fun <P : Any, R : Any> SingleResultUseCase<P, R>.asSuspend(param: P): R =
    suspendCancellableCoroutine { cont ->
        val cancellable = execute(
            param,
            object : SingleResultUseCase.Callback<R> {
                override fun onComplete(result: R) {
                    cont.resumeWith(Result.success(result))
                }

                override fun onError(error: Throwable) {
                    cont.resumeWith(Result.failure(error))
                }
            })
        cont.invokeOnCancellation { cancellable.cancel() }
    }
