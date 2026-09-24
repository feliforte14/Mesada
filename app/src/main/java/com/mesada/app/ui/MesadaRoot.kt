package com.mesada.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mesada.app.MesadaViewModel
import com.mesada.app.ProfileLoadState
import com.mesada.app.VoiceMode
import com.mesada.app.ui.screens.AddScreen
import com.mesada.app.ui.screens.ExerciseScreen
import com.mesada.app.ui.screens.HistoryScreen
import com.mesada.app.ui.screens.KitchenScreen
import com.mesada.app.ui.screens.OnboardingScreen
import com.mesada.app.ui.screens.ProfileScreen
import com.mesada.app.ui.screens.RewardsScreen
import com.mesada.app.ui.screens.TodayScreen

private enum class Screen(val label: String, val icon: ImageVector) {
    TODAY("Hoy", Icons.Filled.Today),
    ADD("Agregar", Icons.Filled.Add),
    KITCHEN("Cocina", Icons.Filled.Restaurant),
    EXERCISE("Ejercicio", Icons.Filled.DirectionsRun),
    HISTORY("Evolución", Icons.Filled.ShowChart),
    REWARDS("Logros", Icons.Filled.EmojiEvents),
    PROFILE("Perfil", Icons.Filled.Person),
}

@Composable
fun MesadaRoot(vm: MesadaViewModel) {
    val profileState by vm.profileState.collectAsStateWithLifecycle()

    when (val state = profileState) {
        is ProfileLoadState.Loading -> Unit // sin flash: esperamos a saber si hay perfil o no
        is ProfileLoadState.Loaded -> {
            if (state.profile == null || vm.editingProfile) {
                OnboardingScreen(
                    initial = state.profile,
                    onCancel = if (state.profile != null) vm::cancelEditProfile else null,
                    onSave = vm::saveProfile,
                )
            } else {
                MesadaMain(vm)
            }
        }
    }
}

@Composable
private fun MesadaMain(vm: MesadaViewModel) {
    val day by vm.day.collectAsStateWithLifecycle()
    val foods by vm.foods.collectAsStateWithLifecycle()
    val recipes by vm.recipes.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val profileState by vm.profileState.collectAsStateWithLifecycle()
    val profile = (profileState as? ProfileLoadState.Loaded)?.profile
    val timer by vm.timer.state.collectAsStateWithLifecycle()
    val ui by vm.assistant.collectAsStateWithLifecycle()
    val scaleEnabled by vm.scaleEnabled.collectAsStateWithLifecycle()
    val scaleConnection by vm.scaleConnection.collectAsStateWithLifecycle()
    val scaleGrams by vm.scaleGrams.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(Screen.TODAY) }

    val context = LocalContext.current
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.onMicTapped() else vm.micPermissionDenied()
    }
    val onMic = {
        vm.openAssistant()
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted || ui.mode != VoiceMode.IDLE) vm.onMicTapped() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    val blePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    val scalePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results.values.all { it }) vm.setScaleEnabled(true)
    }
    val onScaleToggle = { enabled: Boolean ->
        if (!enabled) {
            vm.setScaleEnabled(false)
        } else if (blePermissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            vm.setScaleEnabled(true)
        } else {
            scalePermission.launch(blePermissions)
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Column(Modifier.fillMaxSize()) {
            TopBar(current = screen, onSelect = { screen = it }, onMic = onMic)
            Box(Modifier.fillMaxWidth().weight(1f)) {
                Box(Modifier.widthIn(max = 1200.dp).fillMaxSize().align(Alignment.TopCenter)) {
                    when (screen) {
                        Screen.TODAY -> TodayScreen(
                            day = day, onRemove = vm::remove,
                            onAddTo = { vm.selectedMeal = it; screen = Screen.ADD },
                            onReset = vm::clearDay,
                        )
                        Screen.ADD -> AddScreen(
                            foods = foods, selectedMeal = vm.selectedMeal, onSelectMeal = { vm.selectedMeal = it },
                            onAdd = { food, qty -> vm.addFood(food, qty) },
                            onCreateFood = vm::createFood, onUpdateFood = vm::updateFood, onDeleteFood = vm::deleteFood,
                        )
                        Screen.KITCHEN -> KitchenScreen(
                            day = day, timer = timer, recipes = recipes, foods = foods,
                            onTimerPreset = { vm.timer.set(it) }, onTimerToggle = vm.timer::toggle,
                            onTimerReset = vm.timer::reset,
                            onAddRecipe = vm::addRecipe, onCreateRecipe = vm::createRecipe, onDeleteRecipe = vm::deleteRecipe,
                            onGoal = vm::changeGoal, onEditProfile = vm::editProfile,
                            scaleEnabled = scaleEnabled, scaleConnection = scaleConnection, scaleGrams = scaleGrams,
                            onScaleToggle = onScaleToggle,
                        )
                        Screen.EXERCISE -> ExerciseScreen(day = day, onSteps = vm::changeSteps)
                        Screen.HISTORY -> HistoryScreen(history)
                        Screen.REWARDS -> RewardsScreen(day = day, history = history)
                        Screen.PROFILE -> ProfileScreen(profile = profile, goals = day.goals, onEdit = vm::editProfile)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = ui.open,
            enter = slideInHorizontally { it }, exit = slideOutHorizontally { it },
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            AssistantPanel(ui, onMic = onMic, onSend = vm::send, onClose = vm::closeAssistant, onToggleSpeak = vm::toggleSpeak)
        }
    }
}

@Composable
private fun TopBar(current: Screen, onSelect: (Screen) -> Unit, onMic: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.inverseSurface, contentColor = MaterialTheme.colorScheme.inverseOnSurface) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("N", color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Text("Nomi", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                Screen.entries.forEach { s -> TopTab(s, current == s) { onSelect(s) } }
            }
            Spacer(Modifier.weight(1f))
            FloatingActionButton(
                onClick = onMic, shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.size(56.dp),
            ) { Icon(Icons.Filled.Mic, contentDescription = "Hablar con el asistente", modifier = Modifier.size(28.dp)) }
        }
    }
}

@Composable
private fun TopTab(screen: Screen, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(screen.icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Text(screen.label, style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}
