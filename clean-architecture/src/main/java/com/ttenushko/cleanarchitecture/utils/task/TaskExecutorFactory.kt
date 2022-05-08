package com.ttenushko.cleanarchitecture.utils.task

import kotlinx.coroutines.CoroutineScope

public interface TaskExecutorFactory {
    public fun <P : Any, R : Any, T> createTaskExecutor(task: Task<P, R>): TaskExecutor<P, R, T>

    public companion object {
        public fun create(coroutineScope: CoroutineScope): TaskExecutorFactory =
            object : TaskExecutorFactory {
                override fun <P : Any, R : Any, T> createTaskExecutor(task: Task<P, R>): TaskExecutor<P, R, T> =
                    CoroutineTaskExecutor(coroutineScope, task)
            }
    }
}

public fun <P : Any, R : Any, T> TaskExecutorFactory.createTaskExecutor(
    task: Task<P, R>,
    startHandler: ((T) -> Unit)? = null,
    resultHandler: ((R, T) -> Unit)? = null,
    completeHandler: ((T) -> Unit)? = null,
    errorHandler: ((error: Throwable, T) -> Unit)? = null
): TaskExecutor<P, R, T> =
    this@createTaskExecutor.createTaskExecutor<P, R, T>(task).apply {
        this.startHandler = startHandler
        this.resultHandler = resultHandler
        this.completeHandler = completeHandler
        this.errorHandler = errorHandler
    }
