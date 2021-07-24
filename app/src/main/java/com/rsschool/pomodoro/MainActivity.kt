package com.rsschool.pomodoro

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.rsschool.pomodoro.databinding.ActivityMainBinding
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity(), TimerListener, LifecycleObserver {

    private lateinit var binding: ActivityMainBinding

    private val timers = mutableListOf<TimerModel>()
    private val timerAdapter = TimerAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = timerAdapter
        }

        binding.addNewTimerButton.setOnClickListener {
            if (timers.size < 24)
                SetTimeDialog(this)
            else
                Toast.makeText(applicationContext, "Too many timers", Toast.LENGTH_SHORT).show()
        }
    }

    private var nextId = 0
    private var activeTimerIndex = -1
    private var job: Job? = null

    override fun add(initTime: Long, label: String) {
        if (timers.size != 0)
            nextId = timers[timers.lastIndex].id + 1
        timers.add(
            TimerModel(
                nextId++, label,
                initTime, SystemClock.elapsedRealtime(), 0,
                TimerStatus.STOPPED
            )
        )
        timerAdapter.submitList(timers.toList())

        binding.clickMeImage.isVisible = false
        binding.recyclerView.smoothScrollToPosition(timers.size - 1)
    }

    override fun delete(id: Int) {
        timers.remove(timers.find { it.id == id })
        timerAdapter.submitList(timers.toList())
        job?.cancel()

        if (timers.size == 0) binding.clickMeImage.isVisible = true
    }

    override fun start(id: Int) {
        changeTimer(id, true)
    }

    override fun stop(id: Int) {
        changeTimer(id, false)
    }

    override fun finish(id: Int) {
        val index = timers.indexOfFirst { it.id == id }
        if (index != -1) {
            val label = if (timers[index].label != "") timers[index].label else "Timer"
            val duration = timers[index].initTime.displayTime()

            activeTimerIndex = -1

            val intent = Intent(this, FinishActivity::class.java)
            intent.apply {
                putExtra(LABEL, label)
                putExtra(TIMER_INIT_TIME, duration)
            }
            startActivity(intent)
        }
    }

    private fun changeTimer(id: Int, isStart: Boolean) {
        activeTimerIndex = timers.indexOfFirst { it.status == TimerStatus.ACTIVE }

        // Остановить работающий таймер
        if (activeTimerIndex != -1) {
            timers[activeTimerIndex].apply {
                operationTime += SystemClock.elapsedRealtime() - startTime
                status = TimerStatus.STOPPED
            }
            activeTimerIndex = -1
            job?.cancel()
        }

        // Если был вызван "старт", запустить новый таймер
        if (isStart) {
            activeTimerIndex = timers.indexOfFirst { it.id == id }
            timers[activeTimerIndex].apply {
                status = TimerStatus.ACTIVE
                startTime = SystemClock.elapsedRealtime()
            }
            checkForFinish(timers[activeTimerIndex])
        }

        timerAdapter.notifyDataSetChanged()
    }

    private fun checkForFinish(timer: TimerModel) {
        job = GlobalScope.launch(Dispatchers.Main) {
            while (true) {
                val time =
                    timer.initTime - SystemClock.elapsedRealtime() + timer.startTime - timer.operationTime
                if (time > 0L && timer.status != TimerStatus.FINISHED) {
                    delay(INTERVAL)
                } else {
                    job?.cancel()
                    finish(timer.id)
                    break
                }
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        if (activeTimerIndex != -1) {
            val startIntent = Intent(this, ForegroundService::class.java)
            startIntent.apply {
                putExtra(COMMAND_ID, COMMAND_START)
                putExtra(LABEL, timers[activeTimerIndex].label)
                putExtra(TIMER_INIT_TIME, timers[activeTimerIndex].initTime)
                putExtra(TIMER_START_TIME, timers[activeTimerIndex].startTime)
                putExtra(TIMER_OPERATION_TIME, timers[activeTimerIndex].operationTime)
            }
            startService(startIntent)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        val stopIntent = Intent(this, ForegroundService::class.java)
        stopIntent.putExtra(COMMAND_ID, COMMAND_STOP)
        startService(stopIntent)
    }

    override fun onBackPressed() {
        this.moveTaskToBack(true)
    }
}