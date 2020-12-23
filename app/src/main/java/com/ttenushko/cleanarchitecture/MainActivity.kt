package com.ttenushko.cleanarchitecture

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ttenushko.cleanarchitecture.domain.usecase.*
import com.ttenushko.cleanarchitecture.utils.task.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.newSingleThreadContext

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "test"
    }

    private val taskExecutorFactory = TaskExecutorFactory.create(lifecycleScope)
    private val customDispatcher = newSingleThreadContext("MyOwnThread")
    private val taskSingle =
        taskExecutorFactory.createTaskExecutor<MySingleResultUseCase.Param, MySingleResultUseCase.Result, Unit>(
            singleResultUseCaseTaskProvider() { _, _ ->
                MySingleResultUseCaseImpl(customDispatcher)
            },
            { result, _ -> Log.d(TAG, "Result=$result, thread=${Thread.currentThread()}") },
            { error, _ -> Log.d(TAG, "Error=$error, thread=${Thread.currentThread()}") }
        )

    private val taskMulti =
        taskExecutorFactory.createTaskExecutor<MyMultiResultUseCase.Param, MyMultiResultUseCase.Result, Unit>(
            multiResultUseCaseTaskProvider() { _, _ ->
                MyMultiResultUseCaseImpl2(customDispatcher)
            },
            { result, _ -> Log.d(TAG, "Result=$result, thread=${Thread.currentThread()}") },
            { error, _ -> Log.d(TAG, "Error=$error, thread=${Thread.currentThread()}") },
            { _ -> Log.d(TAG, "Complete, thread=${Thread.currentThread()}") }
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        taskSingle.start(MySingleResultUseCase.Param(3_000L, 23), Unit)
        taskMulti.start(MyMultiResultUseCase.Param(listOf(10, 30, 60, 100, 120, 200), 700L), Unit)

    }

    override fun onDestroy() {
        super.onDestroy()
        taskSingle.stop()
    }
}

interface MySingleResultUseCase :
    SingleResultUseCase<MySingleResultUseCase.Param, MySingleResultUseCase.Result> {
    data class Param(val delay: Long, val result: Int)
    data class Result(val res: Int)
}

class MySingleResultUseCaseImpl(
    dispatcher: CoroutineDispatcher
) : MySingleResultUseCase,
    CoroutineSingleResultUseCase<MySingleResultUseCase.Param, MySingleResultUseCase.Result>(
        dispatcher
    ) {

    companion object {
        private const val TAG = "test"
    }

    override suspend fun run(param: MySingleResultUseCase.Param): MySingleResultUseCase.Result {
        Log.d(TAG, "Started thread=${Thread.currentThread()}")
        Log.d(TAG, "Continued thread=${Thread.currentThread()}")
        delay(param.delay)
        return MySingleResultUseCase.Result(param.result).also {
            Log.d(TAG, "Finished thread=${Thread.currentThread()}")
        }
        //Log.d(TAG, "Finished on: ${Thread.currentThread()}")
    }
}

interface MyMultiResultUseCase :
    MultiResultUseCase<MyMultiResultUseCase.Param, MyMultiResultUseCase.Result> {
    data class Param(val values: List<Int>, val delay: Long)
    data class Result(val res: Int)
}

class MyMultiResultUseCaseImpl(
    dispatcher: CoroutineDispatcher
) : CoroutineMultiResultUseCase<MyMultiResultUseCase.Param, MyMultiResultUseCase.Result>(dispatcher) {

    companion object {
        private const val TAG = "test"
    }

    override suspend fun run(
        param: MyMultiResultUseCase.Param,
        channel: SendChannel<MyMultiResultUseCase.Result>
    ) {
        Log.d(TAG, "Started thread=${Thread.currentThread()}")
        param.values.forEach {
            Log.d(TAG, "Offer thread=${Thread.currentThread()}")
            channel.offer(MyMultiResultUseCase.Result(it))
            delay(param.delay)
        }
        Log.d(TAG, "Finished thread=${Thread.currentThread()}")
    }
}

class MyMultiResultUseCaseImpl2(
    dispatcher: CoroutineDispatcher
) : FlowMultiResultUseCase<MyMultiResultUseCase.Param, MyMultiResultUseCase.Result>(dispatcher) {

    companion object {
        private const val TAG = "test"
    }

    override fun createFlow(param: MyMultiResultUseCase.Param): Flow<MyMultiResultUseCase.Result> {
        Log.d(TAG, "Started thread=${Thread.currentThread()}")
        return flowOf(*param.values.toTypedArray())
            .map {
                Log.d(TAG, "Offer thread=${Thread.currentThread()}")
                delay(param.delay)
                MyMultiResultUseCase.Result(it)
            }.also {
                Log.d(TAG, "Finished thread=${Thread.currentThread()}")
            }
    }
}
