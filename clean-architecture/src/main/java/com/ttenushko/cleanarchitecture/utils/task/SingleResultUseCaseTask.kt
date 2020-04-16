package com.ttenushko.cleanarchitecture.utils.task

import com.ttenushko.cleanarchitecture.domain.common.Cancellable
import com.ttenushko.cleanarchitecture.domain.usecase.SingleResultUseCase
import java.util.concurrent.ConcurrentHashMap

class SingleResultUseCaseTask<R : Any, T>(
    creator: (SingleResultUseCase.Callback<R>) -> Cancellable,
    private val tag: T
) : SingleResultTask<R, T> {

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
    override val result: R
        get() = when (val state = this.state) {
            is State.Running -> throw IllegalStateException("Task is running")
            is State.Completed -> state.result
            is State.Failed -> throw IllegalStateException("Task failed")
            else -> throw IllegalStateException("Unexpected state")
        }
    override val error: Throwable
        get() = when (val state = this.state) {
            is State.Running -> throw IllegalStateException("Task is running")
            is State.Completed -> throw IllegalStateException("Task is complete")
            is State.Failed -> state.error
            else -> throw IllegalStateException("Unexpected state")
        }
    @Volatile
    private var state: State<R> = State.Running
    private val listeners = ConcurrentHashMap<SingleResultTask.Listener<R, T>, State<R>?>()
    private val useCaseCancellable: Cancellable
    private val useCaseCallback = object : SingleResultUseCase.Callback<R> {
        override fun onComplete(result: R) {
            eventHandler.drain(Event.UpdateState(State.Completed(result)))
        }

        override fun onError(error: Throwable) {
            eventHandler.drain(Event.UpdateState(State.Failed(error)))
        }
    }

    init {
        useCaseCancellable = creator(useCaseCallback)
    }

    override fun addListener(listener: SingleResultTask.Listener<R, T>) {
        if (null == listeners.putIfAbsent(listener, State.Unknown)) {
            eventHandler.drain(Event.NotifyListeners)
        }
    }

    override fun removeListener(listener: SingleResultTask.Listener<R, T>) {
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
                    is State.Running -> {
                        // do nothing
                    }
                    is State.Completed -> {
                        entry.key.onComplete(this@SingleResultUseCaseTask, state.result, tag)
                    }
                    is State.Failed -> {
                        entry.key.onError(this@SingleResultUseCaseTask, state.error, tag)
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
                        is State.Running -> require(currentState is State.Running)
                        is State.Completed -> require(currentState is State.Running)
                        is State.Failed -> require(currentState is State.Running)
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
        object Running : State<Nothing>()
        data class Completed<R>(val result: R) : State<R>()
        data class Failed(val error: Throwable) : State<Nothing>()
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