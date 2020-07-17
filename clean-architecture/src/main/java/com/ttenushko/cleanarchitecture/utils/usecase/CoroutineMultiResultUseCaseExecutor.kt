package com.ttenushko.cleanarchitecture.utils.usecase

import com.ttenushko.cleanarchitecture.domain.usecase.MultiResultUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect

internal class CoroutineMultiResultUseCaseExecutor<P : Any, R : Any, T>(
    private val coroutineScope: CoroutineScope,
    private val useCaseProvider: (P, T) -> MultiResultUseCase<P, R>
) : UseCaseExecutor<P, R, T> {

    @Volatile
    override var resultHandler: ((R, T) -> Unit)? = null

    @Volatile
    override var completeHandler: ((T) -> Unit)? = null

    @Volatile
    override var errorHandler: ((Throwable, T) -> Unit)? = null
    override val isRunning: Boolean
        get() = synchronized(lock) { null != useCaseContext }
    private val lock = Any()

    @Volatile
    private var useCaseContext: UseCaseContext? = null

    override fun start(param: P, tag: T): Boolean =
        synchronized(lock) {
            if (null == useCaseContext) {
                val context = UseCaseContext(null).also { useCaseContext = it }
                context.job = coroutineScope.launch {
                    try {
                        useCaseProvider(param, tag).execute(param)
                            .collect { result ->
                                onUseCaseResult(context, result, tag)
                            }
                        onUseCaseComplete(context, tag)
                    } catch (error: Throwable) {
                        if (error !is CancellationException) {
                            onUseCaseError(context, error, tag)
                        } else {
                            onUseCaseCancelled(context)
                        }
                    }
                }
                true
            } else false
        }

    override fun stop(): Boolean =
        synchronized(lock) {
            val context = useCaseContext
            if (null != context) {
                useCaseContext = null
                context.job?.cancel()
                true
            } else false
        }

    private fun onUseCaseResult(ctx: UseCaseContext, result: R, tag: T) {
        synchronized(lock) {
            if (ctx === useCaseContext) Unit
            else null
        }?.also {
            resultHandler?.invoke(result, tag)
        }
    }

    private fun onUseCaseComplete(ctx: UseCaseContext, tag: T) {
        synchronized(lock) {
            if (ctx === useCaseContext) {
                useCaseContext = null
                Unit
            } else null
        }?.also {
            completeHandler?.invoke(tag)
        }
    }

    private fun onUseCaseError(ctx: UseCaseContext, error: Throwable, tag: T) {
        synchronized(lock) {
            if (ctx === useCaseContext) {
                useCaseContext = null
                Unit
            } else null
        }?.also {
            errorHandler?.invoke(error, tag)
        }
    }

    private fun onUseCaseCancelled(ctx: UseCaseContext) {
        synchronized(lock) {
            if (ctx === useCaseContext) {
                useCaseContext = null
                Unit
            } else null
        }
    }

    private fun <P : Any, R : Any> MultiResultUseCase<P, R>.execute(param: P): Flow<R> =
        callbackFlow {
            val cancellable = execute(param, object : MultiResultUseCase.Callback<R> {
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

    private class UseCaseContext(
        @Volatile
        var job: Job?
    )
}