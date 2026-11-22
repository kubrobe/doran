package com.doranalley.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.SystemBarStyle
import com.doranalley.ui.components.DoranBackdrop
import com.doranalley.ui.components.Motion
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import com.doranalley.ui.theme.isNight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.doranalley.ui.components.enterRise
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.doranalley.audio.AudioPlayer
import com.doranalley.ui.screens.CompleteScreen
import com.doranalley.ui.screens.GuidebookScreen
import com.doranalley.ui.screens.HomeScreen
import com.doranalley.ui.screens.LessonScreen
import com.doranalley.ui.screens.OnboardingScreen
import com.doranalley.ui.screens.OutOfHeartsScreen
import com.doranalley.ui.screens.PlacedScreen
import com.doranalley.ui.screens.SettingsScreen
import com.doranalley.ui.screens.StreakScreen
import com.doranalley.ui.theme.DoranTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    val vm: AppViewModel by viewModels()
    private var lastToast = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            vm.audio.problems.collect { p ->
                val now = System.currentTimeMillis()
                if (now - lastToast < 15_000) return@collect
                lastToast = now
                val msg = when (p) {
                    AudioPlayer.Problem.MUTED -> "Your media volume is off. Turn it up to hear the Korean audio."
                    AudioPlayer.Problem.MISSING -> "This audio clip is missing."
                    AudioPlayer.Problem.FAILED -> "Audio could not play. Try again."
                }
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
            }
        }
        setContent {
            val progress by vm.progress.collectAsState()
            val mode = progress.settings.theme
            DoranTheme(mode, reduceMotion = progress.settings.reduceMotion) {
                val night = isNight(mode)
                // Light status-bar icons at night, dark ones by day.
                LaunchedEffect(night) {
                    val transparent = android.graphics.Color.TRANSPARENT
                    val style = if (night) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                }
                val screen by vm.screen.collectAsState()
                val warm = when (screen) { is Screen.Complete, is Screen.Streak, is Screen.Placed -> 1f; else -> 0f }
                // The first screen after launch shows at once; later screens fade and rise in briefly.
                val firstScreen = remember { arrayOf<Any?>(screen::class) }
                Box(Modifier.fillMaxSize()) {
                    DoranBackdrop(Modifier.fillMaxSize(), warm = warm)
                    key(screen::class) {
                        val entrance = remember {
                            if (firstScreen[0] == screen::class) Modifier
                            else Modifier.enterRise(distance = if (screen == Screen.Lesson) 24.dp else 8.dp, durationMs = Motion.SCREEN)
                        }
                        SideEffect { firstScreen[0] = null }
                        Box(Modifier.fillMaxSize().then(entrance)) {
                            when (val s = screen) {
                                is Screen.Onboarding -> OnboardingScreen(vm, s.step)
                                is Screen.Home -> {
                                    BackHandler(enabled = s.tab != Tab.LEARN) { vm.back() }
                                    HomeScreen(vm, s.tab)
                                }
                                Screen.Lesson -> LessonScreen(vm)
                                is Screen.Complete -> CompleteScreen(vm, s.summary)
                                is Screen.Streak -> StreakScreen(vm, s.days)
                                Screen.OutOfHearts -> OutOfHeartsScreen(vm)
                                is Screen.Placed -> PlacedScreen(vm, s.unit)
                                is Screen.Guidebook -> GuidebookScreen(vm, s.unit)
                                Screen.Settings -> SettingsScreen(vm)
                            }
                        }
                    }
                }
                // If a lesson screen is shown without a session (should not happen), go home.
                LaunchedEffect(screen) {
                    if (screen == Screen.Lesson && vm.session.value == null) vm.go(Screen.Home())
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        vm.audio.stop()
        vm.stopSpeaking()
    }
}
