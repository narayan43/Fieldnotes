package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.Section
import com.example.data.entity.SectionWithStats
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionDao {
    @Query("""
        SELECT s.id, s.name, s.createdAt, s.sortOrder, s.colorIndex,
            (SELECT COUNT(*) FROM notes WHERE sectionId = s.id) AS noteCount,
            (SELECT COALESCE(SUM(durationMs), 0) FROM writing_sessions WHERE noteId IN (SELECT id FROM notes WHERE sectionId = s.id)) AS totalWritingTimeMs
        FROM sections s
        ORDER BY s.sortOrder ASC, s.createdAt ASC
    """)
    fun getSectionsWithStats(): Flow<List<SectionWithStats>>

    @Query("SELECT * FROM sections ORDER BY sortOrder ASC, createdAt ASC")
    fun getAllSections(): List<Section>

    @Query("SELECT * FROM sections WHERE id = :id LIMIT 1")
    fun getSectionById(id: Long): Flow<Section?>

    @Query("SELECT * FROM sections WHERE id = :id LIMIT 1")
    suspend fun getSectionByIdOnce(id: Long): Section?

    @Query("SELECT * FROM sections WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getSectionByName(name: String): Section?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSection(section: Section): Long

    @Update
    suspend fun updateSection(section: Section)

    @Delete
    suspend fun deleteSection(section: Section)

    @Query("DELETE FROM sections WHERE id = :id")
    suspend fun deleteSectionById(id: Long)

    @Query("UPDATE sections SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Query("DELETE FROM sections")
    suspend fun deleteAll()
}
