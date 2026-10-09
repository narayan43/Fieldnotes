package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goal_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Goal::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SubGoal::class,
            parentColumns = ["id"],
            childColumns = ["subGoalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["goalId"]),
        Index(value = ["subGoalId"])
    ]
)
data class GoalSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val subGoalId: Long? = null,
    val startedAt: Long,
    val endedAt: Long,
    val durationMs: Long
)

@Entity(tableName = "running_timer")
data class RunningTimer(
    @PrimaryKey val id: Long = 1L,
    val goalId: Long,
    val subGoalId: Long? = null,
    val startedAt: Long
)
