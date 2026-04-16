package com.example.arize

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.arize.ui.theme.ArizeTheme

@Composable
fun ArizeApp(
    initialOnboardingDone: Boolean,
    initialProfile: UserProfile,
    onOnboardingFinished: (UserProfile) -> Unit,
    onProfileUpdated: (UserProfile) -> Unit,
) {
    var onboardingDone by rememberSaveable { mutableStateOf(initialOnboardingDone) }
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.GYM) }
    var profile by remember { mutableStateOf(initialProfile) }
    val touchedBodyParts = remember { mutableStateListOf<String>() }

    if (!onboardingDone) {
        OnboardingLandingPage(
            initialProfile = profile,
            onComplete = {
                profile = it
                onboardingDone = true
                onOnboardingFinished(it)
            }
        )
        return
    }

    val screenColor = when (currentDestination) {
        AppDestinations.FOOD, AppDestinations.PROFILE -> LightSurface
        else -> AppDark
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = screenColor,
        bottomBar = {
            ArizeBottomBar(
                currentDestination = currentDestination,
                onDestinationSelected = { currentDestination = it }
            )
        }
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (currentDestination) {
            AppDestinations.GYM -> GymPage(
                profile = profile,
                modifier = modifier,
                onExerciseTouched = { part ->
                    if (!touchedBodyParts.contains(part)) {
                        touchedBodyParts.add(part)
                    }
                }
            )
            AppDestinations.FOOD -> FoodPage(profile = profile, modifier = modifier)
            AppDestinations.SOCIAL -> SocialPage(modifier = modifier)
            AppDestinations.STATS -> StatsPage(modifier = modifier, touchedBodyParts = touchedBodyParts, profile = profile)
            AppDestinations.PROFILE -> ProfilePage(
                profile = profile,
                modifier = modifier,
                onSave = {
                    profile = it
                    onProfileUpdated(it)
                }
            )
        }
    }
}

@Composable
fun ArizeBottomBar(
    currentDestination: AppDestinations,
    onDestinationSelected: (AppDestinations) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF111623),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppDestinations.entries.forEach { destination ->
                val selected = destination == currentDestination
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(role = Role.Tab, onClick = { onDestinationSelected(destination) })
                        .semantics { contentDescription = destination.label }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = if (selected) AccentBlue else Color.Transparent,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = destination.icon,
                                contentDescription = null,
                                tint = if (selected) Color.White else MutedText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = destination.label,
                        color = if (selected) Color.White else MutedText,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    ArizeTheme {
        ArizeApp(
            initialOnboardingDone = false,
            initialProfile = UserProfile(),
            onOnboardingFinished = {},
            onProfileUpdated = {}
        )
    }
}
