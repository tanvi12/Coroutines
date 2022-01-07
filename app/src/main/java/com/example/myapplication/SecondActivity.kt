package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_second.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main

class SecondActivity : AppCompatActivity() {
    private val PROGRESS_MAX = 100
    private val PROGRESS_START = 0
    private val JOB_TIME = 3000 //ms
    private lateinit var job: CompletableJob

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)
        job_button.setOnClickListener {
            if (!::job.isInitialized)
                initJob()

            job_progress_bar.startJobOrCancel(job)
        }


    }

    fun ProgressBar.startJobOrCancel(job: Job) {
        if (this.progress > 0) {
            println("$Job is already active. Cancelling...")
            resetJob()
        } else {
            job_button.text = "Cancel Job"
            var scope = CoroutineScope(IO + job).launch {
                for (i in PROGRESS_START..PROGRESS_MAX) {
                    delay((JOB_TIME / PROGRESS_MAX).toLong())
                    progress = i
                }
                updateJobComplete("Job is complete")
            }

        }
    }

    fun updateJobComplete(text: String) {
        GlobalScope.launch(Main) {
            job_complete_text.text = text
        }
    }

    private fun resetJob() {
        if(job.isActive || job.isCompleted){
            job.cancel(CancellationException("Resetting Job"))
        }
        initJob()
    }

    fun initJob() {
        job_button.setText("Start Job #1")
        updateJobComplete("")
        job = Job()
        job.invokeOnCompletion {
            it?.message.let {
                var msg = it
                if (msg.isNullOrBlank()) {
                    msg = "Unknow cancellation error."
                    println("$job was cancelled. Reason: $msg")
                    showToast(msg)
                }
            }
        }
        job_progress_bar.max = PROGRESS_MAX
        job_progress_bar.progress = PROGRESS_START

    }

    fun showToast(text: String) {
        GlobalScope.launch(Main) {
            Toast.makeText(this@SecondActivity, text, Toast.LENGTH_SHORT).show()
        }
    }
}