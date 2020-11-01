package com.ttenushko.cleanarchitecture.utils.task

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Suppress("EXPERIMENTAL_API_USAGE")
internal class CoroutineMultiResultTaskExecutor<P : Any, R : Any, T>(
    private val coroutineScope: CoroutineScope,
    private val taskProvider: (P, T) -> MultiResultTask<R>
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
                        taskProvider(param, tag).execute()
                            .collect { result ->
                                onTaskResult(context, result, tag)
                            }
                        onTaskComplete(context, tag)
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

    private fun onTaskResult(ctx: TaskContext, result: R, tag: T) {
        synchronized(lock) {
            if (ctx === taskContext) Unit
            else null
        }?.also {
            resultHandler?.invoke(result, tag)
        }
    }

    private fun onTaskComplete(ctx: TaskContext, tag: T) {
        synchronized(lock) {
            if (ctx === taskContext) {
                taskContext = null
                Unit
            } else null
        }?.also {
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
