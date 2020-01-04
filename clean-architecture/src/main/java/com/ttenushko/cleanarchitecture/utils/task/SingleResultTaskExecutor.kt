package com.ttenushko.cleanarchitecture.utils.task

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach

class SingleResultTaskExecutor<P : Any, R : Any, T>(
    val name: String,
    dispatcher: CoroutineDispatcher,
    private val creator: (P, T) -> SingleResultTask<R, T>,
    private val resultHandler: ((R, T) -> Unit)?,
    private val errorHandler: ((error: Throwable, T) -> Unit)?
) {
    private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())
    private var runningTask: SingleResultTask<R, T>? = null
    private val actor = coroutineScope.actor<Event<R, T>>(capacity = Channel.UNLIMITED) {
        channel.consumeEach { event -> handleEvent(event) }
    }
    private val taskListener = object : SingleResultTask.Listener<R, T> {
        override fun onComplete(task: SingleResultTask<R, T>, result: R, tag: T) {
            actor.offer(Event.complete(task, result, tag))
        }

        override fun onError(task: SingleResultTask<R, T>, error: Throwable, tag: T) {
            actor.offer(Event.fail(task, error, tag))
        }
    }
    val isRunning: Boolean
        get() {
            return synchronized(this@SingleResultTaskExecutor) {
                null != this.runningTask
            }
        }

    fun start(param: P, tag: T): Boolean =
        synchronized(this@SingleResultTaskExecutor) {
            if (null == runningTask) {
                runningTask = creator(param, tag)
                runningTask!!.addListener(taskListener)
                true
            } else false
        }

    fun stop(): Boolean =
        synchronized(this@SingleResultTaskExecutor) {
            runningTask?.let {
                it.removeListener(taskListener)
                it.cancel()
                runningTask = null
                true
            } ?: false
        }

    private fun handleEvent(event: Event<R, T>) {
        when (event.type) {
            Event.TYPE_COMPLETE -> {
                handleComplete(event.task!!, event.result!!, event.tag!!)
            }
            Event.TYPE_ERROR -> {
                handleError(event.task!!, event.error!!, event.tag!!)
            }
        }
    }

    private fun handleComplete(task: SingleResultTask<R, T>, result: R, tag: T) =
        synchronized(this@SingleResultTaskExecutor) {
            if (this.runningTask === task) {
                task.removeListener(taskListener)
                this.runningTask = null
                true
            } else false
        }.also { notifySucceed ->
            if (notifySucceed) {
                resultHandler?.invoke(result, tag)
            }
        }

    private fun handleError(task: SingleResultTask<R, T>, error: Throwable, tag: T) =
        synchronized(this@SingleResultTaskExecutor) {
            if (this.runningTask === task) {
                task.removeListener(taskListener)
                this.runningTask = null
                true
            } else false
        }.also { notifyFail ->
            if (notifyFail) {
                errorHandler?.invoke(error, tag)
            }
        }

    private data class Event<R : Any, T>(
        val type: Int,
        val task: SingleResultTask<R, T>?,
        val result: R? = null,
        val error: Throwable? = null,
        val tag: T? = null
    ) {
        companion object {
            const val TYPE_COMPLETE = 1
            const val TYPE_ERROR = 2

            fun <R : Any, T> complete(
                task: SingleResultTask<R, T>,
                result: R,
                tag: T
            ): Event<R, T> =
                Event(
                    type = TYPE_COMPLETE,
                    task = task,
                    result = result,
                    tag = tag
                )

            fun <R : Any, T> fail(
                task: SingleResultTask<R, T>,
                error: Throwable,
                tag: T
            ): Event<R, T> =
                Event(
                    type = TYPE_ERROR,
                    task = task,
                    error = error,
                    tag = tag
                )
        }
    }
}