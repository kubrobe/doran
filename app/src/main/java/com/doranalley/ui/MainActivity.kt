package com.doranalley.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
        vm.onMicPermission(ok)
    }
    val requestMic: () -> Unit = {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) vm.onMicPermission(true)
        else launcher.launch(Manifest.permission.RECORD_AUDIO)
    }
    // The phone's own speech-input screen, used when in-app Korean recognition doesn't work.
    var askKey by rememberSaveable { mutableStateOf<String?>(null) }
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val key = askKey ?: return@rememberLauncherForActivityResult
        askKey = null
        val alts = if (res.resultCode == Activity.RESULT_OK)
            res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).orEmpty().filter { it.isNotBlank() }
        else emptyList()
        vm.onSystemRecognition(key, alts)
    }
    LaunchedEffect(vm.systemAsk) {
        val ask = vm.systemAsk ?: return@LaunchedEffect
        vm.systemAskLaunched()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "한국어로 말해 보세요")
        }
        askKey = ask.key
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) { // ActivityNotFoundException or a broken speech app
            askKey = null
            vm.onSystemRecognition(ask.key, null)
        }
    }
    val strings = remember(vm.lang) { Strings(vm.content.ui, vm.lang) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.notice) {
        val key = vm.notice ?: return@LaunchedEffect
        vm.notice = null
        snackbar.showSnackbar(strings[key], withDismissAction = true, duration = SnackbarDuration.Long)
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
