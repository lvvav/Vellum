package com.vellum.app

import android.app.Activity
import android.graphics.Color as AColor
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
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
    val ctx = LocalContext.current
    val state = remember { AppState(ctx) }
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }

    // Системный запрос разрешения на VPN: при первом подключении Android показывает его сам
    val askVpn = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            Vpn.start(ctx, state.current())
        } else {
            Toast.makeText(ctx, "Без разрешения VPN не подключиться", Toast.LENGTH_SHORT).show()
        }
    }
    val onToggle: () -> Unit = {
        if (VpnControl.conn == Conn.Idle) {
            val prep = VpnService.prepare(ctx)
            if (prep == null) Vpn.start(ctx, state.current()) else askVpn.launch(prep)
        } else {
            Vpn.stop(ctx)
        }
    }

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
                0 -> HomeScreen(state, onToggle)
                1 -> ServersScreen(state) {
                    scope.launch { Toast.makeText(ctx, state.refresh(), Toast.LENGTH_SHORT).show() }
                }
                else -> SettingsScreen(state)
            }
        }

        GlassNav(
            selected = tab, onSelect = { tab = it },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 14.dp)
        )
    }
}
