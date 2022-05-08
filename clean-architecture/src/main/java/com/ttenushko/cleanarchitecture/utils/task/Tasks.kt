package com.ttenushko.cleanarchitecture.utils.task

import com.ttenushko.cleanarchitecture.domain.usecase.MultiResultUseCase
import com.ttenushko.cleanarchitecture.domain.usecase.SingleResultUseCase
import com.ttenushko.cleanarchitecture.utils.Cancellable
import com.ttenushko.cleanarchitecture.utils.asCancellable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow

public fun <P : Any, R : Any> MultiResultUseCase<P, R>.asTask(): Task<P, R> =
    object : Task<P, R> {
        override fun execute(param: P, callback: Task.Callback<R>): Cancellable =
            this@asTask.execute(param, object : MultiResultUseCase.Callback<R> {
                override fun onResult(result: R) {
                    callback.onResult(result)
                }

                override fun onComplete() {
                    callback.onComplete()
                }

                override fun onError(error: Throwable) {
                    callback.onError(error)
                }
            })
    }

public fun <P : Any, R : Any> SingleResultUseCase<P, R>.asTask(): Task<P, R> =
    object : Task<P, R> {
        override fun execute(param: P, callback: Task.Callback<R>): Cancellable =
            this@asTask.execute(param, object : SingleResultUseCase.Callback<R> {
                override fun onComplete(result: R) {
                    callback.onResult(result)
                    callback.onComplete()
                }

                override fun onError(error: Throwable) {
                    callback.onError(error)
                }
            })
    }

public fun <P : Any, R : Any> ((P) -> Flow<R>).asTask(dispatcher: CoroutineDispatcher): Task<P, R> =
    object : Task<P, R> {
        override fun execute(param: P, callback: Task.Callback<R>): Cancellable =
            CoroutineScope(dispatcher + Job()).let { coroutineScope ->
                coroutineScope.launch {
                    try {
                        this@asTask(param).collect {
                            callback.onResult(it)
                        }
                        callback.onComplete()
                    } catch (error: Throwable) {
                        callback.onError(error)
                    }
                }.also { job ->
                    job.invokeOnCompletion { coroutineScope.cancel() }
                }
                coroutineScope.asCancellable()
            }
    }

public fun <P : Any, R : Any> (suspend (P) -> R).asTask(dispatcher: CoroutineDispatcher): Task<P, R> =
    object : Task<P, R> {
        override fun execute(param: P, callback: Task.Callback<R>): Cancellable =
            CoroutineScope(dispatcher + Job()).let { coroutineScope ->
                coroutineScope.launch {
                    try {
                        val result = this@asTask(param)
                        callback.onResult(result)
                        callback.onComplete()
                    } catch (error: Throwable) {
                        callback.onError(error)
                    }
                }.also { job ->
                    job.invokeOnCompletion { coroutineScope.cancel() }
                }
                coroutineScope.asCancellable()
            }
    }