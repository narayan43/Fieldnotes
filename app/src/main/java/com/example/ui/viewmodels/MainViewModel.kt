package com.example.ui.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.backup.BackupManager
import com.example.backup.ConflictResolution
import com.example.backup.NoteConflict
import com.example.backup.ParsedBackup
import com.example.data.database.AppDatabase
import com.example.data.entity.Goal
import com.example.data.entity.GoalWithStats
import com.example.data.entity.NoteListItem
import com.example.data.entity.RunningTimer
import com.example.data.entity.Section
import com.example.data.entity.SectionWithStats
import com.example.data.entity.SubGoalWithStats
import com.example.data.entity.TagWithCount
import com.example.data.repository.GoalDashboardStats
import com.example.data.repository.GoalsRepository
import com.example.data.repository.NotesRepository
import com.example.export.ExportHelper
import com.example.service.TimerForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class Screen {
    object NotesHome : Screen()
    data class SectionDetail(val sectionId: Long) : Screen()
    data class NoteEditor(val sectionId: Long, val noteId: Long?) : Screen()

    object GoalsHome : Screen()
    data class GoalDetail(val goalId: Long) : Screen()

    object Settings : Screen()
}

enum class BottomTab {
    NOTES,
    GOALS,
    SETTINGS
}

data class ActiveEditorState(
    val sectionId: Long = 0L,
    val noteId: Long? = null,
    val title: String = "",
    val body: String = "",
    val selectedTagId: Long? = null,
    val images: List<String> = emptyList(), // relative paths
    val baseWritingTimeMs: Long = 0L,
    val sessionStartedAt: Long = 0L,
    val currentSessionElapsedMs: Long = 0L
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val notesRepo = NotesRepository(application, db)
    val goalsRepo = GoalsRepository(db)
    val backupManager = BackupManager(application, db)

    // Navigation state
    private val _currentTab = MutableStateFlow(BottomTab.NOTES)
    val currentTab: StateFlow<BottomTab> = _currentTab.asStateFlow()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.NotesHome)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Notes Data
    val sections: StateFlow<List<SectionWithStats>> = notesRepo.getSections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Section Detail Filtering
    private val _activeSectionId = MutableStateFlow<Long?>(null)
    val activeSectionId: StateFlow<Long?> = _activeSectionId.asStateFlow()

    private val _selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTagIds: StateFlow<Set<Long>> = _selectedTagIds.asStateFlow()

    // Goals Data
    val goals: StateFlow<List<GoalWithStats>> = goalsRepo.getGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val runningTimer: StateFlow<RunningTimer?> = goalsRepo.getRunningTimer()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dashboardStats: StateFlow<GoalDashboardStats> = goalsRepo.getDashboardStats()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            GoalDashboardStats(emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyList())
        )

    // Note Editor State
    private val _editorState = MutableStateFlow(ActiveEditorState())
    val editorState: StateFlow<ActiveEditorState> = _editorState.asStateFlow()

    private var editorTickerJob: Job? = null

    // Backup & Restore State
    private val _showFirstLaunchRestore = MutableStateFlow(false)
    val showFirstLaunchRestore: StateFlow<Boolean> = _showFirstLaunchRestore.asStateFlow()

    private val _pendingImport = MutableStateFlow<ParsedBackup?>(null)
    val pendingImport: StateFlow<ParsedBackup?> = _pendingImport.asStateFlow()

    private val _importConflicts = MutableStateFlow<List<NoteConflict>>(emptyList())
    val importConflicts: StateFlow<List<NoteConflict>> = _importConflicts.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        checkFirstLaunch()
    }

    private fun checkFirstLaunch() {
        val prefs = getApplication<Application>().getSharedPreferences("fieldnotes_prefs", 0)
        val hasLaunched = prefs.getBoolean("has_launched_before", false)
        if (!hasLaunched) {
            viewModelScope.launch {
                val hasData = notesRepo.hasAnyData()
                if (!hasData) {
                    _showFirstLaunchRestore.value = true
                }
                prefs.edit().putBoolean("has_launched_before", true).apply()
            }
        }
    }

    fun dismissFirstLaunchPrompt() {
        _showFirstLaunchRestore.value = false
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    // Navigation actions
    fun switchTab(tab: BottomTab) {
        _currentTab.value = tab
        _currentScreen.value = when (tab) {
            BottomTab.NOTES -> Screen.NotesHome
            BottomTab.GOALS -> Screen.GoalsHome
            BottomTab.SETTINGS -> Screen.Settings
        }
    }

    fun openSection(sectionId: Long) {
        _activeSectionId.value = sectionId
        _selectedTagIds.value = emptySet()
        _currentScreen.value = Screen.SectionDetail(sectionId)
    }

    fun toggleTagFilter(tagId: Long) {
        val current = _selectedTagIds.value.toMutableSet()
        if (current.contains(tagId)) {
            current.remove(tagId)
        } else {
            current.add(tagId)
        }
        _selectedTagIds.value = current
    }

    fun clearTagFilters() {
        _selectedTagIds.value = emptySet()
    }

    fun openNoteEditor(sectionId: Long, noteId: Long?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (noteId != null) {
                val note = db.noteDao().getNoteByIdOnce(noteId)
                val images = db.noteDao().getNoteImagesOnce(noteId).map { it.relativePath }
                val sessions = db.writingSessionDao().getAllSessions().filter { it.noteId == noteId }
                val baseTime = sessions.sumOf { it.durationMs }

                _editorState.value = ActiveEditorState(
                    sectionId = sectionId,
                    noteId = noteId,
                    title = note?.title ?: "",
                    body = note?.body ?: "",
                    selectedTagId = note?.tagId,
                    images = images,
                    baseWritingTimeMs = baseTime,
                    sessionStartedAt = now,
                    currentSessionElapsedMs = 0L
                )
            } else {
                // If creating a note and tags exist, default to first tag or null
                val tags = db.tagDao().getTagsForSection(sectionId)
                val defaultTagId = tags.firstOrNull()?.id

                _editorState.value = ActiveEditorState(
                    sectionId = sectionId,
                    noteId = null,
                    title = "",
                    body = "",
                    selectedTagId = defaultTagId,
                    images = emptyList(),
                    baseWritingTimeMs = 0L,
                    sessionStartedAt = now,
                    currentSessionElapsedMs = 0L
                )
            }

            startEditorTimer()
            _currentScreen.value = Screen.NoteEditor(sectionId, noteId)
        }
    }

    private fun startEditorTimer() {
        editorTickerJob?.cancel()
        editorTickerJob = viewModelScope.launch {
            val start = _editorState.value.sessionStartedAt
            while (isActive) {
                delay(1000)
                val elapsed = System.currentTimeMillis() - start
                _editorState.value = _editorState.value.copy(currentSessionElapsedMs = elapsed)
            }
        }
    }

    fun updateEditorTitle(title: String) {
        _editorState.value = _editorState.value.copy(title = title)
    }

    fun updateEditorBody(body: String) {
        _editorState.value = _editorState.value.copy(body = body)
    }

    fun updateEditorTag(tagId: Long) {
        _editorState.value = _editorState.value.copy(selectedTagId = tagId)
    }

    fun addEditorImage(relativePath: String) {
        val list = _editorState.value.images.toMutableList()
        list.add(relativePath)
        _editorState.value = _editorState.value.copy(images = list)
    }

    fun removeEditorImage(index: Int) {
        val list = _editorState.value.images.toMutableList()
        if (index in list.indices) {
            val removed = list.removeAt(index)
            // If image is newly added in this session and not yet saved in note, delete file
            viewModelScope.launch(Dispatchers.IO) {
                com.example.util.FileUtils.deletePrivateFile(getApplication(), removed)
            }
            _editorState.value = _editorState.value.copy(images = list)
        }
    }

    fun moveEditorImage(from: Int, to: Int) {
        val list = _editorState.value.images.toMutableList()
        if (from in list.indices && to in list.indices && from != to) {
            val item = list.removeAt(from)
            list.add(to, item)
            _editorState.value = _editorState.value.copy(images = list)
        }
    }

    fun saveNoteAndExit(onSuccess: () -> Unit) {
        val state = _editorState.value
        if (state.title.trim().isEmpty()) {
            showMessage("Title is required to save note")
            return
        }
        val tagId = state.selectedTagId
        if (tagId == null) {
            showMessage("Please select or create a tag for this note")
            return
        }

        editorTickerJob?.cancel()
        val now = System.currentTimeMillis()

        viewModelScope.launch {
            val savedNoteId = notesRepo.saveNote(
                noteId = state.noteId,
                sectionId = state.sectionId,
                tagId = tagId,
                title = state.title,
                body = state.body,
                imageRelPaths = state.images
            )
            // Log writing session
            notesRepo.recordWritingSession(savedNoteId, state.sessionStartedAt, now)

            _currentScreen.value = Screen.SectionDetail(state.sectionId)
            onSuccess()
        }
    }

    fun cancelEditor() {
        val state = _editorState.value
        editorTickerJob?.cancel()
        val now = System.currentTimeMillis()

        // If editing an existing note and leaving without save, still record writing time spent
        if (state.noteId != null && state.noteId > 0) {
            viewModelScope.launch {
                notesRepo.recordWritingSession(state.noteId, state.sessionStartedAt, now)
            }
        }
        _currentScreen.value = Screen.SectionDetail(state.sectionId)
    }

    fun openGoalDetail(goalId: Long) {
        _currentScreen.value = Screen.GoalDetail(goalId)
    }

    fun navigateBack() {
        when (val screen = _currentScreen.value) {
            is Screen.NoteEditor -> cancelEditor()
            is Screen.SectionDetail -> {
                _activeSectionId.value = null
                _currentScreen.value = Screen.NotesHome
            }
            is Screen.GoalDetail -> {
                _currentScreen.value = Screen.GoalsHome
            }
            else -> {}
        }
    }

    // Goal Timer Actions
    fun startGoalTimer(goalId: Long, subGoalId: Long? = null) {
        TimerForegroundService.startTimer(getApplication(), goalId, subGoalId)
    }

    fun stopGoalTimer() {
        TimerForegroundService.stopTimer(getApplication())
    }

    // Export Actions
    fun exportCurrentViewAsText() {
        viewModelScope.launch(Dispatchers.IO) {
            when (val screen = _currentScreen.value) {
                is Screen.NoteEditor -> {
                    val noteId = screen.noteId
                    if (noteId != null) {
                        val (secName, items) = notesRepo.getExportItemForSingleNote(noteId)
                        val uri = ExportHelper.exportPlainText(getApplication(), secName, items)
                        ExportHelper.shareExport(getApplication(), uri, "text/plain", "Export Note as Plain Text")
                    }
                }
                is Screen.SectionDetail -> {
                    val (secName, items) = notesRepo.getExportItemsForSection(screen.sectionId, _selectedTagIds.value)
                    val uri = ExportHelper.exportPlainText(getApplication(), secName, items)
                    ExportHelper.shareExport(getApplication(), uri, "text/plain", "Export Section as Plain Text")
                }
                else -> {}
            }
        }
    }

    fun exportCurrentViewAsPdf() {
        viewModelScope.launch(Dispatchers.IO) {
            when (val screen = _currentScreen.value) {
                is Screen.NoteEditor -> {
                    val noteId = screen.noteId
                    if (noteId != null) {
                        val (secName, items) = notesRepo.getExportItemForSingleNote(noteId)
                        val uri = ExportHelper.exportPdf(getApplication(), secName, items)
                        ExportHelper.shareExport(getApplication(), uri, "application/pdf", "Export Note as PDF")
                    }
                }
                is Screen.SectionDetail -> {
                    val (secName, items) = notesRepo.getExportItemsForSection(screen.sectionId, _selectedTagIds.value)
                    val uri = ExportHelper.exportPdf(getApplication(), secName, items)
                    ExportHelper.shareExport(getApplication(), uri, "application/pdf", "Export Section as PDF")
                }
                else -> {}
            }
        }
    }

    // Backup actions
    fun exportBackup() {
        viewModelScope.launch {
            try {
                val uri = backupManager.createBackupZip()
                ExportHelper.shareExport(getApplication(), uri, "application/zip", "Save Fieldnotes Backup (.zip)")
            } catch (e: Exception) {
                showMessage("Backup failed: ${e.localizedMessage}")
            }
        }
    }

    fun onBackupFilePicked(zipUri: Uri) {
        viewModelScope.launch {
            try {
                val parsed = backupManager.parseBackupZip(zipUri)
                _pendingImport.value = parsed
            } catch (e: Exception) {
                showMessage("Failed to read backup: ${e.localizedMessage}")
            }
        }
    }

    fun executeRestoreReplace() {
        val parsed = _pendingImport.value ?: return
        viewModelScope.launch {
            try {
                backupManager.restoreReplace(parsed)
                _pendingImport.value = null
                _showFirstLaunchRestore.value = false
                showMessage("Backup restored successfully!")
                // Reset navigation
                _currentTab.value = BottomTab.NOTES
                _currentScreen.value = Screen.NotesHome
            } catch (e: Exception) {
                showMessage("Restore failed: ${e.localizedMessage}")
            }
        }
    }

    fun startRestoreMerge() {
        val parsed = _pendingImport.value ?: return
        viewModelScope.launch {
            try {
                val conflicts = backupManager.findConflicts(parsed)
                if (conflicts.isEmpty()) {
                    // No conflicts, merge directly
                    backupManager.restoreMerge(parsed, emptyMap())
                    _pendingImport.value = null
                    _showFirstLaunchRestore.value = false
                    showMessage("Backup merged successfully!")
                } else {
                    _importConflicts.value = conflicts
                }
            } catch (e: Exception) {
                showMessage("Merge analysis failed: ${e.localizedMessage}")
            }
        }
    }

    fun finishConflictResolution(resolutions: Map<Int, ConflictResolution>) {
        val parsed = _pendingImport.value ?: return
        viewModelScope.launch {
            try {
                backupManager.restoreMerge(parsed, resolutions)
                _pendingImport.value = null
                _importConflicts.value = emptyList()
                _showFirstLaunchRestore.value = false
                showMessage("Backup merged successfully with conflicts resolved!")
            } catch (e: Exception) {
                showMessage("Merge failed: ${e.localizedMessage}")
            }
        }
    }

    fun cancelImport() {
        _pendingImport.value = null
        _importConflicts.value = emptyList()
    }
}
