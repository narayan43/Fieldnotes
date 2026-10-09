package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "writing_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["noteId"])]
)
data class WritingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val startedAt: Long,
    val endedAt: Long,
    val durationMs: Long
)
