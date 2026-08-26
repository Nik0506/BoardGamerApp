package com.example.boardgamerapp.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.boardgamerapp.ui.dashboard.DashboardScreen
import com.example.boardgamerapp.ui.dashboard.DashboardViewModel
import com.example.boardgamerapp.ui.navigation.AppDestination
import com.example.boardgamerapp.ui.screen.PlaceholderScreen
import com.example.boardgamerapp.ui.theme.BoardGamerAppTheme

@Composable
fun BoardGamerApp() {
    val dashboardViewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
    var currentDestination by rememberSaveable {
        mutableStateOf(AppDestination.GAME_NIGHT)
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestination.entries.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            painter = painterResource(destination.icon),
                            contentDescription = destination.label,
                        )
                    },
                    label = { Text(destination.label) },
                    selected = destination == currentDestination,
                    onClick = { currentDestination = destination },
                )
            }
        },
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestination.GAME_NIGHT -> DashboardScreen(
                    uiState = dashboardViewModel.uiState,
                    onRetry = dashboardViewModel::loadGameNight,
                    modifier = Modifier.padding(innerPadding),
                )

                else -> PlaceholderScreen(
                    title = currentDestination.title,
                    description = currentDestination.description,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun BoardGamerAppPreview() {
    BoardGamerAppTheme(dynamicColor = false) {
        BoardGamerApp()
    }
}
