package com.ttenushko.cleanarchitecture.utils.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

interface TaskExecutorFactory {
    fun <P : Any, R : Any, T> createExecutor(useCaseProvider: SingleResultTaskProvider<P, R, T>): TaskExecutor<P, R, T>
    fun <P : Any, R : Any, T> createExecutor(useCaseProvider: MultiResultTaskProvider<P, R, T>): TaskExecutor<P, R, T>

    companion object {
        fun create(coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)): TaskExecutorFactory =
            object : TaskExecutorFactory {
                override fun <P : Any, R : Any, T> createExecutor(useCaseProvider: SingleResultTaskProvider<P, R, T>): TaskExecutor<P, R, T> =
                    CoroutineSingleResultTaskExecutor(coroutineScope) { param, tag ->
                        useCaseProvider.provide(param, tag)
                    }

                override fun <P : Any, R : Any, T> createExecutor(useCaseProvider: MultiResultTaskProvider<P, R, T>): TaskExecutor<P, R, T> =
                    CoroutineMultiResultTaskExecutor(coroutineScope) { param, tag ->
                        useCaseProvider.provide(param, tag)
                    }
            }
    }
}

fun <P : Any, R : Any, T> TaskExecutorFactory.createExecutor(
    taskProvider: SingleResultTaskProvider<P, R, T>,
    resultHandler: ((R, T) -> Unit)? = null,
    errorHandler: ((error: Throwable, T) -> Unit)? = null,
    completeHandler: ((T) -> Unit)? = null
): TaskExecutor<P, R, T> =
    createExecutor(taskProvider).apply {
        this.resultHandler = resultHandler
        this.errorHandler = errorHandler
        this.completeHandler = completeHandler
    }

fun <P : Any, R : Any, T> TaskExecutorFactory.createExecutor(
    taskProvider: MultiResultTaskProvider<P, R, T>,
    resultHandler: ((R, T) -> Unit)? = null,
    errorHandler: ((error: Throwable, T) -> Unit)? = null,
    completeHandler: ((T) -> Unit)? = null
): TaskExecutor<P, R, T> =
    createExecutor(taskProvider).apply {
        this.resultHandler = resultHandler
        this.errorHandler = errorHandler
        this.completeHandler = completeHandler
    }
