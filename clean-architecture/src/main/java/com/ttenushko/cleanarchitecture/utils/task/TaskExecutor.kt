package com.ttenushko.cleanarchitecture.utils.task

import kotlinx.coroutines.CoroutineScope

interface TaskExecutor<P : Any, R : Any, T> {
    var resultHandler: ((R, T) -> Unit)?
    var completeHandler: ((T) -> Unit)?
    var errorHandler: ((Throwable, T) -> Unit)?
    val isRunning: Boolean

    fun start(param: P, tag: T): Boolean
    fun stop(): Boolean

    companion object {
        fun <P : Any, R : Any, T> create(
            coroutineScope: CoroutineScope,
            taskProvider: MultiResultTaskProvider<P, R, T>
        ): TaskExecutor<P, R, T> =
            CoroutineMultiResultTaskExecutor(coroutineScope) { param, tag ->
                taskProvider.provide(param, tag)
            }

        fun <P : Any, R : Any, T> create(
            coroutineScope: CoroutineScope,
            taskProvider: SingleResultTaskProvider<P, R, T>
        ): TaskExecutor<P, R, T> =
            CoroutineSingleResultTaskExecutor(coroutineScope) { param, tag ->
                taskProvider.provide(param, tag)
            }
    }
}

fun <P : Any, R : Any, T> createTask(
    coroutineScope: CoroutineScope,
    taskProvider: SingleResultTaskProvider<P, R, T>,
    resultHandler: ((R, T) -> Unit)? = null,
    errorHandler: ((error: Throwable, T) -> Unit)? = null,
    completeHandler: ((T) -> Unit)? = null
): TaskExecutor<P, R, T> =
    (CoroutineSingleResultTaskExecutor<P, R, T>(coroutineScope) { param, tag ->
        taskProvider.provide(param, tag)
    }).apply {
        this.resultHandler = resultHandler
        this.errorHandler = errorHandler
        this.completeHandler = completeHandler
    }

fun <P : Any, R : Any, T> createTask(
    coroutineScope: CoroutineScope,
    taskProvider: MultiResultTaskProvider<P, R, T>,
    resultHandler: ((R, T) -> Unit)? = null,
    errorHandler: ((error: Throwable, T) -> Unit)? = null,
    completeHandler: ((T) -> Unit)? = null
): TaskExecutor<P, R, T> =
    (CoroutineMultiResultTaskExecutor<P, R, T>(coroutineScope) { param, tag ->
        taskProvider.provide(param, tag)
    }).apply {
        this.resultHandler = resultHandler
        this.errorHandler = errorHandler
        this.completeHandler = completeHandler
    }
