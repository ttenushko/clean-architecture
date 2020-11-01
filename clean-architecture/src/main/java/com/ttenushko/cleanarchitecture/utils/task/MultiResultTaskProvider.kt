package com.ttenushko.cleanarchitecture.utils.task

interface MultiResultTaskProvider<P : Any, R : Any, T> {
    fun provide(param: P, tag: T): MultiResultTask<R>
}

fun <P : Any, R : Any, T> multiResultTaskProvider(taskProvider: (P, T) -> MultiResultTask<R>): MultiResultTaskProvider<P, R, T> =
    object : MultiResultTaskProvider<P, R, T> {
        override fun provide(param: P, tag: T): MultiResultTask<R> =
            taskProvider(param, tag)
    }
