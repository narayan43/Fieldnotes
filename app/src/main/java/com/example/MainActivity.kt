package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.goals.GoalDetailScreen
import com.example.ui.screens.goals.GoalsScreen
import com.example.ui.screens.notes.NoteEditorScreen
import com.example.ui.screens.notes.SectionDetailScreen
import com.example.ui.screens.notes.SectionsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.FieldnotesTheme
import com.example.ui.viewmodels.BottomTab
import com.example.ui.viewmodels.MainViewModel
import com.example.ui.viewmodels.Screen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FieldnotesTheme {
                val viewModel: MainViewModel = viewModel()
                FieldnotesAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun FieldnotesAppRoot(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val showFirstLaunchRestore by viewModel.showFirstLaunchRestore.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Backup picker for first launch restore dialog
    val firstLaunchZipPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onBackupFilePicked(uri)
            viewModel.switchTab(BottomTab.SETTINGS)
        }
    }

    // Hide bottom bar in Note Editor for distraction-free focus
    val showBottomBar = currentScreen !is Screen.NoteEditor

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentTab == BottomTab.NOTES,
                        onClick = { viewModel.switchTab(BottomTab.NOTES) },
                        icon = { Icon(Icons.Default.Folder, contentDescription = "Notes") },
                        label = { Text("Notes") },
                        modifier = Modifier.testTag("nav_tab_notes")
                    )
                    NavigationBarItem(
                        selected = currentTab == BottomTab.GOALS,
                        onClick = { viewModel.switchTab(BottomTab.GOALS) },
                        icon = { Icon(Icons.Default.Timer, contentDescription = "Goals") },
                        label = { Text("Goals") },
                        modifier = Modifier.testTag("nav_tab_goals")
                    )
                    NavigationBarItem(
                        selected = currentTab == BottomTab.SETTINGS,
                        onClick = { viewModel.switchTab(BottomTab.SETTINGS) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is Screen.NotesHome -> SectionsScreen(viewModel = viewModel)
                is Screen.SectionDetail -> SectionDetailScreen(sectionId = screen.sectionId, viewModel = viewModel)
                is Screen.NoteEditor -> NoteEditorScreen(sectionId = screen.sectionId, noteId = screen.noteId, viewModel = viewModel)
                is Screen.GoalsHome -> GoalsScreen(viewModel = viewModel)
                is Screen.GoalDetail -> GoalDetailScreen(goalId = screen.goalId, viewModel = viewModel)
                is Screen.Settings -> SettingsScreen(viewModel = viewModel)
            }
        }
    }

    // First Launch / Empty Database Restore Dialog
    if (showFirstLaunchRestore) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissFirstLaunchPrompt() },
            icon = { Icon(Icons.Default.Archive, contentDescription = null) },
            title = { Text("Restore From Backup?") },
            text = {
                Text("Fieldnotes is completely offline. If you have an existing backup (.zip), you can restore your sections, notes, images, and goal time log now, or start fresh.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissFirstLaunchPrompt()
                        firstLaunchZipPicker.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                    },
                    modifier = Modifier.testTag("first_launch_import_button")
                ) {
                    Text("Import Backup")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissFirstLaunchPrompt() },
                    modifier = Modifier.testTag("first_launch_start_fresh_button")
                ) {
                    Text("Start Fresh")
                }
            }
        )
    }
}
