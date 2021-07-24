package com.rsschool.pomodoro

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.*
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class ForegroundService : Service() {

    private var isServiceStarted = false
    private var notificationManager: NotificationManager? = null
    private var job: Job? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        processCommand(intent)
        return START_REDELIVER_INTENT
    }

    private fun processCommand(intent: Intent?) {
        when (intent?.extras?.getString(COMMAND_ID) ?: INVALID) {
            COMMAND_START -> {
                var label = intent?.extras?.getString(LABEL) ?: return
                if (label == "") label = "Timer"
                val initTime = intent.extras?.getLong(TIMER_INIT_TIME) ?: return
                val startTime = intent.extras?.getLong(TIMER_START_TIME) ?: return
                val operationTime = intent.extras?.getLong(TIMER_OPERATION_TIME) ?: return

                commandStart(label, initTime, startTime, operationTime)
            }
            COMMAND_STOP -> commandStop()
            INVALID -> return
        }
    }

    private fun commandStart(label: String, initTime: Long, startTime: Long, operationTime: Long) {
        if (isServiceStarted)
            return
        try {
            moveToStartedState()
            startForegroundAndShowNotification(label)
            continueTimer(label, initTime, startTime, operationTime)
        } finally {
            isServiceStarted = true
        }
    }

    private fun moveToStartedState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(Intent(this, ForegroundService::class.java))
        else
            startService(Intent(this, ForegroundService::class.java))
    }

    private fun startForegroundAndShowNotification(label: String) {
        createChannel()
        val notification = getNotification(label, "Initialization...")
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Pomodoro"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val notificationChannel = NotificationChannel(
                CHANNEL_ID, channelName, importance
            )
            notificationManager?.createNotificationChannel(notificationChannel)
        }
    }

    private val builder by lazy {
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setGroup("Pomodoro Timer")
            .setGroupSummary(false)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getPendingIntent())
            .setSilent(true)
            .setSmallIcon(R.drawable.ic_baseline_access_alarm_24)
    }

    private fun getPendingIntent(): PendingIntent? {
        val resultIntent = Intent(this, MainActivity::class.java)
        resultIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_ONE_SHOT)
    }

    private fun getNotification(label: String, content: String) =
        builder.setContentTitle(label).setContentText(content).build()

    private fun continueTimer(label: String, initTime: Long, startTime: Long, operationTime: Long) {
        job = GlobalScope.launch(Dispatchers.Main) {
            while (true) {
                val time = initTime - SystemClock.elapsedRealtime() + startTime - operationTime
                if (time > 0) {
                    notificationManager?.notify(
                        NOTIFICATION_ID,
                        getNotification(label, time.displayTime())
                    )
                    delay(INTERVAL)
                } else {
                    notificationManager?.notify(
                        NOTIFICATION_ID,
                        getNotification(label, "Time's up (${initTime.displayTime()})")
                    )
                    turnOnSoundAndVibration()
                    break
                }
            }
        }
    }

    private fun turnOnSoundAndVibration() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        2000,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                vibrator.vibrate(2000)
            }
        }

        try {
            val ringtone = RingtoneManager.getRingtone(
                applicationContext,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            )
            ringtone.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun commandStop() {
        if (!isServiceStarted)
            return
        try {
            job?.cancel()
            stopForeground(true)
            stopSelf()
        } finally {
            isServiceStarted = false
        }
    }

    private companion object {
        private const val CHANNEL_ID = "Channel_ID"
        private const val NOTIFICATION_ID = 777
        private const val INTERVAL = 1000L
    }
}