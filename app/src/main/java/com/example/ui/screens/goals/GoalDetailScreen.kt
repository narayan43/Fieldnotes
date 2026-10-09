package com.example.ui.screens.goals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.SubGoalWithStats
import com.example.ui.viewmodels.MainViewModel
import com.example.util.TimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalId: Long,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val scope = rememberCoroutineScope()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val goal = goals.firstOrNull { it.id == goalId }
    val subGoals by viewModel.goalsRepo.getSubGoals(goalId).collectAsStateWithLifecycle(initialValue = emptyList())
    val runningTimer by viewModel.runningTimer.collectAsStateWithLifecycle()
    val dashboardStats by viewModel.dashboardStats.collectAsStateWithLifecycle()

    var showCreateSubGoalDialog by remember { mutableStateOf(false) }
    var showDeleteGoalDialog by remember { mutableStateOf(false) }
    var subGoalToDelete by remember { mutableStateOf<SubGoalWithStats?>(null) }

    val isGoalTimerRunning = runningTimer?.goalId == goalId && runningTimer?.subGoalId == null

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = goal?.name ?: "Goal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "Total logged: ${TimeFormatter.formatHumanDuration(goal?.totalTimeMs ?: 0L)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("goal_detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to goals")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDeleteGoalDialog = true },
                        modifier = Modifier.testTag("delete_goal_button")
                    ) {
                        Icon(Icons.Default.Delete, tint = MaterialTheme.colorScheme.error, contentDescription = "Delete goal")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSubGoalDialog = true },
                modifier = Modifier.testTag("create_subgoal_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add sub-goal")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Goal Timer Card (Start timer on the goal directly without sub-goal)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isGoalTimerRunning) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "General Goal Time",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Log time for ${goal?.name ?: "this goal"} without choosing a sub-goal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isGoalTimerRunning) {
                            Button(
                                onClick = { viewModel.stopGoalTimer() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.testTag("stop_main_goal_timer")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.startGoalTimer(goalId, null) },
                                modifier = Modifier.testTag("start_main_goal_timer")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Start")
                            }
                        }
                    }
                }
            }

            // Sub-goals header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Sub-Goals (${subGoals.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showCreateSubGoalDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Sub-Goal")
                    }
                }
            }

            // Sub-goals list
            if (subGoals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No sub-goals yet. Break down this goal (e.g. \"Learning hook\", \"Payoff\", \"Structure\") to log focused time on each aspect.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(subGoals, key = { it.id }) { subGoal ->
                    val isSubGoalRunning = runningTimer?.goalId == goalId && runningTimer?.subGoalId == subGoal.id
                    val todayTime = dashboardStats.subGoalTimesToday[subGoal.id] ?: 0L
                    val weekTime = dashboardStats.subGoalTimesWeek[subGoal.id] ?: 0L

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("subgoal_card_${subGoal.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSubGoalRunning) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = subGoal.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Total: ${TimeFormatter.formatHumanDuration(subGoal.totalTimeMs)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Today: ${TimeFormatter.formatHumanDuration(todayTime)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSubGoalRunning) {
                                        Button(
                                            onClick = { viewModel.stopGoalTimer() },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.testTag("stop_subgoal_${subGoal.id}")
                                        ) {
                                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Stop")
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { viewModel.startGoalTimer(goalId, subGoal.id) },
                                            modifier = Modifier.testTag("start_subgoal_${subGoal.id}")
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Timer")
                                        }
                                    }

                                    IconButton(onClick = { subGoalToDelete = subGoal }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            tint = MaterialTheme.colorScheme.outline,
                                            contentDescription = "Delete sub-goal",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Create SubGoal Dialog
    if (showCreateSubGoalDialog) {
        var subGoalName by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showCreateSubGoalDialog = false },
            title = { Text("New Sub-Goal") },
            text = {
                OutlinedTextField(
                    value = subGoalName,
                    onValueChange = {
                        subGoalName = it
                        if (error != null) error = null
                    },
                    label = { Text("Sub-Goal Name") },
                    placeholder = { Text("e.g. Learning hook, Payoff") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subgoal_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val res = viewModel.goalsRepo.createSubGoal(goalId, subGoalName)
                            if (res.isSuccess) {
                                showCreateSubGoalDialog = false
                            } else {
                                error = res.exceptionOrNull()?.message
                            }
                        }
                    },
                    modifier = Modifier.testTag("save_subgoal_button")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateSubGoalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Goal Confirmation Dialog
    if (showDeleteGoalDialog && goal != null) {
        AlertDialog(
            onDismissRequest = { showDeleteGoalDialog = false },
            title = { Text("Delete Goal?") },
            text = {
                Text("Deleting \"${goal.name}\" will also remove all its sub-goals and logged sessions.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteGoalDialog = false
                        scope.launch {
                            viewModel.goalsRepo.deleteGoal(goalId)
                            viewModel.navigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGoalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete SubGoal Confirmation Dialog
    if (subGoalToDelete != null) {
        AlertDialog(
            onDismissRequest = { subGoalToDelete = null },
            title = { Text("Delete Sub-Goal?") },
            text = {
                Text("Delete \"${subGoalToDelete!!.name}\" and its logged sessions?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = subGoalToDelete!!
                        subGoalToDelete = null
                        scope.launch {
                            viewModel.goalsRepo.deleteSubGoal(toDelete.id)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { subGoalToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
