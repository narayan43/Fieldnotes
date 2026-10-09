package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.database.AppDatabase
import com.example.data.entity.GoalSession
import com.example.data.entity.RunningTimer
import com.example.util.TimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerForegroundService : Service {

    constructor() : super()

    companion object {
        const val CHANNEL_ID = "fieldnotes_timer_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"

        fun startTimer(context: Context, goalId: Long, subGoalId: Long? = null) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_START
                putExtra("goalId", goalId)
                if (subGoalId != null) putExtra("subGoalId", subGoalId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopTimer(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var tickerJob: Job? = null
    private var currentGoalName: String = "Goal Work"
    private var startedAt: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                handleStop()
            }
            ACTION_START -> {
                val goalId = intent.getLongExtra("goalId", -1L)
                val subGoalId = if (intent.hasExtra("subGoalId")) intent.getLongExtra("subGoalId", -1L) else null
                handleStart(goalId, subGoalId)
            }
            else -> {
                // Potential system restart after kill: check running timer in DB
                serviceScope.launch(Dispatchers.IO) {
                    val db = AppDatabase.getInstance(applicationContext)
                    val running = db.runningTimerDao().getRunningTimerOnce()
                    if (running != null) {
                        launch(Dispatchers.Main) {
                            handleStart(running.goalId, running.subGoalId, running.startedAt)
                        }
                    } else {
                        stopSelf()
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun handleStart(goalId: Long, subGoalId: Long?, existingStartedAt: Long? = null) {
        serviceScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(applicationContext)

            // If a previous timer was running, stop it and save session
            val previousRunning = db.runningTimerDao().getRunningTimerOnce()
            if (previousRunning != null && existingStartedAt == null) {
                val now = System.currentTimeMillis()
                val duration = (now - previousRunning.startedAt).coerceAtLeast(0)
                if (duration > 1000) {
                    db.goalSessionDao().insertGoalSession(
                        GoalSession(
                            goalId = previousRunning.goalId,
                            subGoalId = previousRunning.subGoalId,
                            startedAt = previousRunning.startedAt,
                            endedAt = now,
                            durationMs = duration
                        )
                    )
                }
            }

            val startTime = existingStartedAt ?: System.currentTimeMillis()
            startedAt = startTime

            // Persist running timer to DB
            val newTimer = RunningTimer(
                id = 1L,
                goalId = goalId,
                subGoalId = subGoalId,
                startedAt = startTime
            )
            db.runningTimerDao().setRunningTimer(newTimer)

            // Lookup goal and subgoal names
            val goal = db.goalDao().getGoalByIdOnce(goalId)
            val subGoal = if (subGoalId != null) db.goalDao().getSubGoalByIdOnce(subGoalId) else null

            currentGoalName = when {
                goal != null && subGoal != null -> "${goal.name} • ${subGoal.name}"
                goal != null -> goal.name
                else -> "Focused Goal"
            }

            launch(Dispatchers.Main) {
                startForegroundWithNotification()
                startTicker()
            }
        }
    }

    private fun handleStop() {
        tickerJob?.cancel()
        serviceScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(applicationContext)
            val running = db.runningTimerDao().getRunningTimerOnce()
            if (running != null) {
                val now = System.currentTimeMillis()
                val duration = (now - running.startedAt).coerceAtLeast(0)
                if (duration > 1000) {
                    db.goalSessionDao().insertGoalSession(
                        GoalSession(
                            goalId = running.goalId,
                            subGoalId = running.subGoalId,
                            startedAt = running.startedAt,
                            endedAt = now,
                            durationMs = duration
                        )
                    )
                }
                db.runningTimerDao().clearRunningTimer()
            }

            launch(Dispatchers.Main) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        }
    }

    private fun startForegroundWithNotification() {
        val initialElapsed = (System.currentTimeMillis() - startedAt).coerceAtLeast(0)
        val notification = buildNotification(currentGoalName, TimeFormatter.formatTimerClock(initialElapsed))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            while (isActive) {
                val elapsed = (System.currentTimeMillis() - startedAt).coerceAtLeast(0)
                val notification = buildNotification(currentGoalName, TimeFormatter.formatTimerClock(elapsed))
                notificationManager.notify(NOTIFICATION_ID, notification)
                delay(1000)
            }
        }
    }

    private fun buildNotification(title: String, elapsedClock: String): Notification {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TimerForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText("Elapsed: $elapsedClock")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Goal Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active focused goal work timer"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
