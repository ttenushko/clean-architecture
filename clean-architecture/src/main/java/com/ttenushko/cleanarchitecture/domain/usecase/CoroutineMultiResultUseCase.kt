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
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val channelCapacity: Int = Channel.UNLIMITED
) : MultiResultUseCase<P, R> {

    @Suppress("DeferredResultUnused")
    final override fun execute(param: P, callback: MultiResultUseCase.Callback<R>): Cancellable =
        CoroutineScope(dispatcher + SupervisorJob()).let { coroutineScope ->
            val actor = coroutineScope.actor<R>(capacity = channelCapacity) {
                consumeEach { result -> callback.onResult(result) }
            }
            coroutineScope.async {
                try {
                    run(param, actor)
                    callback.onComplete()
                } catch (error: Throwable) {
                    callback.onError(error)
                }
            }
            object : Cancellable {
                override val isCancelled: Boolean
                    get() = coroutineScope.coroutineContext[Job]?.isCancelled ?: false

                override fun cancel() {
                    coroutineScope.cancel()
                }
            }
        }

    protected abstract suspend fun run(param: P, channel: SendChannel<R>)
}
