package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Section::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sectionId"]),
        Index(value = ["tagId"])
    ]
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sectionId: Long,
    val tagId: Long,
    val title: String,
    val body: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
)

data class NoteListItem(
    val id: Long,
    val sectionId: Long,
    val tagId: Long,
    val title: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
    val sortOrder: Int,
    val tagName: String,
    val totalWritingTimeMs: Long,
    val firstImageThumbnail: String?
)
