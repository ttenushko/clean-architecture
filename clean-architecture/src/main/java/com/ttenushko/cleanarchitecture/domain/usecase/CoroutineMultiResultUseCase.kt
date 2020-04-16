package com.ttenushko.cleanarchitecture.domain.usecase

import com.ttenushko.cleanarchitecture.domain.common.Cancellable
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
abstract class CoroutineMultiResultUseCase<P : Any, R : Any>(
    private val dispatcher: CoroutineDispatcher,
    private val channelCapacity: Int = Channel.UNLIMITED
) : MultiResultUseCase<P, R> {

    final override fun execute(param: P, callback: MultiResultUseCase.Callback<R>): Cancellable =
        CoroutineScope(dispatcher + Job()).let { coroutineScope ->
            val actor = coroutineScope.actor<R>(capacity = channelCapacity) {
                consumeEach { result -> callback.onResult(result) }
            }
            coroutineScope.launch {
                try {
                    run(param, actor)
                    callback.onComplete()
                } catch (error: Throwable) {
                    callback.onError(error)
                }
            }
            coroutineScope.asCancellable()
        }

    protected abstract suspend fun run(param: P, channel: SendChannel<R>)
}
