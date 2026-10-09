package com.example.data.repository

import com.example.data.database.AppDatabase
import com.example.data.entity.Goal
import com.example.data.entity.GoalSession
import com.example.data.entity.GoalWithStats
import com.example.data.entity.RunningTimer
import com.example.data.entity.SubGoal
import com.example.data.entity.SubGoalWithStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.util.Calendar

data class GoalDashboardStats(
    val goalTimesToday: Map<Long, Long>,
    val goalTimesWeek: Map<Long, Long>,
    val goalTimesAll: Map<Long, Long>,
    val subGoalTimesToday: Map<Long, Long>,
    val subGoalTimesWeek: Map<Long, Long>,
    val subGoalTimesAll: Map<Long, Long>,
    val dailyBreakdownLast7Days: List<DayTimeTotal>
)

data class DayTimeTotal(
    val dayTimestamp: Long,
    val totalMs: Long
)

class GoalsRepository(private val db: AppDatabase) {

    fun getGoals(): Flow<List<GoalWithStats>> = db.goalDao().getGoalsWithStats()

    fun getSubGoals(goalId: Long): Flow<List<SubGoalWithStats>> = db.goalDao().getSubGoalsWithStats(goalId)

    fun getRunningTimer(): Flow<RunningTimer?> = db.runningTimerDao().getRunningTimer()

    suspend fun createGoal(name: String): Result<Long> = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Goal name is required"))
        }
        val existing = db.goalDao().getGoalByName(trimmed)
        if (existing != null) {
            return@withContext Result.failure(IllegalArgumentException("Goal \"$trimmed\" already exists"))
        }
        val all = db.goalDao().getAllGoals()
        val sortOrder = if (all.isEmpty()) 0 else (all.maxOfOrNull { it.sortOrder } ?: 0) + 1
        val id = db.goalDao().insertGoal(Goal(name = trimmed, sortOrder = sortOrder))
        Result.success(id)
    }

    suspend fun createSubGoal(goalId: Long, name: String): Result<Long> = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Sub-goal name is required"))
        }
        val existing = db.goalDao().getSubGoalByName(goalId, trimmed)
        if (existing != null) {
            return@withContext Result.failure(IllegalArgumentException("Sub-goal \"$trimmed\" already exists"))
        }
        val all = db.goalDao().getSubGoalsForGoal(goalId)
        val sortOrder = if (all.isEmpty()) 0 else (all.maxOfOrNull { it.sortOrder } ?: 0) + 1
        val id = db.goalDao().insertSubGoal(SubGoal(goalId = goalId, name = trimmed, sortOrder = sortOrder))
        Result.success(id)
    }

    suspend fun deleteGoal(goalId: Long) = withContext(Dispatchers.IO) {
        db.goalDao().deleteGoalById(goalId)
    }

    suspend fun deleteSubGoal(subGoalId: Long) = withContext(Dispatchers.IO) {
        db.goalDao().deleteSubGoalById(subGoalId)
    }

    fun getGoalSessions(goalId: Long): Flow<List<GoalSession>> = db.goalSessionDao().getSessionsForGoal(goalId)

    fun getSubGoalSessions(subGoalId: Long): Flow<List<GoalSession>> = db.goalSessionDao().getSessionsForSubGoal(subGoalId)

    fun getDashboardStats(): Flow<GoalDashboardStats> {
        return db.goalSessionDao().getAllSessionsFlow().combine(db.goalDao().getGoalsWithStats()) { sessions, goals ->
            calculateStats(sessions)
        }
    }

    private fun calculateStats(sessions: List<GoalSession>): GoalDashboardStats {
        val cal = Calendar.getInstance()

        // Start of today (midnight)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis

        // Start of this week (Sunday or Monday based on locale, 6 days ago or week field)
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        val weekStart = cal.timeInMillis

        val goalToday = mutableMapOf<Long, Long>()
        val goalWeek = mutableMapOf<Long, Long>()
        val goalAll = mutableMapOf<Long, Long>()

        val subGoalToday = mutableMapOf<Long, Long>()
        val subGoalWeek = mutableMapOf<Long, Long>()
        val subGoalAll = mutableMapOf<Long, Long>()

        // 7 days breakdown: last 7 days ending today
        val dayBuckets = mutableListOf<DayTimeTotal>()
        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance()
            dayCal.set(Calendar.HOUR_OF_DAY, 0)
            dayCal.set(Calendar.MINUTE, 0)
            dayCal.set(Calendar.SECOND, 0)
            dayCal.set(Calendar.MILLISECOND, 0)
            dayCal.add(Calendar.DAY_OF_YEAR, -i)
            val dStart = dayCal.timeInMillis
            val dEnd = dStart + (24 * 60 * 60 * 1000L)

            val sum = sessions.filter { it.startedAt in dStart until dEnd }.sumOf { it.durationMs }
            dayBuckets.add(DayTimeTotal(dStart, sum))
        }

        sessions.forEach { s ->
            // All time
            goalAll[s.goalId] = (goalAll[s.goalId] ?: 0L) + s.durationMs
            if (s.subGoalId != null) {
                subGoalAll[s.subGoalId] = (subGoalAll[s.subGoalId] ?: 0L) + s.durationMs
            }

            // Week
            if (s.startedAt >= weekStart) {
                goalWeek[s.goalId] = (goalWeek[s.goalId] ?: 0L) + s.durationMs
                if (s.subGoalId != null) {
                    subGoalWeek[s.subGoalId] = (subGoalWeek[s.subGoalId] ?: 0L) + s.durationMs
                }
            }

            // Today
            if (s.startedAt >= todayStart) {
                goalToday[s.goalId] = (goalToday[s.goalId] ?: 0L) + s.durationMs
                if (s.subGoalId != null) {
                    subGoalToday[s.subGoalId] = (subGoalToday[s.subGoalId] ?: 0L) + s.durationMs
                }
            }
        }

        return GoalDashboardStats(
            goalTimesToday = goalToday,
            goalTimesWeek = goalWeek,
            goalTimesAll = goalAll,
            subGoalTimesToday = subGoalToday,
            subGoalTimesWeek = subGoalWeek,
            subGoalTimesAll = subGoalAll,
            dailyBreakdownLast7Days = dayBuckets
        )
    }
}
