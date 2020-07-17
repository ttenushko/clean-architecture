package com.ttenushko.cleanarchitecture.utils.usecase

import com.ttenushko.cleanarchitecture.domain.usecase.MultiResultUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect

internal class MultiResultUseCaseExecutor<P : Any, R : Any, T>(
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
        get() = synchronized(lock) { null != useCaseJob }
    private val lock = Any()
    private var useCaseJob: Job? = null

    override fun start(param: P, tag: T): Boolean =
        synchronized(lock) {
            if (null == useCaseJob) {
                useCaseJob = coroutineScope.launch {
                    try {
                        useCaseProvider(param, tag).execute(param)
                            .collect { result ->
                                onUseCaseResult(coroutineContext[Job]!!, result, tag)
                            }
                        onUseCaseComplete(coroutineContext[Job]!!, tag)
                    } catch (error: Throwable) {
                        if (error !is CancellationException) {
                            onUseCaseError(coroutineContext[Job]!!, error, tag)
                        } else {
                            onUseCaseCancelled(coroutineContext[Job]!!)
                        }
                    }
                }
                true
            } else false
        }

    override fun stop(): Boolean =
        synchronized(lock) {
            val job = useCaseJob
            if (null != job) {
                useCaseJob = null
                job.cancel()
                true
            } else false
        }

    private fun onUseCaseResult(job: Job, result: R, tag: T) {
        synchronized(lock) {
            if (job === useCaseJob) Unit
            else null
        }?.also {
            resultHandler?.invoke(result, tag)
        }
    }

    private fun onUseCaseComplete(job: Job, tag: T) {
        synchronized(lock) {
            if (job === useCaseJob) {
                useCaseJob = null
                Unit
            } else null
        }?.also {
            completeHandler?.invoke(tag)
        }
    }

    private fun onUseCaseError(job: Job, error: Throwable, tag: T) {
        synchronized(lock) {
            if (job === useCaseJob) {
                useCaseJob = null
                Unit
            } else null
        }?.also {
            errorHandler?.invoke(error, tag)
        }
    }

    private fun onUseCaseCancelled(job: Job) {
        synchronized(lock) {
            if (job === useCaseJob) {
                useCaseJob = null
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
}