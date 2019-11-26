package com.ttenushko.cleanarchitecture.utils.task

import com.ttenushko.cleanarchitecture.domain.common.Cancellable
import com.ttenushko.cleanarchitecture.domain.usecase.MultiResultUseCase
import java.util.concurrent.CopyOnWriteArraySet

class MultiResultUseCaseTask<R : Any, T>(
    creator: (MultiResultUseCase.Callback<R>) -> Cancellable,
    private val tag: T
) : MultiResultTask<R, T> {

    companion object {
        private const val STATE_RUNNING = 1
        private const val STATE_COMPLETE = 2
        private const val STATE_ERROR = 3
    }

    @Volatile
    private var state = STATE_RUNNING
    @Volatile
    private var taskResult: R? = null
    @Volatile
    private lateinit var taskError: Throwable
    private val events = ValueQueueDrain<Event<R, T>> { event -> handleEvent(event) }
    private val listeners = CopyOnWriteArraySet<MultiResultTask.Listener<R, T>>()
    private val useCaseCancellable: Cancellable
    private val useCaseCallback = object : MultiResultUseCase.Callback<R> {
        override fun onResult(result: R) {
            events.drain(Event.result(result))
        }

        override fun onComplete() {
            events.drain(Event.succeed())
        }

        override fun onError(error: Throwable) {
            events.drain(Event.fail(error))
        }
    }
    override val isCancelled: Boolean
        get() = useCaseCancellable.isCancelled
    override val isComplete: Boolean
        get() = state.let { (STATE_COMPLETE == it || STATE_ERROR == it) }
    override val isSucceed: Boolean
        get() = (STATE_COMPLETE == state)
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

    override fun addListener(listener: MultiResultTask.Listener<R, T>) {
        events.drain(Event.addListener(listener))
    }

    override fun removeListener(listener: MultiResultTask.Listener<R, T>) {
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
            Event.TYPE_RESULT -> {
                handleResult(event.result as R)
            }
            Event.TYPE_COMPLETE -> {
                handleComplete()
            }
            Event.TYPE_ERROR -> {
                handleError(event.error!!)
            }
            else -> {
                throw IllegalArgumentException("Unsupported event type: ${event.type}")
            }
        }
    }

    private fun handleAddListener(listener: MultiResultTask.Listener<R, T>) {
        if (listeners.add(listener)) {
            when (state) {
                STATE_RUNNING -> {
                    taskResult?.let { listener.onResult(this@MultiResultUseCaseTask, it, tag) }
                }
                STATE_COMPLETE -> {
                    listener.onComplete(this@MultiResultUseCaseTask, tag)
                }
                STATE_ERROR -> {
                    listener.onError(this@MultiResultUseCaseTask, taskError, tag)
                }
            }
        }
    }

    private fun handleResult(result: R) {
        check(STATE_RUNNING == state)
        taskResult = result
        listeners.forEach { it.onResult(this@MultiResultUseCaseTask, result, tag) }
    }

    private fun handleComplete() {
        check(STATE_RUNNING == state)
        taskResult = null
        state = STATE_COMPLETE
        listeners.forEach { it.onComplete(this@MultiResultUseCaseTask, tag) }
    }

    private fun handleError(error: Throwable) {
        check(STATE_RUNNING == state)
        taskResult = null
        taskError = error
        state = STATE_ERROR
        listeners.forEach { it.onError(this@MultiResultUseCaseTask, error, tag) }
    }

    // TODO: add object pool
    private data class Event<R : Any, T>(
        val type: Int,
        val listener: MultiResultTask.Listener<R, T>? = null,
        val result: R? = null,
        val error: Throwable? = null
    ) {
        companion object {
            const val TYPE_ADD_LISTENER = 1
            const val TYPE_RESULT = 3
            const val TYPE_COMPLETE = 4
            const val TYPE_ERROR = 5

            fun <R : Any, T> addListener(listener: MultiResultTask.Listener<R, T>): Event<R, T> =
                Event(
                    type = TYPE_ADD_LISTENER,
                    listener = listener
                )

            fun <R : Any, T> result(result: R): Event<R, T> =
                Event(
                    type = TYPE_RESULT,
                    result = result
                )

            fun <R : Any, T> succeed(): Event<R, T> =
                Event(
                    type = TYPE_COMPLETE
                )

            fun <R : Any, T> fail(error: Throwable): Event<R, T> =
                Event(
                    type = TYPE_ERROR,
                    error = error
                )
        }
    }
}

fun <P : Any, R : Any, T> MultiResultUseCase<P, R>.asMultiResultTask(
    param: P,
    tag: T
): MultiResultTask<R, T> =
    MultiResultUseCaseTask(
        { callback -> this.execute(param, callback) },
        tag
    )