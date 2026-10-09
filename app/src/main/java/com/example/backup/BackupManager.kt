package com.example.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.database.AppDatabase
import com.example.data.entity.Goal
import com.example.data.entity.GoalSession
import com.example.data.entity.Note
import com.example.data.entity.NoteImage
import com.example.data.entity.Section
import com.example.data.entity.SubGoal
import com.example.data.entity.Tag
import com.example.data.entity.WritingSession
import com.example.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupManifest(
    val version: Int = 1,
    val exportedAt: Long,
    val sectionsCount: Int,
    val tagsCount: Int,
    val notesCount: Int,
    val noteImagesCount: Int,
    val writingSessionsCount: Int,
    val goalsCount: Int,
    val subGoalsCount: Int,
    val goalSessionsCount: Int
)

data class ParsedBackup(
    val manifest: BackupManifest,
    val sections: List<Section>,
    val tags: List<Tag>,
    val notes: List<Note>,
    val noteImages: List<NoteImage>,
    val writingSessions: List<WritingSession>,
    val goals: List<Goal>,
    val subGoals: List<SubGoal>,
    val goalSessions: List<GoalSession>,
    val extractedImagesDir: File
)

data class NoteConflict(
    val importedNoteIndex: Int,
    val title: String,
    val sectionName: String
)

enum class ConflictResolution {
    KEEP_BOTH,
    SKIP
}

class BackupManager(private val context: Context, private val db: AppDatabase) {

    suspend fun createBackupZip(): Uri = withContext(Dispatchers.IO) {
        val exportsDir = FileUtils.getExportsDirectory(context)
        val backupFile = File(exportsDir, "fieldnotes_backup_${System.currentTimeMillis()}.zip")

        val sections = db.sectionDao().getAllSections()
        val tags = db.tagDao().getAllTags()
        val notes = db.noteDao().getAllNotes()
        val noteImages = db.noteDao().getAllNoteImages()
        val writingSessions = db.writingSessionDao().getAllSessions()
        val goals = db.goalDao().getAllGoals()
        val subGoals = db.goalDao().getAllSubGoals()
        val goalSessions = db.goalSessionDao().getAllSessions()

        val manifest = BackupManifest(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            sectionsCount = sections.size,
            tagsCount = tags.size,
            notesCount = notes.size,
            noteImagesCount = noteImages.size,
            writingSessionsCount = writingSessions.size,
            goalsCount = goals.size,
            subGoalsCount = subGoals.size,
            goalSessionsCount = goalSessions.size
        )

        // Generate manifest JSON
        val manifestJson = JSONObject().apply {
            put("version", manifest.version)
            put("exportedAt", manifest.exportedAt)
            put("sectionsCount", manifest.sectionsCount)
            put("tagsCount", manifest.tagsCount)
            put("notesCount", manifest.notesCount)
            put("noteImagesCount", manifest.noteImagesCount)
            put("writingSessionsCount", manifest.writingSessionsCount)
            put("goalsCount", manifest.goalsCount)
            put("subGoalsCount", manifest.subGoalsCount)
            put("goalSessionsCount", manifest.goalSessionsCount)
        }

        // Generate data JSON
        val dataJson = JSONObject().apply {
            put("sections", JSONArray().apply {
                sections.forEach { s ->
                    put(JSONObject().apply {
                        put("id", s.id)
                        put("name", s.name)
                        put("createdAt", s.createdAt)
                        put("sortOrder", s.sortOrder)
                        put("colorIndex", s.colorIndex)
                    })
                }
            })
            put("tags", JSONArray().apply {
                tags.forEach { t ->
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("sectionId", t.sectionId)
                        put("name", t.name)
                        put("createdAt", t.createdAt)
                    })
                }
            })
            put("notes", JSONArray().apply {
                notes.forEach { n ->
                    put(JSONObject().apply {
                        put("id", n.id)
                        put("sectionId", n.sectionId)
                        put("tagId", n.tagId)
                        put("title", n.title)
                        put("body", n.body)
                        put("createdAt", n.createdAt)
                        put("updatedAt", n.updatedAt)
                        put("sortOrder", n.sortOrder)
                    })
                }
            })
            put("noteImages", JSONArray().apply {
                noteImages.forEach { img ->
                    put(JSONObject().apply {
                        put("id", img.id)
                        put("noteId", img.noteId)
                        put("relativePath", img.relativePath)
                        put("sortOrder", img.sortOrder)
                    })
                }
            })
            put("writingSessions", JSONArray().apply {
                writingSessions.forEach { ws ->
                    put(JSONObject().apply {
                        put("id", ws.id)
                        put("noteId", ws.noteId)
                        put("startedAt", ws.startedAt)
                        put("endedAt", ws.endedAt)
                        put("durationMs", ws.durationMs)
                    })
                }
            })
            put("goals", JSONArray().apply {
                goals.forEach { g ->
                    put(JSONObject().apply {
                        put("id", g.id)
                        put("name", g.name)
                        put("createdAt", g.createdAt)
                        put("sortOrder", g.sortOrder)
                    })
                }
            })
            put("subGoals", JSONArray().apply {
                subGoals.forEach { sg ->
                    put(JSONObject().apply {
                        put("id", sg.id)
                        put("goalId", sg.goalId)
                        put("name", sg.name)
                        put("createdAt", sg.createdAt)
                        put("sortOrder", sg.sortOrder)
                    })
                }
            })
            put("goalSessions", JSONArray().apply {
                goalSessions.forEach { gs ->
                    put(JSONObject().apply {
                        put("id", gs.id)
                        put("goalId", gs.goalId)
                        if (gs.subGoalId != null) put("subGoalId", gs.subGoalId)
                        put("startedAt", gs.startedAt)
                        put("endedAt", gs.endedAt)
                        put("durationMs", gs.durationMs)
                    })
                }
            })
        }

        ZipOutputStream(FileOutputStream(backupFile)).use { zos ->
            // Entry 1: manifest.json
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(manifestJson.toString(2).toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // Entry 2: data.json
            zos.putNextEntry(ZipEntry("data.json"))
            zos.write(dataJson.toString().toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // Entry 3...: images/
            noteImages.forEach { img ->
                val imageFile = FileUtils.getFileFromRelativePath(context, img.relativePath)
                if (imageFile.exists()) {
                    zos.putNextEntry(ZipEntry(img.relativePath))
                    FileInputStream(imageFile).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        }

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )
    }

    suspend fun parseBackupZip(zipUri: Uri): ParsedBackup = withContext(Dispatchers.IO) {
        val tempExtractDir = File(context.cacheDir, "import_temp_${System.currentTimeMillis()}").apply { mkdirs() }
        val imagesExtractDir = File(tempExtractDir, "images").apply { mkdirs() }

        var manifestJsonString: String? = null
        var dataJsonString: String? = null

        context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "manifest.json" -> {
                            manifestJsonString = zis.bufferedReader(Charsets.UTF_8).readText()
                        }
                        entry.name == "data.json" -> {
                            dataJsonString = zis.bufferedReader(Charsets.UTF_8).readText()
                        }
                        entry.name.startsWith("images/") -> {
                            val fileName = File(entry.name).name
                            if (fileName.isNotEmpty()) {
                                val outFile = File(imagesExtractDir, fileName)
                                FileOutputStream(outFile).use { fos ->
                                    zis.copyTo(fos)
                                }
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } ?: throw IllegalArgumentException("Could not open zip input stream")

        if (manifestJsonString == null || dataJsonString == null) {
            throw IllegalArgumentException("Invalid backup archive: missing manifest.json or data.json")
        }

        val manifestObj = JSONObject(manifestJsonString!!)
        val manifest = BackupManifest(
            version = manifestObj.optInt("version", 1),
            exportedAt = manifestObj.optLong("exportedAt", System.currentTimeMillis()),
            sectionsCount = manifestObj.optInt("sectionsCount", 0),
            tagsCount = manifestObj.optInt("tagsCount", 0),
            notesCount = manifestObj.optInt("notesCount", 0),
            noteImagesCount = manifestObj.optInt("noteImagesCount", 0),
            writingSessionsCount = manifestObj.optInt("writingSessionsCount", 0),
            goalsCount = manifestObj.optInt("goalsCount", 0),
            subGoalsCount = manifestObj.optInt("subGoalsCount", 0),
            goalSessionsCount = manifestObj.optInt("goalSessionsCount", 0)
        )

        val dataObj = JSONObject(dataJsonString!!)

        val sections = mutableListOf<Section>()
        val sectionsArray = dataObj.optJSONArray("sections") ?: JSONArray()
        for (i in 0 until sectionsArray.length()) {
            val obj = sectionsArray.getJSONObject(i)
            sections.add(
                Section(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    sortOrder = obj.optInt("sortOrder", 0),
                    colorIndex = obj.optInt("colorIndex", 0)
                )
            )
        }

        val tags = mutableListOf<Tag>()
        val tagsArray = dataObj.optJSONArray("tags") ?: JSONArray()
        for (i in 0 until tagsArray.length()) {
            val obj = tagsArray.getJSONObject(i)
            tags.add(
                Tag(
                    id = obj.getLong("id"),
                    sectionId = obj.getLong("sectionId"),
                    name = obj.getString("name"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val notes = mutableListOf<Note>()
        val notesArray = dataObj.optJSONArray("notes") ?: JSONArray()
        for (i in 0 until notesArray.length()) {
            val obj = notesArray.getJSONObject(i)
            notes.add(
                Note(
                    id = obj.getLong("id"),
                    sectionId = obj.getLong("sectionId"),
                    tagId = obj.getLong("tagId"),
                    title = obj.getString("title"),
                    body = obj.optString("body", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                    sortOrder = obj.optInt("sortOrder", 0)
                )
            )
        }

        val noteImages = mutableListOf<NoteImage>()
        val noteImagesArray = dataObj.optJSONArray("noteImages") ?: JSONArray()
        for (i in 0 until noteImagesArray.length()) {
            val obj = noteImagesArray.getJSONObject(i)
            noteImages.add(
                NoteImage(
                    id = obj.getLong("id"),
                    noteId = obj.getLong("noteId"),
                    relativePath = obj.getString("relativePath"),
                    sortOrder = obj.optInt("sortOrder", 0)
                )
            )
        }

        val writingSessions = mutableListOf<WritingSession>()
        val writingSessionsArray = dataObj.optJSONArray("writingSessions") ?: JSONArray()
        for (i in 0 until writingSessionsArray.length()) {
            val obj = writingSessionsArray.getJSONObject(i)
            writingSessions.add(
                WritingSession(
                    id = obj.getLong("id"),
                    noteId = obj.getLong("noteId"),
                    startedAt = obj.getLong("startedAt"),
                    endedAt = obj.getLong("endedAt"),
                    durationMs = obj.getLong("durationMs")
                )
            )
        }

        val goals = mutableListOf<Goal>()
        val goalsArray = dataObj.optJSONArray("goals") ?: JSONArray()
        for (i in 0 until goalsArray.length()) {
            val obj = goalsArray.getJSONObject(i)
            goals.add(
                Goal(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    sortOrder = obj.optInt("sortOrder", 0)
                )
            )
        }

        val subGoals = mutableListOf<SubGoal>()
        val subGoalsArray = dataObj.optJSONArray("subGoals") ?: JSONArray()
        for (i in 0 until subGoalsArray.length()) {
            val obj = subGoalsArray.getJSONObject(i)
            subGoals.add(
                SubGoal(
                    id = obj.getLong("id"),
                    goalId = obj.getLong("goalId"),
                    name = obj.getString("name"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    sortOrder = obj.optInt("sortOrder", 0)
                )
            )
        }

        val goalSessions = mutableListOf<GoalSession>()
        val goalSessionsArray = dataObj.optJSONArray("goalSessions") ?: JSONArray()
        for (i in 0 until goalSessionsArray.length()) {
            val obj = goalSessionsArray.getJSONObject(i)
            goalSessions.add(
                GoalSession(
                    id = obj.getLong("id"),
                    goalId = obj.getLong("goalId"),
                    subGoalId = if (obj.has("subGoalId") && !obj.isNull("subGoalId")) obj.getLong("subGoalId") else null,
                    startedAt = obj.getLong("startedAt"),
                    endedAt = obj.getLong("endedAt"),
                    durationMs = obj.getLong("durationMs")
                )
            )
        }

        ParsedBackup(
            manifest = manifest,
            sections = sections,
            tags = tags,
            notes = notes,
            noteImages = noteImages,
            writingSessions = writingSessions,
            goals = goals,
            subGoals = subGoals,
            goalSessions = goalSessions,
            extractedImagesDir = imagesExtractDir
        )
    }

    suspend fun findConflicts(parsed: ParsedBackup): List<NoteConflict> = withContext(Dispatchers.IO) {
        val conflicts = mutableListOf<NoteConflict>()
        val sectionMap = parsed.sections.associateBy { it.id }

        parsed.notes.forEachIndexed { index, note ->
            val secName = sectionMap[note.sectionId]?.name ?: ""
            val existingSec = db.sectionDao().getSectionByName(secName)
            if (existingSec != null) {
                val existingNote = db.noteDao().getNoteByTitleAndSection(existingSec.id, note.title)
                if (existingNote != null) {
                    conflicts.add(
                        NoteConflict(
                            importedNoteIndex = index,
                            title = note.title,
                            sectionName = secName
                        )
                    )
                }
            }
        }
        conflicts
    }

    suspend fun restoreReplace(parsed: ParsedBackup) = withContext(Dispatchers.IO) {
        // Clear all tables
        db.runningTimerDao().clearRunningTimer()
        db.goalSessionDao().deleteAll()
        db.goalDao().deleteAllSubGoals()
        db.goalDao().deleteAllGoals()
        db.writingSessionDao().deleteAll()
        db.noteDao().deleteAllNoteImages()
        db.noteDao().deleteAllNotes()
        db.tagDao().deleteAll()
        db.sectionDao().deleteAll()

        // Clear existing private images
        val internalImagesDir = FileUtils.getImagesDirectory(context)
        internalImagesDir.listFiles()?.forEach { it.delete() }

        // Copy unpacked images
        parsed.extractedImagesDir.listFiles()?.forEach { src ->
            val dst = File(internalImagesDir, src.name)
            src.copyTo(dst, overwrite = true)
        }

        // Mapping old IDs to new generated IDs
        val sectionIdMap = mutableMapOf<Long, Long>()
        parsed.sections.forEach { s ->
            val newId = db.sectionDao().insertSection(s.copy(id = 0))
            sectionIdMap[s.id] = newId
        }

        val tagIdMap = mutableMapOf<Long, Long>()
        parsed.tags.forEach { t ->
            val newSecId = sectionIdMap[t.sectionId] ?: return@forEach
            val newId = db.tagDao().insertTag(t.copy(id = 0, sectionId = newSecId))
            tagIdMap[t.id] = newId
        }

        val noteIdMap = mutableMapOf<Long, Long>()
        parsed.notes.forEach { n ->
            val newSecId = sectionIdMap[n.sectionId] ?: return@forEach
            val newTagId = tagIdMap[n.tagId] ?: return@forEach
            val newId = db.noteDao().insertNote(n.copy(id = 0, sectionId = newSecId, tagId = newTagId))
            noteIdMap[n.id] = newId
        }

        parsed.noteImages.forEach { img ->
            val newNoteId = noteIdMap[img.noteId] ?: return@forEach
            db.noteDao().insertNoteImage(img.copy(id = 0, noteId = newNoteId))
        }

        parsed.writingSessions.forEach { ws ->
            val newNoteId = noteIdMap[ws.noteId] ?: return@forEach
            db.writingSessionDao().insertSession(ws.copy(id = 0, noteId = newNoteId))
        }

        val goalIdMap = mutableMapOf<Long, Long>()
        parsed.goals.forEach { g ->
            val newId = db.goalDao().insertGoal(g.copy(id = 0))
            goalIdMap[g.id] = newId
        }

        val subGoalIdMap = mutableMapOf<Long, Long>()
        parsed.subGoals.forEach { sg ->
            val newGoalId = goalIdMap[sg.goalId] ?: return@forEach
            val newId = db.goalDao().insertSubGoal(sg.copy(id = 0, goalId = newGoalId))
            subGoalIdMap[sg.id] = newId
        }

        parsed.goalSessions.forEach { gs ->
            val newGoalId = goalIdMap[gs.goalId] ?: return@forEach
            val newSubGoalId = gs.subGoalId?.let { subGoalIdMap[it] }
            db.goalSessionDao().insertGoalSession(gs.copy(id = 0, goalId = newGoalId, subGoalId = newSubGoalId))
        }

        // Clean up extracted temp dir
        parsed.extractedImagesDir.parentFile?.deleteRecursively()
    }

    suspend fun restoreMerge(
        parsed: ParsedBackup,
        resolutions: Map<Int, ConflictResolution> // noteIndex -> resolution
    ) = withContext(Dispatchers.IO) {
        val internalImagesDir = FileUtils.getImagesDirectory(context)

        // 1. Sections merge (case-insensitive name)
        val sectionIdMap = mutableMapOf<Long, Long>()
        parsed.sections.forEach { s ->
            val existing = db.sectionDao().getSectionByName(s.name)
            if (existing != null) {
                sectionIdMap[s.id] = existing.id
            } else {
                val newId = db.sectionDao().insertSection(s.copy(id = 0))
                sectionIdMap[s.id] = newId
            }
        }

        // 2. Tags merge (reuse existing by section & name)
        val tagIdMap = mutableMapOf<Long, Long>()
        parsed.tags.forEach { t ->
            val targetSecId = sectionIdMap[t.sectionId] ?: return@forEach
            val existing = db.tagDao().getTagByNameAndSection(targetSecId, t.name)
            if (existing != null) {
                tagIdMap[t.id] = existing.id
            } else {
                val newId = db.tagDao().insertTag(t.copy(id = 0, sectionId = targetSecId))
                tagIdMap[t.id] = newId
            }
        }

        // 3. Notes merge
        val noteIdMap = mutableMapOf<Long, Long>()
        parsed.notes.forEachIndexed { index, note ->
            val targetSecId = sectionIdMap[note.sectionId] ?: return@forEachIndexed
            val targetTagId = tagIdMap[note.tagId] ?: return@forEachIndexed

            val resolution = resolutions[index]
            if (resolution == ConflictResolution.SKIP) {
                return@forEachIndexed // skip note, its images, and its writing sessions
            }

            var titleToUse = note.title
            if (resolution == ConflictResolution.KEEP_BOTH) {
                // If title collides, append " (Imported)"
                val existing = db.noteDao().getNoteByTitleAndSection(targetSecId, note.title)
                if (existing != null) {
                    titleToUse = "${note.title} (Imported)"
                }
            }

            val newNoteId = db.noteDao().insertNote(
                note.copy(
                    id = 0,
                    sectionId = targetSecId,
                    tagId = targetTagId,
                    title = titleToUse
                )
            )
            noteIdMap[note.id] = newNoteId
        }

        // 4. Note images
        parsed.noteImages.forEach { img ->
            val newNoteId = noteIdMap[img.noteId] ?: return@forEach
            val srcFile = File(parsed.extractedImagesDir, File(img.relativePath).name)
            if (srcFile.exists()) {
                val destFileName = "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
                val destFile = File(internalImagesDir, destFileName)
                srcFile.copyTo(destFile, overwrite = true)
                db.noteDao().insertNoteImage(
                    img.copy(
                        id = 0,
                        noteId = newNoteId,
                        relativePath = "images/$destFileName"
                    )
                )
            }
        }

        // 5. Writing sessions
        parsed.writingSessions.forEach { ws ->
            val newNoteId = noteIdMap[ws.noteId] ?: return@forEach
            db.writingSessionDao().insertSession(ws.copy(id = 0, noteId = newNoteId))
        }

        // 6. Goals merge (case-insensitive name)
        val goalIdMap = mutableMapOf<Long, Long>()
        parsed.goals.forEach { g ->
            val existing = db.goalDao().getGoalByName(g.name)
            if (existing != null) {
                goalIdMap[g.id] = existing.id
            } else {
                val newId = db.goalDao().insertGoal(g.copy(id = 0))
                goalIdMap[g.id] = newId
            }
        }

        // 7. Sub-goals merge
        val subGoalIdMap = mutableMapOf<Long, Long>()
        parsed.subGoals.forEach { sg ->
            val targetGoalId = goalIdMap[sg.goalId] ?: return@forEach
            val existing = db.goalDao().getSubGoalByName(targetGoalId, sg.name)
            if (existing != null) {
                subGoalIdMap[sg.id] = existing.id
            } else {
                val newId = db.goalDao().insertSubGoal(sg.copy(id = 0, goalId = targetGoalId))
                subGoalIdMap[sg.id] = newId
            }
        }

        // 8. Goal sessions
        parsed.goalSessions.forEach { gs ->
            val targetGoalId = goalIdMap[gs.goalId] ?: return@forEach
            val targetSubGoalId = gs.subGoalId?.let { subGoalIdMap[it] }

            // Deduplicate only if exact session ID matches existing session
            val existingSession = db.goalSessionDao().getSessionById(gs.id)
            if (existingSession == null) {
                db.goalSessionDao().insertGoalSession(
                    gs.copy(
                        id = 0,
                        goalId = targetGoalId,
                        subGoalId = targetSubGoalId
                    )
                )
            }
        }

        // Clean up extracted temp dir
        parsed.extractedImagesDir.parentFile?.deleteRecursively()
    }
}
