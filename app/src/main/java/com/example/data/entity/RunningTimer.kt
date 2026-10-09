package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "running_timer")
data class RunningTimer(
    @PrimaryKey val id: Long = 1L,
    val goalId: Long,
    val subGoalId: Long? = null,
    val startedAt: Long
)
