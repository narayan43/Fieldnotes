package com.example.ui.screens.notes

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.viewmodels.MainViewModel
import com.example.util.FileUtils
import com.example.util.TimeFormatter
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    sectionId: Long,
    noteId: Long?,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.cancelEditor()
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val tags by viewModel.notesRepo.getTags(sectionId).collectAsStateWithLifecycle(initialValue = emptyList())

    var showTagDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Photo picker launcher (zero-permission Android Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val relativePath = FileUtils.saveUriToPrivateStorage(context, uri)
            if (relativePath != null) {
                viewModel.addEditorImage(relativePath)
            } else {
                viewModel.showMessage("Failed to import image")
            }
        }
    }

    val selectedTag = tags.firstOrNull { it.id == editorState.selectedTagId }
    val accumulatedTimeMs = editorState.baseWritingTimeMs + editorState.currentSessionElapsedMs

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (noteId == null) "New Note" else "Edit Note",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.cancelEditor() },
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close editor")
                    }
                },
                actions = {
                    if (noteId != null) {
                        IconButton(
                            onClick = { showExportDialog = true },
                            modifier = Modifier.testTag("export_note_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Export note")
                        }
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.testTag("delete_note_button")
                        ) {
                            Icon(Icons.Default.Delete, tint = MaterialTheme.colorScheme.error, contentDescription = "Delete note")
                        }
                    }
                    Button(
                        onClick = { viewModel.saveNoteAndExit { } },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_note_button")
                    ) {
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live writing time banner
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Writing time on this note: ${TimeFormatter.formatHumanDuration(accumulatedTimeMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Tag selector: Single tag required
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tag_selector_card")
                        .clickable { showTagDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedTag != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Label,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            if (selectedTag != null) {
                                Text(
                                    text = "Tag: ${selectedTag.name}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Text(
                                    text = "Select or create tag *",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Title Field (Required)
            item {
                OutlinedTextField(
                    value = editorState.title,
                    onValueChange = { viewModel.updateEditorTitle(it) },
                    label = { Text("Title *") },
                    placeholder = { Text("Note title...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Body Text Field (Multiline)
            item {
                OutlinedTextField(
                    value = editorState.body,
                    onValueChange = { viewModel.updateEditorBody(it) },
                    label = { Text("Body") },
                    placeholder = { Text("Write your thoughts, observations, or draft here...") },
                    minLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_body_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Image controls header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Visual Reference (${editorState.images.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("add_gallery_image_button")
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Image")
                    }
                }
            }

            // Attached Images List
            if (editorState.images.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No images attached. Add visual references from your gallery.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                itemsIndexed(editorState.images, key = { index, path -> "$path-$index" }) { index, relPath ->
                    val imageFile = FileUtils.getFileFromRelativePath(context, relPath)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column {
                            AsyncImage(
                                model = imageFile,
                                contentDescription = "Attached note image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (index > 0) {
                                        IconButton(onClick = { viewModel.moveEditorImage(index, index - 1) }) {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move image up", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    if (index < editorState.images.size - 1) {
                                        IconButton(onClick = { viewModel.moveEditorImage(index, index + 1) }) {
                                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move image down", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.removeEditorImage(index) },
                                    modifier = Modifier.testTag("remove_image_$index")
                                ) {
                                    Icon(Icons.Default.Delete, tint = MaterialTheme.colorScheme.error, contentDescription = "Remove image")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Tag Selection & Creation Dialog
    if (showTagDialog) {
        TagSelectorDialog(
            sectionId = sectionId,
            tags = tags,
            selectedTagId = editorState.selectedTagId,
            onSelectTag = { tagId ->
                viewModel.updateEditorTag(tagId)
                showTagDialog = false
            },
            onCreateTag = { name ->
                scope.launch {
                    val result = viewModel.notesRepo.createTag(sectionId, name)
                    if (result.isSuccess) {
                        viewModel.updateEditorTag(result.getOrThrow())
                        showTagDialog = false
                    } else {
                        viewModel.showMessage(result.exceptionOrNull()?.message ?: "Failed to create tag")
                    }
                }
            },
            onDismiss = { showTagDialog = false }
        )
    }

    // Single Note Export Dialog
    if (showExportDialog && noteId != null) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Note") },
            text = {
                Text("Export \"${editorState.title}\" with images and writing time.")
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showExportDialog = false
                            viewModel.exportCurrentViewAsText()
                        }
                    ) {
                        Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Plain Text")
                    }
                    Button(
                        onClick = {
                            showExportDialog = false
                            viewModel.exportCurrentViewAsPdf()
                        }
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Note Confirmation Dialog
    if (showDeleteConfirmDialog && noteId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Note?") },
            text = {
                Text("Deleting \"${editorState.title}\" will remove its text, writing sessions, and delete attached image files.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        scope.launch {
                            viewModel.notesRepo.deleteNote(noteId)
                            viewModel.cancelEditor()
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TagSelectorDialog(
    sectionId: Long,
    tags: List<com.example.data.entity.TagWithCount>,
    selectedTagId: Long?,
    onSelectTag: (Long) -> Unit,
    onCreateTag: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredTags = remember(searchQuery, tags) {
        if (searchQuery.isBlank()) tags
        else tags.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }
    val exactMatchExists = tags.any { it.name.equals(searchQuery.trim(), ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Tag") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "A note has exactly one tag in this section.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search or create tag...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tag_search_input")
                )
                Spacer(modifier = Modifier.height(12.dp))

                // If searched tag does not exist, offer to create it
                if (searchQuery.isNotBlank() && !exactMatchExists) {
                    Button(
                        onClick = { onCreateTag(searchQuery.trim()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_searched_tag_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create tag \"${searchQuery.trim()}\"")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(filteredTags.size) { index ->
                        val tag = filteredTags[index]
                        val isSelected = tag.id == selectedTagId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else androidx.compose.ui.graphics.Color.Transparent
                                )
                                .clickable { onSelectTag(tag.id) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${tag.noteCount} note${if (tag.noteCount == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
