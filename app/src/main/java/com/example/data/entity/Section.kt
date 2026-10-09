package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sections",
    indices = [Index(value = ["name"], unique = true)]
)
data class Section(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0,
    val colorIndex: Int = 0
)

data class SectionWithStats(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val sortOrder: Int,
    val colorIndex: Int,
    val noteCount: Int,
    val totalWritingTimeMs: Long
)
