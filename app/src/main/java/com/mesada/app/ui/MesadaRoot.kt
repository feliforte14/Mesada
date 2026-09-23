package com.mesada.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.mesada.app.ui.screens.KitchenScreen
import com.mesada.app.ui.screens.OnboardingScreen
import com.mesada.app.ui.screens.TodayScreen

private enum class Screen(val label: String, val icon: ImageVector) {
    TODAY("Hoy", Icons.Filled.Today),
    ADD("Agregar", Icons.Filled.Add),
    KITCHEN("Cocina", Icons.Filled.Restaurant),
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
    val timer by vm.timer.state.collectAsStateWithLifecycle()
    val ui by vm.assistant.collectAsStateWithLifecycle()
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

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(Modifier.fillMaxSize()) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.fillMaxHeight(),
                header = {
                    Text("Mesada", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 20.dp))
                },
            ) {
                Screen.entries.forEach { s ->
                    NavigationRailItem(
                        selected = screen == s, onClick = { screen = s },
                        icon = { Icon(s.icon, contentDescription = null, modifier = Modifier.size(30.dp)) },
                        label = { Text(s.label) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSecondary,
                            indicatorColor = MaterialTheme.colorScheme.secondary,
                            unselectedIconColor = MaterialTheme.colorScheme.inverseOnSurface,
                            unselectedTextColor = MaterialTheme.colorScheme.inverseOnSurface,
                            selectedTextColor = MaterialTheme.colorScheme.secondary,
                        ),
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                when (screen) {
                    Screen.TODAY -> TodayScreen(
                        day = day, onRemove = vm::remove,
                        onAddTo = { vm.selectedMeal = it; screen = Screen.ADD },
                        onSteps = vm::changeSteps, onReset = vm::clearDay,
                    )
                    Screen.ADD -> AddScreen(vm.selectedMeal, { vm.selectedMeal = it }) { food, qty -> vm.addFood(food, qty) }
                    Screen.KITCHEN -> KitchenScreen(
                        day = day, timer = timer,
                        onTimerPreset = { vm.timer.set(it) }, onTimerToggle = vm.timer::toggle,
                        onTimerReset = vm.timer::reset, onAddIdea = vm::addIdea, onGoal = vm::changeGoal,
                        onEditProfile = vm::editProfile,
                    )
                }
            }
        }

        if (!ui.open) FloatingActionButton(
            onClick = onMic, shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(28.dp).size(92.dp),
        ) { Icon(Icons.Filled.Mic, contentDescription = "Hablar con el asistente", modifier = Modifier.size(40.dp)) }

        AnimatedVisibility(
            visible = ui.open,
            enter = slideInHorizontally { it }, exit = slideOutHorizontally { it },
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            AssistantPanel(ui, onMic = onMic, onSend = vm::send, onClose = vm::closeAssistant, onToggleSpeak = vm::toggleSpeak)
        }
    }
}
