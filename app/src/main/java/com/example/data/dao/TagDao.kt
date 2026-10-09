package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.Tag
import com.example.data.entity.TagWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("""
        SELECT t.id, t.sectionId, t.name, t.createdAt,
            (SELECT COUNT(*) FROM notes WHERE tagId = t.id AND sectionId = t.sectionId) AS noteCount
        FROM tags t
        WHERE t.sectionId = :sectionId
        ORDER BY t.name COLLATE NOCASE ASC
    """)
    fun getTagsWithCountsForSection(sectionId: Long): Flow<List<TagWithCount>>

    @Query("SELECT * FROM tags WHERE sectionId = :sectionId ORDER BY name COLLATE NOCASE ASC")
    suspend fun getTagsForSection(sectionId: Long): List<Tag>

    @Query("SELECT * FROM tags WHERE sectionId = :sectionId AND LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getTagByNameAndSection(sectionId: Long, name: String): Tag?

    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    suspend fun getTagById(id: Long): Tag?

    @Query("SELECT * FROM tags")
    suspend fun getAllTags(): List<Tag>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTag(tag: Tag): Long

    @Update
    suspend fun updateTag(tag: Tag)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTagById(id: Long)

    @Query("DELETE FROM tags")
    suspend fun deleteAll()
}
