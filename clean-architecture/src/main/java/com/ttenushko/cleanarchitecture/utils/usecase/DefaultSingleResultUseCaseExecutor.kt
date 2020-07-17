package com.ttenushko.cleanarchitecture.utils.usecase

import com.ttenushko.cleanarchitecture.domain.common.Cancellable
import com.ttenushko.cleanarchitecture.domain.usecase.SingleResultUseCase

internal class DefaultSingleResultUseCaseExecutor<P : Any, R : Any, T>(
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
                try {
                    context.cancellable = useCaseProvider(param, tag)
                        .execute(
                            param,
                            object : SingleResultUseCase.Callback<R> {
                                override fun onComplete(result: R) {
                                    onUseCaseComplete(context, result, tag)
                                }

                                override fun onError(error: Throwable) {
                                    onUseCaseError(context, error, tag)
                                }
                            })
                    Result.success(true)
                } catch (error: Throwable) {
                    Result.failure<Boolean>(error)
                }
            } else Result.success(false)
        }.fold(
            onSuccess = { success -> success },
            onFailure = { error ->
                errorHandler?.invoke(error, tag)
                true
            }
        )

    override fun stop(): Boolean =
        synchronized(lock) {
            val context = useCaseContext
            if (null != context) {
                useCaseContext = null
                context.cancellable?.cancel()
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

    private data class UseCaseContext(
        @Volatile
        var cancellable: Cancellable?
    )
}