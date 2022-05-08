package com.ttenushko.cleanarchitecture.utils.task

import kotlinx.coroutines.CoroutineScope


public interface TaskExecutor<P : Any, R : Any, T> {
    public var startHandler: ((T) -> Unit)?
    public var resultHandler: ((R, T) -> Unit)?
    public var completeHandler: ((T) -> Unit)?
    public var errorHandler: ((Throwable, T) -> Unit)?
    public val isRunning: Boolean

    public fun start(param: P, tag: T): Boolean
    public fun stop(): Boolean

    public companion object {
        public fun <P : Any, R : Any, T> create(
            coroutineScope: CoroutineScope,
            task: Task<P, R>
        ): TaskExecutor<P, R, T> =
            CoroutineTaskExecutor(coroutineScope, task)
    }
}

public fun <P : Any, R : Any, T> createTaskExecutor(
    coroutineScope: CoroutineScope,
    task: Task<P, R>,
    startHandler: ((T) -> Unit)? = null,
    resultHandler: ((R, T) -> Unit)? = null,
    completeHandler: ((T) -> Unit)? = null,
    errorHandler: ((error: Throwable, T) -> Unit)? = null
): TaskExecutor<P, R, T> =
    CoroutineTaskExecutor<P, R, T>(coroutineScope, task)
        .apply {
            this.startHandler = startHandler
            this.resultHandler = resultHandler
            this.completeHandler = completeHandler
            this.errorHandler = errorHandler
        }