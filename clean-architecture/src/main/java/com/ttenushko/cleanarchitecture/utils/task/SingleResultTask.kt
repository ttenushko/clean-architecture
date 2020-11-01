package com.ttenushko.cleanarchitecture.utils.task

interface SingleResultTask<R> {
    suspend fun execute(): R
}
