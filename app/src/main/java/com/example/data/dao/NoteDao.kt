package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.Note
import com.example.data.entity.NoteImage
import com.example.data.entity.NoteListItem
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("""
        SELECT n.id, n.sectionId, n.tagId, n.title, n.body, n.createdAt, n.updatedAt, n.sortOrder,
            t.name AS tagName,
            (SELECT COALESCE(SUM(durationMs), 0) FROM writing_sessions WHERE noteId = n.id) AS totalWritingTimeMs,
            (SELECT relativePath FROM note_images WHERE noteId = n.id ORDER BY sortOrder ASC LIMIT 1) AS firstImageThumbnail
        FROM notes n
        JOIN tags t ON t.id = n.tagId
        WHERE n.sectionId = :sectionId
        ORDER BY n.sortOrder ASC, n.createdAt ASC
    """)
    fun getNotesForSection(sectionId: Long): Flow<List<NoteListItem>>

    @Query("""
        SELECT n.id, n.sectionId, n.tagId, n.title, n.body, n.createdAt, n.updatedAt, n.sortOrder,
            t.name AS tagName,
            (SELECT COALESCE(SUM(durationMs), 0) FROM writing_sessions WHERE noteId = n.id) AS totalWritingTimeMs,
            (SELECT relativePath FROM note_images WHERE noteId = n.id ORDER BY sortOrder ASC LIMIT 1) AS firstImageThumbnail
        FROM notes n
        JOIN tags t ON t.id = n.tagId
        WHERE n.sectionId = :sectionId AND n.tagId IN (:tagIds)
        ORDER BY n.sortOrder ASC, n.createdAt ASC
    """)
    fun getNotesForSectionAndTags(sectionId: Long, tagIds: List<Long>): Flow<List<NoteListItem>>

    @Query("""
        SELECT n.id, n.sectionId, n.tagId, n.title, n.body, n.createdAt, n.updatedAt, n.sortOrder,
            t.name AS tagName,
            (SELECT COALESCE(SUM(durationMs), 0) FROM writing_sessions WHERE noteId = n.id) AS totalWritingTimeMs,
            (SELECT relativePath FROM note_images WHERE noteId = n.id ORDER BY sortOrder ASC LIMIT 1) AS firstImageThumbnail
        FROM notes n
        JOIN tags t ON t.id = n.tagId
        WHERE n.sectionId = :sectionId
        ORDER BY n.sortOrder ASC, n.createdAt ASC
    """)
    suspend fun getNotesForSectionOnce(sectionId: Long): List<NoteListItem>

    @Query("""
        SELECT n.id, n.sectionId, n.tagId, n.title, n.body, n.createdAt, n.updatedAt, n.sortOrder,
            t.name AS tagName,
            (SELECT COALESCE(SUM(durationMs), 0) FROM writing_sessions WHERE noteId = n.id) AS totalWritingTimeMs,
            (SELECT relativePath FROM note_images WHERE noteId = n.id ORDER BY sortOrder ASC LIMIT 1) AS firstImageThumbnail
        FROM notes n
        JOIN tags t ON t.id = n.tagId
        WHERE n.sectionId = :sectionId AND n.tagId IN (:tagIds)
        ORDER BY n.sortOrder ASC, n.createdAt ASC
    """)
    suspend fun getNotesForSectionAndTagsOnce(sectionId: Long, tagIds: List<Long>): List<NoteListItem>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun getNoteById(id: Long): Flow<Note?>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteByIdOnce(id: Long): Note?

    @Query("SELECT * FROM notes WHERE sectionId = :sectionId AND LOWER(title) = LOWER(:title) LIMIT 1")
    suspend fun getNoteByTitleAndSection(sectionId: Long, title: String): Note?

    @Query("SELECT * FROM notes")
    suspend fun getAllNotes(): List<Note>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("UPDATE notes SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateNoteSortOrder(id: Long, sortOrder: Int)

    // Note Images
    @Query("SELECT * FROM note_images WHERE noteId = :noteId ORDER BY sortOrder ASC")
    fun getNoteImages(noteId: Long): Flow<List<NoteImage>>

    @Query("SELECT * FROM note_images WHERE noteId = :noteId ORDER BY sortOrder ASC")
    suspend fun getNoteImagesOnce(noteId: Long): List<NoteImage>

    @Query("SELECT * FROM note_images")
    suspend fun getAllNoteImages(): List<NoteImage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteImage(image: NoteImage): Long

    @Delete
    suspend fun deleteNoteImage(image: NoteImage)

    @Query("DELETE FROM note_images WHERE noteId = :noteId")
    suspend fun deleteImagesForNote(noteId: Long)

    @Query("DELETE FROM note_images WHERE id = :id")
    suspend fun deleteNoteImageById(id: Long)

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    @Query("DELETE FROM note_images")
    suspend fun deleteAllNoteImages()
}
