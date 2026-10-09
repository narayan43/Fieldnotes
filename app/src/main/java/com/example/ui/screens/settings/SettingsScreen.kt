package com.example.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.backup.ConflictResolution
import com.example.ui.viewmodels.MainViewModel
import com.example.util.TimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val pendingImport by viewModel.pendingImport.collectAsStateWithLifecycle()
    val importConflicts by viewModel.importConflicts.collectAsStateWithLifecycle()

    // File picker launcher for .zip
    val importZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onBackupFilePicked(uri)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings & Backup",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Backup & Restore Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Full Backup & Restore",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Backup packages your entire app into a single .zip file including all sections, notes, tags, writing session logs, goals, sub-goals, and every attached image. Images are stored inside the zip, and uninstalling removes app-private storage unless you exported a backup first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.exportBackup() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_backup_button")
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export Backup")
                            }

                            OutlinedButton(
                                onClick = {
                                    importZipLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("import_backup_button")
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import Backup")
                            }
                        }
                    }
                }
            }

            // Privacy & Offline Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Offline-First & Private",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "• No accounts, trackers, or cloud dependencies.\n• Images are copied exclusively into private internal sandbox storage.\n• No public folders or cluttered gallery albums are created.\n• Goal time log and note writing time remain completely separate.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // App Identity & Info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "About Fieldnotes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Version 1.0 • Built for focused writers, researchers, and thinkers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Import Choice Dialog: Replace vs Merge
    if (pendingImport != null && importConflicts.isEmpty()) {
        val parsed = pendingImport!!
        var selectedChoice by remember { mutableStateOf("replace") }

        AlertDialog(
            onDismissRequest = { viewModel.cancelImport() },
            title = { Text("Import Backup Archive") },
            text = {
                Column {
                    Text(
                        "Found backup created on ${TimeFormatter.formatDateTime(parsed.manifest.exportedAt)} with ${parsed.manifest.sectionsCount} sections, ${parsed.manifest.notesCount} notes, ${parsed.manifest.noteImagesCount} images, and ${parsed.manifest.goalsCount} goals.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Select how to import data:", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedChoice = "replace" }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedChoice == "replace",
                            onClick = { selectedChoice = "replace" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Replace all data", fontWeight = FontWeight.Medium)
                            Text(
                                "Deletes current data and restores the backup snapshot completely.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedChoice = "merge" }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedChoice == "merge",
                            onClick = { selectedChoice = "merge" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Merge with current data", fontWeight = FontWeight.Medium)
                            Text(
                                "Keeps current data and merges imported sections, tags, and notes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedChoice == "replace") {
                            viewModel.executeRestoreReplace()
                        } else {
                            viewModel.startRestoreMerge()
                        }
                    },
                    modifier = Modifier.testTag("confirm_import_choice_button")
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelImport() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Merge Conflict Resolution Dialog
    if (importConflicts.isNotEmpty()) {
        MergeConflictResolutionDialog(
            conflicts = importConflicts,
            onResolve = { resolutions ->
                viewModel.finishConflictResolution(resolutions)
            },
            onCancel = { viewModel.cancelImport() }
        )
    }
}

@Composable
fun MergeConflictResolutionDialog(
    conflicts: List<com.example.backup.NoteConflict>,
    onResolve: (Map<Int, ConflictResolution>) -> Unit,
    onCancel: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var applyToAll by remember { mutableStateOf(false) }
    val resolutions = remember { mutableStateMapOf<Int, ConflictResolution>() }

    val currentConflict = conflicts.getOrNull(currentIndex)

    if (currentConflict != null) {
        AlertDialog(
            onDismissRequest = onCancel,
            title = { Text("Duplicate Note Title") },
            text = {
                Column {
                    Text(
                        "An existing note with the same title was found in the same section:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Title: \"${currentConflict.title}\"",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Section: ${currentConflict.sectionName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Conflict ${currentIndex + 1} of ${conflicts.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { applyToAll = !applyToAll }
                    ) {
                        Checkbox(
                            checked = applyToAll,
                            onCheckedChange = { applyToAll = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply choice to all remaining conflicts", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val choice = ConflictResolution.KEEP_BOTH
                        if (applyToAll) {
                            for (i in currentIndex until conflicts.size) {
                                resolutions[conflicts[i].importedNoteIndex] = choice
                            }
                            onResolve(resolutions)
                        } else {
                            resolutions[currentConflict.importedNoteIndex] = choice
                            if (currentIndex + 1 < conflicts.size) {
                                currentIndex++
                            } else {
                                onResolve(resolutions)
                            }
                        }
                    },
                    modifier = Modifier.testTag("conflict_keep_both_button")
                ) {
                    Text("Keep Both")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        val choice = ConflictResolution.SKIP
                        if (applyToAll) {
                            for (i in currentIndex until conflicts.size) {
                                resolutions[conflicts[i].importedNoteIndex] = choice
                            }
                            onResolve(resolutions)
                        } else {
                            resolutions[currentConflict.importedNoteIndex] = choice
                            if (currentIndex + 1 < conflicts.size) {
                                currentIndex++
                            } else {
                                onResolve(resolutions)
                            }
                        }
                    },
                    modifier = Modifier.testTag("conflict_skip_button")
                ) {
                    Text("Skip")
                }
            }
        )
    }
}
