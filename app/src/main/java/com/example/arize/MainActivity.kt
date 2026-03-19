package com.example.arize

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.arize.ui.theme.ArizeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArizeTheme {
                ArizeApp()
            }
        }
    }
}

@Composable
fun ArizeApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.GYM) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            imageVector = it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            val modifier = Modifier.padding(innerPadding)
            when (currentDestination) {
                AppDestinations.GYM -> GymLandingPage(modifier)
                AppDestinations.FOOD -> SimplePlaceholder("Food Tracker", modifier)
                AppDestinations.SOCIAL -> SimplePlaceholder("Social Community", modifier)
                AppDestinations.STATS -> SimplePlaceholder("Your Stats", modifier)
                AppDestinations.PROFILE -> SimplePlaceholder("Your Profile", modifier)
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    GYM("Gym", Icons.Default.FitnessCenter),
    FOOD("Food", Icons.Default.Restaurant),
    SOCIAL("Social", Icons.Default.Groups),
    STATS("Stats", Icons.Default.BarChart),
    PROFILE("Profile", Icons.Default.Person),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GymLandingPage(modifier: Modifier = Modifier) {
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState()
    var selectedTimeDisplay by remember { mutableStateOf("") }
    
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val selectedDays = remember { mutableStateListOf<String>() }
    val scrollState = rememberScrollState()

    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTimeDisplay = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Icon
        Surface(
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 24.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            text = "Welcome to Arize",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Let's set up your workout schedule",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
        )

        Text(
            text = "Select Workout Days",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            days.forEach { day ->
                val isSelected = selectedDays.contains(day)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (isSelected) selectedDays.remove(day)
                        else selectedDays.add(day)
                    },
                    label = { Text(day) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Preferred Reminder Time",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedCard(
            onClick = { showTimePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (selectedTimeDisplay.isEmpty()) "Select Time" else selectedTimeDisplay,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (selectedTimeDisplay.isEmpty()) {
                        Text(
                            text = "Tap to choose a time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { 
                // Notification logic placeholder
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium,
            enabled = selectedDays.isNotEmpty() && selectedTimeDisplay.isNotEmpty()
        ) {
            Text("SET MY GOALS", style = MaterialTheme.typography.titleMedium)
        }
        
        if (selectedDays.isNotEmpty() && selectedTimeDisplay.isNotEmpty()) {
            Text(
                text = "We'll notify you on ${selectedDays.joinToString()} at $selectedTimeDisplay",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = "Select Time",
                    style = MaterialTheme.typography.labelMedium
                )
                content()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    dismissButton()
                    confirmButton()
                }
            }
        }
    }
}

@Composable
fun SimplePlaceholder(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
    }
}

@Preview(showBackground = true)
@Composable
fun GymLandingPagePreview() {
    ArizeTheme {
        GymLandingPage()
    }
}
