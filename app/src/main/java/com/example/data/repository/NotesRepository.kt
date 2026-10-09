package com.example.data.repository

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.entity.Note
import com.example.data.entity.NoteImage
import com.example.data.entity.NoteListItem
import com.example.data.entity.Section
import com.example.data.entity.SectionWithStats
import com.example.data.entity.Tag
import com.example.data.entity.TagWithCount
import com.example.data.entity.WritingSession
import com.example.export.NoteExportItem
import com.example.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class NotesRepository(private val context: Context, private val db: AppDatabase) {

    fun getSections(): Flow<List<SectionWithStats>> = db.sectionDao().getSectionsWithStats()

    fun getSection(id: Long): Flow<Section?> = db.sectionDao().getSectionById(id)

    suspend fun hasAnyData(): Boolean = withContext(Dispatchers.IO) {
        val sections = db.sectionDao().getAllSections()
        val goals = db.goalDao().getAllGoals()
        sections.isNotEmpty() || goals.isNotEmpty()
    }

    suspend fun createSection(name: String, colorIndex: Int = 0): Result<Long> = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Section name is required"))
        }
        val existing = db.sectionDao().getSectionByName(trimmed)
        if (existing != null) {
            return@withContext Result.failure(IllegalArgumentException("A section named \"$trimmed\" already exists"))
        }
        val all = db.sectionDao().getAllSections()
        val sortOrder = if (all.isEmpty()) 0 else (all.maxOfOrNull { it.sortOrder } ?: 0) + 1
        val id = db.sectionDao().insertSection(
            Section(
                name = trimmed,
                sortOrder = sortOrder,
                colorIndex = colorIndex % 7
            )
        )
        Result.success(id)
    }

    suspend fun renameSection(id: Long, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Section name is required"))
        }
        val existing = db.sectionDao().getSectionByName(trimmed)
        if (existing != null && existing.id != id) {
            return@withContext Result.failure(IllegalArgumentException("A section named \"$trimmed\" already exists"))
        }
        val section = db.sectionDao().getSectionByIdOnce(id) ?: return@withContext Result.failure(IllegalArgumentException("Section not found"))
        db.sectionDao().updateSection(section.copy(name = trimmed))
        Result.success(Unit)
    }

    suspend fun deleteSection(id: Long) = withContext(Dispatchers.IO) {
        // Collect images to delete from disk
        val notes = db.noteDao().getNotesForSectionOnce(id)
        notes.forEach { n ->
            val images = db.noteDao().getNoteImagesOnce(n.id)
            images.forEach { img ->
                FileUtils.deletePrivateFile(context, img.relativePath)
            }
        }
        db.sectionDao().deleteSectionById(id)
    }

    fun getTags(sectionId: Long): Flow<List<TagWithCount>> = db.tagDao().getTagsWithCountsForSection(sectionId)

    suspend fun createTag(sectionId: Long, name: String): Result<Long> = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Tag name cannot be empty"))
        }
        val existing = db.tagDao().getTagByNameAndSection(sectionId, trimmed)
        if (existing != null) {
            return@withContext Result.success(existing.id)
        }
        val id = db.tagDao().insertTag(
            Tag(
                sectionId = sectionId,
                name = trimmed
            )
        )
        Result.success(id)
    }

    fun getNotes(sectionId: Long, selectedTagIds: Set<Long>): Flow<List<NoteListItem>> {
        return if (selectedTagIds.isEmpty()) {
            db.noteDao().getNotesForSection(sectionId)
        } else {
            db.noteDao().getNotesForSectionAndTags(sectionId, selectedTagIds.toList())
        }
    }

    fun getNote(noteId: Long): Flow<Note?> = db.noteDao().getNoteById(noteId)

    fun getNoteImages(noteId: Long): Flow<List<NoteImage>> = db.noteDao().getNoteImages(noteId)

    fun getWritingSessions(noteId: Long): Flow<List<WritingSession>> = db.writingSessionDao().getWritingSessionsForNote(noteId)

    fun getTotalWritingTimeForNote(noteId: Long): Flow<Long> = db.writingSessionDao().getTotalWritingTimeForNote(noteId)

    suspend fun saveNote(
        noteId: Long?,
        sectionId: Long,
        tagId: Long,
        title: String,
        body: String,
        imageRelPaths: List<String>
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val finalNoteId: Long

        if (noteId == null || noteId == 0L) {
            val existingNotes = db.noteDao().getNotesForSectionOnce(sectionId)
            val nextSort = if (existingNotes.isEmpty()) 0 else (existingNotes.maxOfOrNull { it.sortOrder } ?: 0) + 1
            finalNoteId = db.noteDao().insertNote(
                Note(
                    sectionId = sectionId,
                    tagId = tagId,
                    title = title.trim(),
                    body = body,
                    createdAt = now,
                    updatedAt = now,
                    sortOrder = nextSort
                )
            )
        } else {
            val existing = db.noteDao().getNoteByIdOnce(noteId)!!
            db.noteDao().updateNote(
                existing.copy(
                    tagId = tagId,
                    title = title.trim(),
                    body = body,
                    updatedAt = now
                )
            )
            finalNoteId = noteId
        }

        // Sync note images
        val currentImages = db.noteDao().getNoteImagesOnce(finalNoteId)
        val currentPaths = currentImages.map { it.relativePath }.toSet()
        val newPaths = imageRelPaths.toSet()

        // Delete removed images from storage and db
        currentImages.filter { it.relativePath !in newPaths }.forEach { removed ->
            FileUtils.deletePrivateFile(context, removed.relativePath)
            db.noteDao().deleteNoteImage(removed)
        }

        // Replace / re-order images
        db.noteDao().deleteImagesForNote(finalNoteId)
        imageRelPaths.forEachIndexed { index, path ->
            db.noteDao().insertNoteImage(
                NoteImage(
                    noteId = finalNoteId,
                    relativePath = path,
                    sortOrder = index
                )
            )
        }

        finalNoteId
    }

    suspend fun deleteNote(noteId: Long) = withContext(Dispatchers.IO) {
        val images = db.noteDao().getNoteImagesOnce(noteId)
        images.forEach { img ->
            FileUtils.deletePrivateFile(context, img.relativePath)
        }
        db.noteDao().deleteNoteById(noteId)
    }

    suspend fun moveNoteOrder(sectionId: Long, fromIndex: Int, toIndex: Int) = withContext(Dispatchers.IO) {
        val notes = db.noteDao().getNotesForSectionOnce(sectionId).toMutableList()
        if (fromIndex in notes.indices && toIndex in notes.indices && fromIndex != toIndex) {
            val item = notes.removeAt(fromIndex)
            notes.add(toIndex, item)
            notes.forEachIndexed { index, noteItem ->
                db.noteDao().updateNoteSortOrder(noteItem.id, index)
            }
        }
    }

    suspend fun recordWritingSession(noteId: Long, startedAt: Long, endedAt: Long) = withContext(Dispatchers.IO) {
        val duration = (endedAt - startedAt).coerceAtLeast(0)
        // Store session if user spent at least 2 seconds
        if (duration >= 2000) {
            db.writingSessionDao().insertSession(
                WritingSession(
                    noteId = noteId,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    durationMs = duration
                )
            )
        }
    }

    suspend fun getExportItemsForSection(sectionId: Long, selectedTagIds: Set<Long>): Pair<String, List<NoteExportItem>> = withContext(Dispatchers.IO) {
        val section = db.sectionDao().getSectionByIdOnce(sectionId)
        val sectionName = section?.name ?: "Section"

        val notes = if (selectedTagIds.isEmpty()) {
            db.noteDao().getNotesForSectionOnce(sectionId)
        } else {
            db.noteDao().getNotesForSectionAndTagsOnce(sectionId, selectedTagIds.toList())
        }

        val exportItems = notes.map { n ->
            val images = db.noteDao().getNoteImagesOnce(n.id).map { it.relativePath }
            NoteExportItem(
                title = n.title,
                tagName = n.tagName,
                body = n.body,
                writingTimeMs = n.totalWritingTimeMs,
                images = images
            )
        }

        Pair(sectionName, exportItems)
    }

    suspend fun getExportItemForSingleNote(noteId: Long): Pair<String, List<NoteExportItem>> = withContext(Dispatchers.IO) {
        val note = db.noteDao().getNoteByIdOnce(noteId) ?: return@withContext Pair("Note", emptyList())
        val section = db.sectionDao().getSectionByIdOnce(note.sectionId)
        val sectionName = section?.name ?: "Section"
        val tag = db.tagDao().getTagById(note.tagId)
        val tagName = tag?.name ?: "Untagged"
        val images = db.noteDao().getNoteImagesOnce(noteId).map { it.relativePath }
        val sessions = db.writingSessionDao().getAllSessions().filter { it.noteId == noteId }
        val totalWritingTime = sessions.sumOf { it.durationMs }

        val item = NoteExportItem(
            title = note.title,
            tagName = tagName,
            body = note.body,
            writingTimeMs = totalWritingTime,
            images = images
        )

        Pair(sectionName, listOf(item))
    }
}
