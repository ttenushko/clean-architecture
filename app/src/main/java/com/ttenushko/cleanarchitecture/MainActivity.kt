package com.ttenushko.cleanarchitecture

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ttenushko.cleanarchitecture.domain.usecase.CoroutineMultiResultUseCase
import com.ttenushko.cleanarchitecture.domain.usecase.CoroutineSingleResultUseCase
import com.ttenushko.cleanarchitecture.domain.usecase.MultiResultUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "test"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        MySingleResultUseCase().execute(1, object : SingleResultUseCase.Callback<Int> {
//            override fun onComplete(result: Int) {
//                Log.d(TAG, "Result = $result")
//            }
//
//            override fun onError(error: Throwable) {
//                Log.d(TAG, "Failed", error)
//            }
//        })

        MyMultiResultUseCase().execute(1, object : MultiResultUseCase.Callback<Int> {
            override fun onResult(result: Int) {
                Log.d(TAG, "Result = $result")
            }

            override fun onError(error: Throwable) {
                Log.d(TAG, "Failed", error)
            }

            override fun onComplete() {
                Log.d(TAG, "Complete")
            }
        })
    }

    class MySingleResultUseCase : CoroutineSingleResultUseCase<Int, Int>() {
        override suspend fun run(param: Int): Int {
            return withContext(Dispatchers.IO) {
                30
            }
        }
    }

    class MyMultiResultUseCase : CoroutineMultiResultUseCase<Int, Int>() {

        override suspend fun run(param: Int, channel: SendChannel<Int>) {
            withContext(Dispatchers.IO) {
                listOf(10, 20, 30, 40).forEach {
                    channel.offer(it)
                    delay(200)
                }
            }
        }
    }
}
