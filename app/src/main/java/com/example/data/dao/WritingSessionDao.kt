package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.WritingSession
import kotlinx.coroutines.flow.Flow

@Dao
interface WritingSessionDao {
    @Query("SELECT * FROM writing_sessions WHERE noteId = :noteId ORDER BY startedAt DESC")
    fun getWritingSessionsForNote(noteId: Long): Flow<List<WritingSession>>

    @Query("SELECT COALESCE(SUM(durationMs), 0) FROM writing_sessions WHERE noteId = :noteId")
    fun getTotalWritingTimeForNote(noteId: Long): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(ws.durationMs), 0)
        FROM writing_sessions ws
        JOIN notes n ON n.id = ws.noteId
        WHERE n.sectionId = :sectionId
    """)
    fun getTotalWritingTimeForSection(sectionId: Long): Flow<Long>

    @Query("SELECT * FROM writing_sessions")
    suspend fun getAllSessions(): List<WritingSession>

    @Query("SELECT * FROM writing_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): WritingSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WritingSession): Long

    @Query("DELETE FROM writing_sessions")
    suspend fun deleteAll()
}
