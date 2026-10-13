package com.antigravity.somnolog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.antigravity.somnolog.ui.dashboard.DashboardScreen
import com.antigravity.somnolog.ui.dashboard.DashboardViewModel
import com.antigravity.somnolog.ui.history.HistoryScreen
import com.antigravity.somnolog.ui.history.HistoryViewModel
import com.antigravity.somnolog.ui.theme.OledBlack
import com.antigravity.somnolog.ui.theme.SomnoLogTheme

class MainActivity : ComponentActivity() {

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SomnoLogTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = OledBlack
                ) {
                    SomnoLogAppNavigation(
                        dashboardViewModel = dashboardViewModel,
                        historyViewModel = historyViewModel
                    )
                }
            }
        }
    }
}

enum class CurrentScreen {
    DASHBOARD,
    HISTORY
}

@Composable
fun SomnoLogAppNavigation(
    dashboardViewModel: DashboardViewModel,
    historyViewModel: HistoryViewModel
) {
    var currentScreen by remember { mutableStateOf(CurrentScreen.DASHBOARD) }

    when (currentScreen) {
        CurrentScreen.DASHBOARD -> {
            DashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateToHistory = { currentScreen = CurrentScreen.HISTORY }
            )
        }
        CurrentScreen.HISTORY -> {
            HistoryScreen(
                viewModel = historyViewModel,
                onNavigateBack = { currentScreen = CurrentScreen.DASHBOARD }
            )
        }
    }
}
