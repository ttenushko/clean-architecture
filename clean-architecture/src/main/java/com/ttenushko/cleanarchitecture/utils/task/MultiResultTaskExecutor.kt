package com.ttenushko.cleanarchitecture.utils.task

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach

class MultiResultTaskExecutor<P : Any, R : Any, T>(
    val name: String,
    dispatcher: CoroutineDispatcher,
    private val creator: (P, T) -> MultiResultTask<R, T>,
    private val resultHandler: ((R, T) -> Unit)?,
    private val errorHandler: ((error: Throwable, T) -> Unit)?,
    private val completeHandler: ((T) -> Unit)? = null
) {
    private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())
    private var runningTask: MultiResultTask<R, T>? = null
    private val actor = coroutineScope.actor<Event<R, T>>(capacity = Channel.UNLIMITED) {
        channel.consumeEach { event -> handleEvent(event) }
    }
    private val taskListener = object : MultiResultTask.Listener<R, T> {
        override fun onResult(task: MultiResultTask<R, T>, result: R, tag: T) {
            actor.offer(Event.result(task, result, tag))
        }

        override fun onComplete(task: MultiResultTask<R, T>, tag: T) {
            actor.offer(Event.complete(task, tag))
        }

        override fun onError(task: MultiResultTask<R, T>, error: Throwable, tag: T) {
            actor.offer(Event.error(task, error, tag))
        }
    }
    val isRunning: Boolean
        get() {
            return synchronized(this@MultiResultTaskExecutor) {
                null != this.runningTask
            }
        }

    fun start(param: P, tag: T): Boolean =
        synchronized(this@MultiResultTaskExecutor) {
            if (null == runningTask) {
                runningTask = creator(param, tag)
                runningTask!!.addListener(taskListener)
                true
            } else false
        }

    fun stop(): Boolean =
        synchronized(this@MultiResultTaskExecutor) {
            runningTask?.let {
                it.removeListener(taskListener)
                it.cancel()
                runningTask = null
                true
            } ?: false
        }

    private fun handleEvent(event: Event<R, T>) {
        when (event.type) {
            Event.TYPE_RESULT -> {
                handleResult(event.task!!, event.result!!, event.tag!!)
            }
            Event.TYPE_COMPLETE -> {
                handleComplete(event.task!!, event.tag!!)
            }
            Event.TYPE_ERROR -> {
                handleError(event.task!!, event.error!!, event.tag!!)
            }
        }
    }

    private fun handleResult(task: MultiResultTask<R, T>, result: R, tag: T) =
        synchronized(this@MultiResultTaskExecutor) {
            this.runningTask === task
        }.also { notifyResult ->
            if (notifyResult) {
                resultHandler?.invoke(result, tag)
            }
        }

    private fun handleComplete(task: MultiResultTask<R, T>, tag: T) =
        synchronized(this@MultiResultTaskExecutor) {
            if (this.runningTask === task) {
                task.removeListener(taskListener)
                this.runningTask = null
                true
            } else false
        }.also { notifySucceed ->
            if (notifySucceed) {
                completeHandler?.invoke(tag)
            }
        }

    private fun handleError(task: MultiResultTask<R, T>, error: Throwable, tag: T) =
        synchronized(this@MultiResultTaskExecutor) {
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
        val task: MultiResultTask<R, T>?,
        val result: R? = null,
        val error: Throwable? = null,
        val tag: T? = null
    ) {
        companion object {
            const val TYPE_RESULT = 1
            const val TYPE_COMPLETE = 2
            const val TYPE_ERROR = 3

            fun <R : Any, T> result(
                task: MultiResultTask<R, T>,
                result: R,
                tag: T
            ): Event<R, T> =
                Event(
                    type = TYPE_RESULT,
                    task = task,
                    result = result,
                    tag = tag
                )

            fun <R : Any, T> complete(
                task: MultiResultTask<R, T>,
                tag: T
            ): Event<R, T> =
                Event(
                    type = TYPE_COMPLETE,
                    task = task,
                    tag = tag
                )

            fun <R : Any, T> error(
                task: MultiResultTask<R, T>,
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