package com.ttenushko.cleanarchitecture.utils.task

import com.ttenushko.cleanarchitecture.domain.common.Cancellable
import com.ttenushko.cleanarchitecture.domain.usecase.MultiResultUseCase
import java.util.concurrent.ConcurrentHashMap

class MultiResultUseCaseTask<R : Any, T>(
    creator: (MultiResultUseCase.Callback<R>) -> Cancellable,
    private val tag: T
) : MultiResultTask<R, T> {

    override val isCancelled: Boolean
        get() = useCaseCancellable.isCancelled
    override val isComplete: Boolean
        get() = state.let { state ->
            state is State.Completed || state is State.Failed
        }
    override val isSucceed: Boolean
        get() = state.let { state ->
            state is State.Completed
        }
    override val error: Throwable
        get() = when (val state = this.state) {
            is State.Started -> throw IllegalStateException("Task is running")
            is State.Running -> throw IllegalStateException("Task is running")
            is State.Completed -> throw IllegalStateException("Task is complete")
            is State.Failed -> state.error
            else -> throw IllegalStateException("Unexpected state")
        }
    @Volatile
    private var state: State<R> = State.Started
    private val listeners = ConcurrentHashMap<MultiResultTask.Listener<R, T>, State<R>?>()
    private val useCaseCancellable: Cancellable
    private val useCaseCallback = object : MultiResultUseCase.Callback<R> {
        override fun onResult(result: R) {
            eventHandler.drain(Event.UpdateState(State.Running(result)))
        }

        override fun onComplete() {
            eventHandler.drain(Event.UpdateState(State.Completed))
        }

        override fun onError(error: Throwable) {
            eventHandler.drain(Event.UpdateState(State.Failed(error)))
        }
    }

    init {
        useCaseCancellable = creator(useCaseCallback)
    }

    override fun addListener(listener: MultiResultTask.Listener<R, T>) {
        if (null == listeners.putIfAbsent(listener, State.Unknown)) {
            eventHandler.drain(Event.NotifyListeners)
        }
    }

    override fun removeListener(listener: MultiResultTask.Listener<R, T>) {
        listeners.remove(listener)
    }

    override fun cancel() {
        useCaseCancellable.cancel()
    }

    private fun notifyListeners(state: State<R>) {
        listeners.entries.forEach { entry ->
            if (entry.value != state) {
                entry.setValue(state)
                when (state) {
                    is State.Started -> {
                        // do nothing
                    }
                    is State.Running -> {
                        entry.key.onResult(this@MultiResultUseCaseTask, state.result, tag)
                    }
                    is State.Completed -> {
                        entry.key.onComplete(this@MultiResultUseCaseTask, tag)
                    }
                    is State.Failed -> {
                        entry.key.onError(this@MultiResultUseCaseTask, state.error, tag)
                    }
                }
            }
        }
    }

    private val eventHandler = ValueQueueDrain<Event<R>> { event ->
        when (event) {
            is Event.NotifyListeners -> {
                notifyListeners(state)
            }
            is Event.UpdateState<R> -> {
                val currentState = this.state
                val newState = event.state
                if (currentState != newState) {
                    when (newState) {
                        is State.Started -> require(currentState is State.Started)
                        is State.Running -> require(currentState is State.Started || currentState is State.Running)
                        is State.Completed -> require(currentState is State.Started || currentState is State.Running)
                        is State.Failed -> require(currentState is State.Started || currentState is State.Running)
                    }
                    this.state = newState
                    notifyListeners(newState)
                }
            }
        }
    }

    private sealed class Event<out R> {
        object NotifyListeners : Event<Nothing>()
        data class UpdateState<R>(val state: State<R>) : Event<R>()
    }

    private sealed class State<out R> {
        object Unknown : State<Nothing>()
        object Started : State<Nothing>()
        data class Running<R>(val result: R) : State<R>()
        object Completed : State<Nothing>()
        data class Failed(val error: Throwable) : State<Nothing>()
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