package com.rsschool.pomodoro

import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.os.CountDownTimer
import android.os.SystemClock
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.rsschool.pomodoro.databinding.TimerItemBinding

class TimerViewHolder(
    private val binding: TimerItemBinding,
    private val listener: TimerListener,
    private val resources: Resources
) : RecyclerView.ViewHolder(binding.root) {

    private var countDownTimer: CountDownTimer? = null

    fun update(timer: TimerModel) {
        binding.timerTime.text =
            if (timer.status != TimerStatus.FINISHED)
                (timer.initTime - timer.operationTime).displayTime()
            else START_TIME

        if (timer.status == TimerStatus.ACTIVE)
            startTimer(timer)
        else if (timer.status == TimerStatus.STOPPED)
            stopTimer(timer)

        updateCardView(timer)
        updateCustomView(timer)
        initButtonsListeners(timer)
    }

    private fun updateCardView(timer: TimerModel) {
        if (timer.status != TimerStatus.FINISHED) {
            binding.timerItem.background = Color.WHITE.toDrawable()
        } else {
            binding.timerItem.background = ContextCompat.getDrawable(
                (listener as MainActivity).applicationContext, R.color.pink
            )
            binding.startPauseButton.text = resources.getString(R.string.restart)
        }

        val params = binding.timerTime.layoutParams as ConstraintLayout.LayoutParams
        if (timer.label == "") {
            binding.timerLabel.isVisible = false
            params.bottomToBottom = binding.timerItem.id
        } else {
            binding.timerLabel.isVisible = true
            params.bottomToTop = binding.timerLabel.id
            binding.timerLabel.text = timer.label
        }
        binding.timerTime.requestLayout()
    }

    private fun updateCustomView(timer: TimerModel) {
        binding.progressBar.setPeriod(timer.initTime)
        binding.progressBar.setCurrent(timer.operationTime)
    }

    private fun initButtonsListeners(timer: TimerModel) {
        binding.startPauseButton.setOnClickListener {
            when (timer.status) {
                TimerStatus.ACTIVE -> listener.stop(timer.id)
                TimerStatus.STOPPED -> listener.start(timer.id)
                TimerStatus.FINISHED -> {
                    timer.status = TimerStatus.STOPPED
                    binding.timerTime.text = timer.initTime.displayTime()
                    update(timer)
                }
            }
        }

        binding.deleteButton.setOnClickListener { listener.delete(timer.id) }
    }

    private fun startTimer(timer: TimerModel) {
        countDownTimer?.cancel()
        countDownTimer = getCountDownTimer(timer)
        countDownTimer?.start()

        binding.startPauseButton.text = resources.getString(R.string.stop)
        binding.blinkingIndicator.isInvisible = false
        (binding.blinkingIndicator.background as? AnimationDrawable)?.start()
    }

    private fun stopTimer(timer: TimerModel) {
        countDownTimer?.cancel()

        binding.startPauseButton.text = resources.getString(R.string.start)
        binding.blinkingIndicator.isInvisible = true
        (binding.blinkingIndicator.background as? AnimationDrawable)?.stop()
    }

    private fun getCountDownTimer(timer: TimerModel): CountDownTimer {
        return object :
            CountDownTimer((timer.initTime - timer.operationTime + INTERVAL), INTERVAL) {

            override fun onTick(millisUntilFinished: Long) {
                if (timer.status != TimerStatus.STOPPED) {
                    val runningTime =
                        SystemClock.elapsedRealtime() - timer.startTime + timer.operationTime
                    if (runningTime > timer.initTime)
                        onFinish()
                    else {
                        binding.timerTime.text = (timer.initTime - runningTime).displayTime()
                        binding.progressBar.setCurrent(runningTime)
                    }
                } else countDownTimer?.cancel()
            }

            override fun onFinish() {
                stopTimer(timer)
                timer.apply {
                    startTime = 0
                    operationTime = 0
                    status = TimerStatus.FINISHED
                }

                binding.timerTime.text = START_TIME
                binding.progressBar.setCurrent(0L)
                binding.timerItem.background = ContextCompat.getDrawable(
                    (listener as MainActivity).applicationContext, R.color.pink
                )
                binding.startPauseButton.text = resources.getString(R.string.restart)
            }
        }
    }
}