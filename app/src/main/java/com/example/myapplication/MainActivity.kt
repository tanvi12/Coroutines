package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity() {
    private val result1 = "Result #1"
    private val result2 = "Result #2"
    val JOB_TIMEOUT = 2100L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnClickListener {
            runBlockingExample()
        }
    }


    // it will block entire thread until it gets completed
    private fun runBlockingExample (){
        CoroutineScope(Main).launch {
            val result1 = getResult()
            println("result1: $result1")

            val result2 = getResult()
            println("result2: $result2")

            val result3 = getResult()
            println("result3: $result3")

            val result4 = getResult()
            println("result4: $result4")

            val result5 = getResult()
            println("result5: $result5")
        }

        CoroutineScope(Main).launch {
           delay(1000)
            runBlocking {
                delay(4000)
                println("done run blocking")
            }
        }
    }

    val handler = CoroutineExceptionHandler { context, exception ->
        println("Exception thrown somewhere within parent or child: $exception.")
    }


    val childExceptionHandler = CoroutineExceptionHandler{ _, exception ->
        println("Exception thrown in one of the children: $exception.")
    }

    fun main(){

        val parentJob = CoroutineScope(Main).launch(handler) {

            supervisorScope { // *** Make sure to handle errors in children ***

                // --------- JOB A ---------
                val jobA =  launch {
                    val resultA = getResult(1)
                    println("resultA: ${resultA}")
                }

                // --------- JOB B ---------
                val jobB = launch(childExceptionHandler) {
                    val resultB = getResult(2)
                    println("resultB: ${resultB}")
                }

                // --------- JOB C ---------
                val jobC = launch {
                    val resultC = getResult(3)
                    println("resultC: ${resultC}")
                }
            }
        }

        parentJob.invokeOnCompletion { throwable ->
            if(throwable != null){
                println("Parent job failed: ${throwable}")
            }
            else{
                println("Parent job SUCCESS")
            }
        }
    }

    suspend fun getResult(number: Int): Int{
        return withContext(Main){
            delay(number*500L)
            if(number == 2){
                throw Exception("Error getting result for number: ${number}")
            }
            number*2
        }
    }






private suspend fun getResult() : Int{
        delay(1000)
        return  Random.nextInt(0,100)
    }

    private fun fakeAPISequentialRequests(){
        CoroutineScope(IO).launch {
            val executionTime = measureTimeMillis {
                val result1 = async {
                    getResultFromApi()
                }.await()

                val result2 = async {
                    getResult3FromApi(result1)
                }

                setTextOnMainThread(result2.await())

            }
            println("total elapsed time: $executionTime")
        }
    }

    private fun fakeAPIRequestUsingAsyncAndAwaitPatter(){
       CoroutineScope(IO).launch {
           val executiontime = measureTimeMillis {
               val result1 :Deferred<String> = async {
                   getResultFromApi()
               }

               val result2 :Deferred<String> = async {
                   getResult2FromApi()
               }
               // to get result we need to call await and it waits until we get result
               setTextOnMainThread(result1.await())
               setTextOnMainThread(result2.await())
           }
       }
    }
    private fun fakeAPIRequestParallel(){
        CoroutineScope(IO).launch {
            val job1 = launch{
                val time1 = measureTimeMillis {
                    println("debug: launching job1 in thread: ${Thread.currentThread().name }")
                    val result1 = getResultFromApi()
                    setTextOnMainThread(result1)
                }
                println("debug: completed job1 $time1 ms")
            }

//            job1.join()   // it will first finish job1 and then start job2
            val job2 = launch{
                val time2 = measureTimeMillis {
                    println("debug: launching job2 in thread: ${Thread.currentThread().name }")
                    val result2 = getResult2FromApi()
                    setTextOnMainThread(result2)
                }
                println("debug: completed job2 $time2 ms")
            }
        }
    }
    private suspend fun fakeAPIRequestTimeoutExample(){
        withContext(IO){
            val job = withTimeoutOrNull(JOB_TIMEOUT) {
                val result1 = getResultFromApi()
                setTextOnMainThread(result1)

                val result2 = getResult2FromApi()
                setTextOnMainThread(result2)
            }

            // if it is timeout it will return null value
            if(job == null){
                val CancelMessage = "Cancelling job.. Job took longer than $JOB_TIMEOUT"
                setTextOnMainThread(CancelMessage)
            }
        }
    }
    private suspend fun fakeAPIRequest(){
        val result1 = getResultFromApi()
        setTextOnMainThread(result1)

        val result2 = getResult2FromApi()
        setTextOnMainThread(result2)

    }

    private suspend fun setTextOnMainThread(input:String){
      withContext(Main){
          setNewText(input)
      }
    }
    private suspend fun getResultFromApi(): String {

        logThread("getResult1FromApi")
        delay(1000)  // this will only delay this single coroutine
//        Thread.sleep(1000) // entire thread will be sleep
        // consider coroutine is task that can run in thread, many coroutine can be run on single thread
        return result1
    }

    private suspend fun getResult2FromApi(): String {
        logThread("getResult2FromApi")
        delay(1000)  // this will only delay this single coroutine
        return result2
    }


    private suspend fun getResult3FromApi(result1: String): String {
        logThread("getResult3FromApi")
        delay(2000)  // this will only delay this single coroutine
        return result1+ result2
    }
    private fun setNewText(input: String){
        val newText = text.text.toString() + "\n$input"
        text.text = newText
    }

    private fun logThread(methodName: String) {
        println("debug: ${methodName}: ${Thread.currentThread().name}")
    }
}