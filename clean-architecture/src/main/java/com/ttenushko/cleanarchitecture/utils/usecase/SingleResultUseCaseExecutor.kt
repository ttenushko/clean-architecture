package com.ttenushko.cleanarchitecture.utils.usecase

import com.ttenushko.cleanarchitecture.domain.usecase.SingleResultUseCase
import kotlinx.coroutines.*

internal class SingleResultUseCaseExecutor<P : Any, R : Any, T>(
    private val coroutineScope: CoroutineScope,
    private val useCaseProvider: (P, T) -> SingleResultUseCase<P, R>
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
                        onUseCaseComplete(
                            coroutineContext[Job]!!,
                            useCaseProvider(param, tag).execute(param),
                            tag
                        )
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

    private fun onUseCaseComplete(job: Job, result: R, tag: T) {
        synchronized(lock) {
            if (job === useCaseJob) {
                useCaseJob = null
                Unit
            } else null
        }?.also {
            resultHandler?.invoke(result, tag)
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

    private suspend fun <P : Any, R : Any> SingleResultUseCase<P, R>.execute(param: P): R =
        suspendCancellableCoroutine { cont ->
            val cancellable = execute(param, object : SingleResultUseCase.Callback<R> {
                override fun onComplete(result: R) {
                    cont.resumeWith(Result.success(result))
                }

                override fun onError(error: Throwable) {
                    cont.resumeWith(Result.failure(error))
                }
            })
            cont.invokeOnCancellation { cancellable.cancel() }
        }
}