package com.ttenushko.cleanarchitecture.utils.task

import com.ttenushko.cleanarchitecture.domain.common.Cancellable
import com.ttenushko.cleanarchitecture.domain.usecase.MultiResultUseCase
import com.ttenushko.cleanarchitecture.domain.usecase.SingleResultUseCase


fun <P : Any, R : Any, T> singleResultUseCaseTaskProvider(useCaseProvider: (P, T) -> SingleResultUseCase<P, R>): TaskProvider<P, R, T> =
    object : TaskProvider<P, R, T> {
        override fun provide(param: P, tag: T): Task<P, R> =
            useCaseProvider(param, tag).asTask()
    }

fun <P : Any, R : Any, T> multiResultUseCaseTaskProvider(useCaseProvider: (P, T) -> MultiResultUseCase<P, R>): TaskProvider<P, R, T> =
    object : TaskProvider<P, R, T> {
        override fun provide(param: P, tag: T): Task<P, R> =
            useCaseProvider(param, tag).asTask()
    }

fun <P : Any, R : Any> MultiResultUseCase<P, R>.asTask(): Task<P, R> =
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

fun <P : Any, R : Any> SingleResultUseCase<P, R>.asTask(): Task<P, R> =
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
