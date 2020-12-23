package com.ttenushko.cleanarchitecture.utils.task

interface TaskProvider<P : Any, R : Any, T> {
    fun provide(param: P, tag: T): Task<P, R>
}