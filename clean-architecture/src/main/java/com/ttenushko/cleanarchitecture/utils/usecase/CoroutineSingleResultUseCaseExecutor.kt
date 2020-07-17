package com.ttenushko.cleanarchitecture.utils.usecase

import com.ttenushko.cleanarchitecture.domain.usecase.SingleResultUseCase
import kotlinx.coroutines.*

internal class CoroutineSingleResultUseCaseExecutor<P : Any, R : Any, T>(
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
                        onUseCaseComplete(
                            context,
                            useCaseProvider(param, tag).execute(param),
                            tag
                        )
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

    private fun onUseCaseComplete(ctx: UseCaseContext, result: R, tag: T) {
        synchronized(lock) {
            if (ctx === useCaseContext) {
                useCaseContext = null
                Unit
            } else null
        }?.also {
            resultHandler?.invoke(result, tag)
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

    private class UseCaseContext(
        @Volatile
        var job: Job?
    )
}