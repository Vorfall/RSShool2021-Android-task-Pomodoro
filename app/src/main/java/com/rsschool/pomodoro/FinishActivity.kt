package com.rsschool.pomodoro

import android.graphics.drawable.AnimationDrawable
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.appcompat.app.AppCompatActivity
import com.rsschool.pomodoro.databinding.ActivityFinishBinding

class FinishActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFinishBinding
    private lateinit var vibrator: Vibrator
    private lateinit var ringtone: Ringtone

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFinishBinding.inflate(layoutInflater)
        setContentView(binding.root)

        (binding.circles.background as? AnimationDrawable)?.start()

        binding.label.text = intent?.getStringExtra(LABEL)
        binding.timerDuration.text = intent?.getStringExtra(TIMER_INIT_TIME)

        binding.buttonFinishOk.setOnClickListener { finish() }

        turnOnVibration()
        turnOnSound()
    }

    private fun turnOnVibration() {
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        val pattern = MutableList(200) { 300L }.toLongArray()
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        pattern,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                vibrator.vibrate(pattern, 2)
            }
        }
    }

    private fun turnOnSound() {
        try {
            ringtone = RingtoneManager.getRingtone(
                applicationContext,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            )
            ringtone.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        vibrator.cancel()
        ringtone.stop()
        super.onDestroy()
    }
}