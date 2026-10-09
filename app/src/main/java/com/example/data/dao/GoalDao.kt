package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.Goal
import com.example.data.entity.GoalWithStats
import com.example.data.entity.SubGoal
import com.example.data.entity.SubGoalWithStats
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("""
        SELECT g.id, g.name, g.createdAt, g.sortOrder,
            (SELECT COALESCE(SUM(durationMs), 0) FROM goal_sessions WHERE goalId = g.id) AS totalTimeMs
        FROM goals g
        ORDER BY g.sortOrder ASC, g.createdAt ASC
    """)
    fun getGoalsWithStats(): Flow<List<GoalWithStats>>

    @Query("SELECT * FROM goals ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getAllGoals(): List<Goal>

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    fun getGoalById(id: Long): Flow<Goal?>

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun getGoalByIdOnce(id: Long): Goal?

    @Query("SELECT * FROM goals WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getGoalByName(name: String): Goal?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGoal(goal: Goal): Long

    @Update
    suspend fun updateGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: Long)

    // SubGoals
    @Query("""
        SELECT sg.id, sg.goalId, sg.name, sg.createdAt, sg.sortOrder,
            (SELECT COALESCE(SUM(durationMs), 0) FROM goal_sessions WHERE subGoalId = sg.id) AS totalTimeMs
        FROM sub_goals sg
        WHERE sg.goalId = :goalId
        ORDER BY sg.sortOrder ASC, sg.createdAt ASC
    """)
    fun getSubGoalsWithStats(goalId: Long): Flow<List<SubGoalWithStats>>

    @Query("SELECT * FROM sub_goals WHERE goalId = :goalId ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getSubGoalsForGoal(goalId: Long): List<SubGoal>

    @Query("SELECT * FROM sub_goals")
    suspend fun getAllSubGoals(): List<SubGoal>

    @Query("SELECT * FROM sub_goals WHERE id = :id LIMIT 1")
    suspend fun getSubGoalByIdOnce(id: Long): SubGoal?

    @Query("SELECT * FROM sub_goals WHERE goalId = :goalId AND LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getSubGoalByName(goalId: Long, name: String): SubGoal?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSubGoal(subGoal: SubGoal): Long

    @Update
    suspend fun updateSubGoal(subGoal: SubGoal)

    @Query("DELETE FROM sub_goals WHERE id = :id")
    suspend fun deleteSubGoalById(id: Long)

    @Query("DELETE FROM goals")
    suspend fun deleteAllGoals()

    @Query("DELETE FROM sub_goals")
    suspend fun deleteAllSubGoals()
}
