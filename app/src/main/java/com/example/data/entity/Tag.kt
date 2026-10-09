package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tags",
    foreignKeys = [
        ForeignKey(
            entity = Section::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sectionId", "name"], unique = true),
        Index(value = ["sectionId"])
    ]
)
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sectionId: Long,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class TagWithCount(
    val id: Long,
    val sectionId: Long,
    val name: String,
    val createdAt: Long,
    val noteCount: Int
)
