package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.GoalSession
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalSessionDao {
    @Query("SELECT * FROM goal_sessions WHERE goalId = :goalId ORDER BY startedAt DESC")
    fun getSessionsForGoal(goalId: Long): Flow<List<GoalSession>>

    @Query("SELECT * FROM goal_sessions WHERE subGoalId = :subGoalId ORDER BY startedAt DESC")
    fun getSessionsForSubGoal(subGoalId: Long): Flow<List<GoalSession>>

    @Query("SELECT * FROM goal_sessions ORDER BY startedAt DESC")
    fun getAllSessionsFlow(): Flow<List<GoalSession>>

    @Query("SELECT * FROM goal_sessions")
    suspend fun getAllSessions(): List<GoalSession>

    @Query("SELECT * FROM goal_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): GoalSession?

    @Query("SELECT COALESCE(SUM(durationMs), 0) FROM goal_sessions WHERE goalId = :goalId AND startedAt >= :sinceTimeMs")
    fun getGoalTimeSince(goalId: Long, sinceTimeMs: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(durationMs), 0) FROM goal_sessions WHERE subGoalId = :subGoalId AND startedAt >= :sinceTimeMs")
    fun getSubGoalTimeSince(subGoalId: Long, sinceTimeMs: Long): Flow<Long>

    @Query("SELECT * FROM goal_sessions WHERE startedAt >= :sinceTimeMs ORDER BY startedAt ASC")
    fun getSessionsSince(sinceTimeMs: Long): Flow<List<GoalSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalSession(session: GoalSession): Long

    @Query("DELETE FROM goal_sessions")
    suspend fun deleteAll()
}
