package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
)

data class GoalWithStats(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val sortOrder: Int,
    val totalTimeMs: Long
)
