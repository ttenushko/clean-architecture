package com.ttenushko.cleanarchitecture.utils.usecase

import kotlinx.coroutines.CoroutineScope

interface UseCaseExecutorFactory {
    fun <P : Any, R : Any, T> createExecutor(useCaseProvider: SingleResultUseCaseProvider<P, R, T>): UseCaseExecutor<P, R, T>
    fun <P : Any, R : Any, T> createExecutor(useCaseProvider: MultiResultUseCaseProvider<P, R, T>): UseCaseExecutor<P, R, T>

    companion object {
        fun create(coroutineScope: CoroutineScope): UseCaseExecutorFactory =
            object : UseCaseExecutorFactory {
                override fun <P : Any, R : Any, T> createExecutor(useCaseProvider: SingleResultUseCaseProvider<P, R, T>): UseCaseExecutor<P, R, T> =
                    CoroutineSingleResultUseCaseExecutor(coroutineScope) { param, tag ->
                        useCaseProvider.provide(param, tag)
                    }

                override fun <P : Any, R : Any, T> createExecutor(useCaseProvider: MultiResultUseCaseProvider<P, R, T>): UseCaseExecutor<P, R, T> =
                    CoroutineMultiResultUseCaseExecutor(coroutineScope) { param, tag ->
                        useCaseProvider.provide(param, tag)
                    }
            }

        fun create(): UseCaseExecutorFactory =
            object : UseCaseExecutorFactory {
                override fun <P : Any, R : Any, T> createExecutor(useCaseProvider: SingleResultUseCaseProvider<P, R, T>): UseCaseExecutor<P, R, T> =
                    DefaultSingleResultUseCaseExecutor() { param, tag ->
                        useCaseProvider.provide(param, tag)
                    }

                override fun <P : Any, R : Any, T> createExecutor(useCaseProvider: MultiResultUseCaseProvider<P, R, T>): UseCaseExecutor<P, R, T> =
                    DefaultMultiResultUseCaseExecutor() { param, tag ->
                        useCaseProvider.provide(param, tag)
                    }
            }
    }
}

fun <P : Any, R : Any, T> UseCaseExecutorFactory.createExecutor(
    useCaseProvider: SingleResultUseCaseProvider<P, R, T>,
    resultHandler: ((R, T) -> Unit)? = null,
    errorHandler: ((error: Throwable, T) -> Unit)? = null,
    completeHandler: ((T) -> Unit)? = null
): UseCaseExecutor<P, R, T> =
    createExecutor(useCaseProvider).apply {
        this.resultHandler = resultHandler
        this.errorHandler = errorHandler
        this.completeHandler = completeHandler
    }

fun <P : Any, R : Any, T> UseCaseExecutorFactory.createExecutor(
    useCaseProvider: MultiResultUseCaseProvider<P, R, T>,
    resultHandler: ((R, T) -> Unit)? = null,
    errorHandler: ((error: Throwable, T) -> Unit)? = null,
    completeHandler: ((T) -> Unit)? = null
): UseCaseExecutor<P, R, T> =
    createExecutor(useCaseProvider).apply {
        this.resultHandler = resultHandler
        this.errorHandler = errorHandler
        this.completeHandler = completeHandler
    }