package com.ttenushko.cleanarchitecture.utils.task

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class CoroutineSingleResultTaskExecutor<P : Any, R : Any, T>(
    private val coroutineScope: CoroutineScope,
    private val taskProvider: (P, T) -> SingleResultTask<R>
) : TaskExecutor<P, R, T> {

    @Volatile
    override var resultHandler: ((R, T) -> Unit)? = null

    @Volatile
    override var completeHandler: ((T) -> Unit)? = null

    @Volatile
    override var errorHandler: ((Throwable, T) -> Unit)? = null
    override val isRunning: Boolean
        get() = synchronized(lock) { null != taskContext }
    private val lock = Any()

    @Volatile
    private var taskContext: TaskContext? = null

    override fun start(param: P, tag: T): Boolean =
        synchronized(lock) {
            if (null == taskContext) {
                val context = TaskContext(null).also { taskContext = it }
                context.job = coroutineScope.launch {
                    try {
                        onTaskComplete(
                            context,
                            taskProvider(param, tag).execute(),
                            tag
                        )
                    } catch (error: Throwable) {
                        if (error !is CancellationException) {
                            onTaskError(context, error, tag)
                        } else {
                            onTaskCancelled(context)
                        }
                    }
                }
                true
            } else false
        }

    override fun stop(): Boolean =
        synchronized(lock) {
            val context = taskContext
            if (null != context) {
                taskContext = null
                context.job?.cancel()
                true
            } else false
        }

    private fun onTaskComplete(ctx: TaskContext, result: R, tag: T) {
        synchronized(lock) {
            if (ctx === taskContext) {
                taskContext = null
                Unit
            } else null
        }?.also {
            resultHandler?.invoke(result, tag)
            completeHandler?.invoke(tag)
        }
    }

    private fun onTaskError(ctx: TaskContext, error: Throwable, tag: T) {
        synchronized(lock) {
            if (ctx === taskContext) {
                taskContext = null
                Unit
            } else null
        }?.also {
            errorHandler?.invoke(error, tag)
        }
    }

    private fun onTaskCancelled(ctx: TaskContext) {
        synchronized(lock) {
            if (ctx === taskContext) {
                taskContext = null
                Unit
            } else null
        }
    }

    private class TaskContext(
        @Volatile
        var job: Job?
    )
}
