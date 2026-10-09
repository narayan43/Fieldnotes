package com.example

import android.app.Application
import com.example.data.database.AppDatabase
import com.example.service.TimerForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FieldnotesApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)

        // Process recreation check: If a timer was running before process death, resume service
        CoroutineScope(Dispatchers.IO).launch {
            val runningTimer = db.runningTimerDao().getRunningTimerOnce()
            if (runningTimer != null) {
                TimerForegroundService.startTimer(
                    applicationContext,
                    runningTimer.goalId,
                    runningTimer.subGoalId
                )
            }
        }
    }
}
