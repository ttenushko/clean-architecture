package com.ttenushko.cleanarchitecture.utils.task

interface SingleResultTaskProvider<P : Any, R : Any, T> {
    fun provide(param: P, tag: T): SingleResultTask<R>
}

fun <P : Any, R : Any, T> singleResultTaskProvider(taskProvider: (P, T) -> SingleResultTask<R>): SingleResultTaskProvider<P, R, T> =
    object : SingleResultTaskProvider<P, R, T> {
        override fun provide(param: P, tag: T): SingleResultTask<R> =
            taskProvider(param, tag)
    }
