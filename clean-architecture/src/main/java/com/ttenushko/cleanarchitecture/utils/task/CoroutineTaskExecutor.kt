package com.ttenushko.cleanarchitecture.utils.task

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect

@Suppress("EXPERIMENTAL_API_USAGE")
internal class CoroutineTaskExecutor<P : Any, R : Any, T>(
    private val coroutineScope: CoroutineScope,
    private val taskProvider: (P, T) -> Task<P, R>
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
                        taskProvider(param, tag)
                            .execute(param)
                            .collect { onTaskResult(context, it, tag) }
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
            } else null
        }?.also {
            completeHandler?.invoke(tag)
        }
    }

    private fun onTaskError(ctx: TaskContext, error: Throwable, tag: T) {
        synchronized(lock) {
            if (ctx === taskContext) {
                taskContext = null
            } else null
        }?.also {
            errorHandler?.invoke(error, tag)
        }
    }

    private fun onTaskCancelled(ctx: TaskContext) {
        synchronized(lock) {
            if (ctx === taskContext) {
                taskContext = null
            } else null
        }
    }

    private fun <P : Any, R : Any> Task<P, R>.execute(param: P): Flow<R> =
        callbackFlow {
            val cancellable = this@execute.execute(param, object : Task.Callback<R> {
                override fun onResult(result: R) {
                    try {
                        sendBlocking(result)
                    } catch (error: Exception) {
                        // ignore
                    }
                }

                override fun onComplete() {
                    channel.close()
                }

                override fun onError(error: Throwable) {
                    cancel("Error occurred.", error)
                }
            })
            awaitClose { cancellable.cancel() }
        }

    private class TaskContext(
        @Volatile
        var job: Job?
    )
}
