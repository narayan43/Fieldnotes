package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sub_goals",
    foreignKeys = [
        ForeignKey(
            entity = Goal::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["goalId"]),
        Index(value = ["goalId", "name"], unique = true)
    ]
)
data class SubGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
)

data class SubGoalWithStats(
    val id: Long,
    val goalId: Long,
    val name: String,
    val createdAt: Long,
    val sortOrder: Int,
    val totalTimeMs: Long
)
