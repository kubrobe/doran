package com.doranalley.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.doranalley.ui.screens.DiaryScreen
import com.doranalley.ui.screens.HomeScreen
import com.doranalley.ui.screens.MirrorScreen
import com.doranalley.ui.screens.PlayScreen
import com.doranalley.ui.screens.QuestScreen
import com.doranalley.ui.screens.RecallScreen
import com.doranalley.ui.screens.SettingsScreen
import com.doranalley.ui.theme.DoranTheme

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DoranTheme { DoranApp(vm) } }
    }

    override fun onResume() {
        super.onResume()
        vm.micGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (vm.micGranted) vm.micDenied = false
        vm.onResume()
    }

    override fun onPause() {
        vm.onPause()
        super.onPause()
    }
}

@Composable
fun DoranApp(vm: AppViewModel) {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        vm.micGranted = ok
        vm.micDenied = !ok
        if (!ok) vm.notice = "notice_mic_denied"
    }
    val requestMic: () -> Unit = {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) vm.micGranted = true
        else launcher.launch(Manifest.permission.RECORD_AUDIO)
    }
    val strings = remember(vm.lang) { Strings(vm.content.ui, vm.lang) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.notice) {
        val key = vm.notice ?: return@LaunchedEffect
        vm.notice = null
        snackbar.showSnackbar(strings[key])
    }
    CompositionLocalProvider(LocalStrings provides strings) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                BackHandler(enabled = vm.screen != Screen.Home) { vm.back() }
                when (val sc = vm.screen) {
                    Screen.Home -> HomeScreen(vm)
                    is Screen.Play -> PlayScreen(vm, requestMic)
                    is Screen.Mirror -> MirrorScreen(vm, sc.episodeId, requestMic)
                    Screen.Recall -> RecallScreen(vm, requestMic)
                    Screen.Quest -> QuestScreen(vm, requestMic)
                    Screen.Diary -> DiaryScreen(vm, requestMic)
                    Screen.Settings -> SettingsScreen(vm, requestMic)
                }
            }
        }
    }
}
