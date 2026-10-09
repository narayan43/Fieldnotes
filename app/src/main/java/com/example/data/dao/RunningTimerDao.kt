package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.RunningTimer
import kotlinx.coroutines.flow.Flow

@Dao
interface RunningTimerDao {
    @Query("SELECT * FROM running_timer WHERE id = 1 LIMIT 1")
    fun getRunningTimer(): Flow<RunningTimer?>

    @Query("SELECT * FROM running_timer WHERE id = 1 LIMIT 1")
    suspend fun getRunningTimerOnce(): RunningTimer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setRunningTimer(timer: RunningTimer)

    @Query("DELETE FROM running_timer")
    suspend fun clearRunningTimer()
}
