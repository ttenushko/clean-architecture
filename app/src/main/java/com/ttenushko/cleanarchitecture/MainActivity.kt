package com.ttenushko.cleanarchitecture

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ttenushko.cleanarchitecture.domain.usecase.*
import com.ttenushko.cleanarchitecture.utils.task.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "test"
    }

    private val customDispatcher = newSingleThreadContext("MyOwnThread")
    private val taskSingle =
        SingleResultTaskExecutor<MySingleResultUseCase.Param, MySingleResultUseCase.Result, Unit>(
            "singleResultTask",
            Dispatchers.Main,
            { param, tag ->
                MySingleResultUseCaseImpl(customDispatcher).asSingleResultTask(
                    param,
                    tag
                )
            },
            { result, _ -> Log.d(TAG, "Result=$result, thread=${Thread.currentThread()}") },
            { error, _ -> Log.d(TAG, "Error=$error, thread=${Thread.currentThread()}") }
        )

    private val taskMulti =
        MultiResultTaskExecutor<MyMultiResultUseCase.Param, MyMultiResultUseCase.Result, Unit>(
            "multiResultTask",
            Dispatchers.Main,
            { param, tag ->
                MyMultiResultUseCaseImpl2(customDispatcher).asMultiResultTask(
                    param,
                    tag
                )
            },
            { result, _ -> Log.d(TAG, "Result=$result, thread=${Thread.currentThread()}") },
            { error, _ -> Log.d(TAG, "Error=$error, thread=${Thread.currentThread()}") },
            { _ -> Log.d(TAG, "Complete, thread=${Thread.currentThread()}") }
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        taskSingle.start(MySingleResultUseCase.Param(3_000L, 23), Unit)
//        taskMulti.start(MyMultiResultUseCase.Param(listOf(10, 30, 60, 100, 120, 200), 700L), Unit)

        val listener1 = object : MultiResultTask.Listener<MyMultiResultUseCase.Result, Unit> {
            override fun onResult(
                task: MultiResultTask<MyMultiResultUseCase.Result, Unit>,
                result: MyMultiResultUseCase.Result,
                tag: Unit
            ) {
                Log.d(TAG, "Listener1: onResult($result)")
            }

            override fun onComplete(
                task: MultiResultTask<MyMultiResultUseCase.Result, Unit>,
                tag: Unit
            ) {
                Log.d(TAG, "Listener1: onComplete()")
            }

            override fun onError(
                task: MultiResultTask<MyMultiResultUseCase.Result, Unit>,
                error: Throwable,
                tag: Unit
            ) {
                Log.d(TAG, "Listener1: onError($error)")
            }
        }
        val listener2 = object : MultiResultTask.Listener<MyMultiResultUseCase.Result, Unit> {
            override fun onResult(
                task: MultiResultTask<MyMultiResultUseCase.Result, Unit>,
                result: MyMultiResultUseCase.Result,
                tag: Unit
            ) {
                Log.d(TAG, "Listener2: onResult($result)")
            }

            override fun onComplete(
                task: MultiResultTask<MyMultiResultUseCase.Result, Unit>,
                tag: Unit
            ) {
                Log.d(TAG, "Listener2: onComplete()")
            }

            override fun onError(
                task: MultiResultTask<MyMultiResultUseCase.Result, Unit>,
                error: Throwable,
                tag: Unit
            ) {
                Log.d(TAG, "Listener2: onError($error)")
            }
        }

        CoroutineScope(Dispatchers.Main + Job()).let { scope ->
            Log.d(TAG, "0: Task started")
            scope.launch {
                val task = MyMultiResultUseCaseImpl(customDispatcher).asMultiResultTask(
                    MyMultiResultUseCase.Param(listOf(10, 30, 60, 100, 120, 200), 300L), Unit
                )
                delay(1000)
                Log.d(TAG, "1: isComplete=${task.isComplete}")
                task.addListener(listener1)
                task.addListener(listener1)
                task.addListener(listener1)
                delay(4000)
                Log.d(TAG, "2: isComplete=${task.isComplete}")
                task.addListener(listener2)
                task.addListener(listener2)
                task.addListener(listener2)
                delay(2000)
                Log.d(TAG, "3: Done")
            }
        }

//        val listener1 = object :
//            SingleResultTask.Listener<MySingleResultUseCase.Result, Unit> {
//            override fun onComplete(
//                task: SingleResultTask<MySingleResultUseCase.Result, Unit>,
//                result: MySingleResultUseCase.Result,
//                tag: Unit
//            ) {
//                Log.d(TAG, "Listener1: onComplete($result)")
//            }
//
//            override fun onError(
//                task: SingleResultTask<MySingleResultUseCase.Result, Unit>,
//                error: Throwable,
//                tag: Unit
//            ) {
//                Log.d(TAG, "Listener1: onError($error)")
//            }
//        }
//        val listener2 = object :
//            SingleResultTask.Listener<MySingleResultUseCase.Result, Unit> {
//            override fun onComplete(
//                task: SingleResultTask<MySingleResultUseCase.Result, Unit>,
//                result: MySingleResultUseCase.Result,
//                tag: Unit
//            ) {
//                Log.d(TAG, "Listener2: onComplete($result)")
//            }
//
//            override fun onError(
//                task: SingleResultTask<MySingleResultUseCase.Result, Unit>,
//                error: Throwable,
//                tag: Unit
//            ) {
//                Log.d(TAG, "Listener2: onError($error)")
//            }
//        }
//
//        CoroutineScope(Dispatchers.Main + Job()).let { scope ->
//            Log.d(TAG, "0: Task started")
//            scope.launch {
//                val task = MySingleResultUseCaseImpl().asSingleResultTask(
//                    MySingleResultUseCase.Param(
//                        3_000L,
//                        23
//                    ), Unit
//                )
//                delay(1000)
//                Log.d(TAG, "1: isComplete=${task.isComplete}")
//                task.addListener(listener1)
//                task.addListener(listener1)
//                task.addListener(listener1)
//                delay(4000)
//                Log.d(TAG, "2: isComplete=${task.isComplete}")
//                task.addListener(listener2)
//                task.addListener(listener2)
//                task.addListener(listener2)
//                delay(2000)
//                Log.d(TAG, "3: Done")
//            }
//        }
//        MySingleResultUseCase().execute(1, object : SingleResultUseCase.Callback<Int> {
//            override fun onComplete(result: Int) {
//                Log.d(TAG, "Result = $result")
//            }
//
//            override fun onError(error: Throwable) {
//                Log.d(TAG, "Failed", error)
//            }
//        })
//
//        MyMultiResultUseCase().execute(1, object : MultiResultUseCase.Callback<Int> {
//            override fun onResult(result: Int) {
//                Log.d(TAG, "Result = $result")
//            }
//
//            override fun onError(error: Throwable) {
//                Log.d(TAG, "Failed", error)
//            }
//
//            override fun onComplete() {
//                Log.d(TAG, "Complete")
//            }
//        })
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
