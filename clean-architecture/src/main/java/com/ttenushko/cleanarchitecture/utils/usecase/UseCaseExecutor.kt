package com.ttenushko.cleanarchitecture.utils.usecase

import kotlinx.coroutines.CoroutineScope

interface UseCaseExecutor<P : Any, R : Any, T> {
    var resultHandler: ((R, T) -> Unit)?
    var completeHandler: ((T) -> Unit)?
    var errorHandler: ((Throwable, T) -> Unit)?
    val isRunning: Boolean

    fun start(param: P, tag: T): Boolean
    fun stop(): Boolean

    companion object {
        fun <P : Any, R : Any, T> create(
            coroutineScope: CoroutineScope,
            useCaseProvider: SingleResultUseCaseProvider<P, R, T>
        ): UseCaseExecutor<P, R, T> =
            CoroutineSingleResultUseCaseExecutor(coroutineScope) { param, tag ->
                useCaseProvider.provide(param, tag)
            }

        fun <P : Any, R : Any, T> create(
            coroutineScope: CoroutineScope,
            useCaseProvider: MultiResultUseCaseProvider<P, R, T>
        ): UseCaseExecutor<P, R, T> =
            CoroutineMultiResultUseCaseExecutor(coroutineScope) { param, tag ->
                useCaseProvider.provide(param, tag)
            }

        fun <P : Any, R : Any, T> create(useCaseProvider: SingleResultUseCaseProvider<P, R, T>): UseCaseExecutor<P, R, T> =
            DefaultSingleResultUseCaseExecutor() { param, tag ->
                useCaseProvider.provide(param, tag)
            }

        fun <P : Any, R : Any, T> create(useCaseProvider: MultiResultUseCaseProvider<P, R, T>): UseCaseExecutor<P, R, T> =
            DefaultMultiResultUseCaseExecutor() { param, tag ->
                useCaseProvider.provide(param, tag)
            }
    }
}

fun <P : Any, R : Any, T> createExecutor(
    coroutineScope: CoroutineScope,
    useCaseProvider: SingleResultUseCaseProvider<P, R, T>,
    resultHandler: ((R, T) -> Unit)? = null,
    errorHandler: ((error: Throwable, T) -> Unit)? = null,
    completeHandler: ((T) -> Unit)? = null
): UseCaseExecutor<P, R, T> =
    (CoroutineSingleResultUseCaseExecutor<P, R, T>(coroutineScope) { param, tag ->
        useCaseProvider.provide(param, tag)
    }).apply {
        this.resultHandler = resultHandler
        this.errorHandler = errorHandler
        this.completeHandler = completeHandler
    }

fun <P : Any, R : Any, T> createExecutor(
    coroutineScope: CoroutineScope,
    useCaseProvider: MultiResultUseCaseProvider<P, R, T>,
    resultHandler: ((R, T) -> Unit)? = null,
    errorHandler: ((error: Throwable, T) -> Unit)? = null,
    completeHandler: ((T) -> Unit)? = null
): UseCaseExecutor<P, R, T> =
    (CoroutineMultiResultUseCaseExecutor<P, R, T>(coroutineScope) { param, tag ->
        useCaseProvider.provide(param, tag)
    }).apply {
        this.resultHandler = resultHandler
        this.errorHandler = errorHandler
        this.completeHandler = completeHandler
    }

fun <P : Any, R : Any, T> createExecutor(
    useCaseProvider: SingleResultUseCaseProvider<P, R, T>,
    resultHandler: ((R, T) -> Unit)? = null,
    errorHandler: ((error: Throwable, T) -> Unit)? = null,
    completeHandler: ((T) -> Unit)? = null
): UseCaseExecutor<P, R, T> =
    (DefaultSingleResultUseCaseExecutor<P, R, T> { param, tag ->
        useCaseProvider.provide(param, tag)
    }).apply {
        this.resultHandler = resultHandler
        this.errorHandler = errorHandler
        this.completeHandler = completeHandler
    }

fun <P : Any, R : Any, T> createExecutor(
    useCaseProvider: MultiResultUseCaseProvider<P, R, T>,
    resultHandler: ((R, T) -> Unit)? = null,
    errorHandler: ((error: Throwable, T) -> Unit)? = null,
    completeHandler: ((T) -> Unit)? = null
): UseCaseExecutor<P, R, T> =
    (DefaultMultiResultUseCaseExecutor<P, R, T> { param, tag ->
        useCaseProvider.provide(param, tag)
    }).apply {
        this.resultHandler = resultHandler
        this.errorHandler = errorHandler
        this.completeHandler = completeHandler
    }
