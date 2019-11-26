package com.ttenushko.cleanarchitecture.utils.task

import com.ttenushko.cleanarchitecture.domain.common.Cancellable
import com.ttenushko.cleanarchitecture.domain.usecase.SingleResultUseCase
import java.util.concurrent.CopyOnWriteArraySet

class SingleResultUseCaseTask<R : Any, T>(
    creator: (SingleResultUseCase.Callback<R>) -> Cancellable,
    private val tag: T
) : SingleResultTask<R, T> {

    companion object {
        private const val STATE_RUNNING = 1
        private const val STATE_COMPLETE = 2
        private const val STATE_ERROR = 3
    }

    @Volatile
    private var state = STATE_RUNNING
    @Volatile
    private lateinit var taskResult: R
    @Volatile
    private lateinit var taskError: Throwable
    private val events = ValueQueueDrain<Event<R, T>> { event -> handleEvent(event) }
    private val listeners = CopyOnWriteArraySet<SingleResultTask.Listener<R, T>>()
    private val useCaseCancellable: Cancellable
    private val useCaseCallback = object : SingleResultUseCase.Callback<R> {
        override fun onComplete(result: R) {
            events.drain(Event.complete(result))
        }

        override fun onError(error: Throwable) {
            events.drain(Event.error(error))
        }
    }
    override val isCancelled: Boolean
        get() = useCaseCancellable.isCancelled
    override val isComplete: Boolean
        get() = state.let { (STATE_COMPLETE == it || STATE_ERROR == it) }
    override val isSucceed: Boolean
        get() = (STATE_COMPLETE == state)
    override val result: R
        get() = when (val state = this.state) {
            STATE_COMPLETE -> taskResult
            STATE_RUNNING -> throw IllegalStateException("Task is running")
            STATE_ERROR -> throw IllegalStateException("Task failed")
            else -> throw IllegalStateException("Unknown task state: $state")
        }
    override val error: Throwable
        get() = when (val state = this.state) {
            STATE_ERROR -> taskError
            STATE_RUNNING -> throw IllegalStateException("Task is running")
            STATE_COMPLETE -> throw IllegalStateException("Task is complete")
            else -> throw IllegalStateException("Unknown task state: $state")
        }

    init {
        useCaseCancellable = creator(useCaseCallback)
    }

    override fun addListener(listener: SingleResultTask.Listener<R, T>) {
        events.drain(Event.addListener(listener))
    }

    override fun removeListener(listener: SingleResultTask.Listener<R, T>) {
        listeners.remove(listener)
    }

    override fun cancel() {
        useCaseCancellable.cancel()
    }

    private fun handleEvent(event: Event<R, T>) {
        when (event.type) {
            Event.TYPE_ADD_LISTENER -> {
                handleAddListener(event.listener!!)
            }
            Event.TYPE_COMPLETE -> {
                handleComplete(event.result!!)
            }
            Event.TYPE_ERROR -> {
                handleError(event.error!!)
            }
            else -> {
                throw IllegalArgumentException("Unsupported event type: ${event.type}")
            }
        }
    }

    private fun handleAddListener(listener: SingleResultTask.Listener<R, T>) {
        if (listeners.add(listener)) {
            when (state) {
                STATE_RUNNING -> {
                    // do nothing
                }
                STATE_COMPLETE -> {
                    listener.onComplete(this@SingleResultUseCaseTask, taskResult, tag)
                }
                STATE_ERROR -> {
                    listener.onError(this@SingleResultUseCaseTask, taskError, tag)
                }
            }
        }
    }

    private fun handleComplete(result: R) {
        check(STATE_RUNNING == state)
        taskResult = result
        state = STATE_COMPLETE
        listeners.forEach { it.onComplete(this@SingleResultUseCaseTask, result, tag) }
    }

    private fun handleError(error: Throwable) {
        check(STATE_RUNNING == state)
        taskError = error
        state = STATE_ERROR
        listeners.forEach { it.onError(this@SingleResultUseCaseTask, error, tag) }
    }

    // TODO: add object pool
    private data class Event<R : Any, T>(
        val type: Int,
        val listener: SingleResultTask.Listener<R, T>? = null,
        val result: R? = null,
        val error: Throwable? = null
    ) {
        companion object {
            const val TYPE_ADD_LISTENER = 1
            const val TYPE_COMPLETE = 2
            const val TYPE_ERROR = 3

            fun <R : Any, T> addListener(callback: SingleResultTask.Listener<R, T>): Event<R, T> =
                Event(
                    type = TYPE_ADD_LISTENER,
                    listener = callback
                )

            fun <R : Any, T> complete(result: R): Event<R, T> =
                Event(
                    type = TYPE_COMPLETE,
                    result = result
                )

            fun <R : Any, T> error(error: Throwable): Event<R, T> =
                Event(
                    type = TYPE_ERROR,
                    error = error
                )
        }
    }
}

fun <P : Any, R : Any, T> SingleResultUseCase<P, R>.asSingleResultTask(
    param: P,
    tag: T
): SingleResultTask<R, T> =
    SingleResultUseCaseTask(
        { callback -> this.execute(param, callback) },
        tag
    )