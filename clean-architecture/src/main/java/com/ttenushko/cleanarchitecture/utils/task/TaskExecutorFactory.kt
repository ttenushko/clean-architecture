package com.ttenushko.cleanarchitecture.utils.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

interface TaskExecutorFactory {
    fun <P : Any, R : Any, T> createTaskExecutor(taskProvider: TaskProvider<P, R, T>): TaskExecutor<P, R, T>

    companion object {
        fun create(coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)): TaskExecutorFactory =
            object : TaskExecutorFactory {
                override fun <P : Any, R : Any, T> createTaskExecutor(taskProvider: TaskProvider<P, R, T>): TaskExecutor<P, R, T> =
                    CoroutineTaskExecutor(coroutineScope) { param, tag ->
                        taskProvider.provide(param, tag)
                    }
            }
    }
}

fun <P : Any, R : Any, T> TaskExecutorFactory.createTaskExecutor(
    taskProvider: TaskProvider<P, R, T>,
    resultHandler: ((R, T) -> Unit)? = null,
    errorHandler: ((error: Throwable, T) -> Unit)? = null,
    completeHandler: ((T) -> Unit)? = null
): TaskExecutor<P, R, T> =
    this@createTaskExecutor.createTaskExecutor(taskProvider).apply {
        this.resultHandler = resultHandler
        this.errorHandler = errorHandler
        this.completeHandler = completeHandler
    }
