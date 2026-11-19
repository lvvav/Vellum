package com.vellum.app

import android.graphics.Color as AColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vellum.app.ui.AuroraBackground
import com.vellum.app.ui.GlassNav
import com.vellum.app.ui.HomeScreen
import com.vellum.app.ui.ServersScreen
import com.vellum.app.ui.SettingsScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT)
        )
        setContent { App() }
    }
}

@Composable
fun App() {
    val state = remember { AppState() }
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground(connected = state.conn == Conn.Connected, animated = state.smoothMotion)

        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 30 }) togetherWith fadeOut(tween(140))
            },
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            label = "tab"
        ) { t ->
            when (t) {
                0 -> HomeScreen(state) { scope.launch { state.toggle() } }
                1 -> ServersScreen(state) { scope.launch { state.refresh() } }
                else -> SettingsScreen(state)
            }
        }

        GlassNav(
            selected = tab, onSelect = { tab = it },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 14.dp)
        )
    }
}
