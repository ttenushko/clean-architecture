package com.ttenushko.cleanarchitecture.utils.task

import kotlinx.coroutines.flow.Flow

interface MultiResultTask<R> {
    fun execute(): Flow<R>
}