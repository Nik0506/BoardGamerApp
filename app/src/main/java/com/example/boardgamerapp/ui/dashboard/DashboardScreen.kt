package com.example.boardgamerapp.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.boardgamerapp.domain.model.GameNight
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        DashboardUiState.Loading -> LoadingContent(modifier)
        DashboardUiState.Empty -> MessageContent(
            title = "Kein Spieleabend geplant",
            message = "Sobald ein neuer Termin feststeht, wird er hier angezeigt.",
            modifier = modifier,
        )
        is DashboardUiState.Error -> MessageContent(
            title = "Daten konnten nicht geladen werden",
            message = state.message,
            actionLabel = "Erneut versuchen",
            onAction = viewModel::load,
            modifier = modifier,
        )
        is DashboardUiState.Success -> GameNightContent(
            gameNight = state.gameNight,
            hostName = state.host.name,
            modifier = modifier,
        )
    }
}

@Composable
private fun LoadingContent(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(
            text = "Spieleabend wird geladen …",
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun GameNightContent(
    gameNight: GameNight,
    hostName: String,
    modifier: Modifier,
) {
    val dateTime = DateFormat.getDateTimeInstance(
        DateFormat.FULL,
        DateFormat.SHORT,
        Locale.GERMANY,
    ).format(Date(gameNight.startsAtEpochMillis))

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Nächster Spieleabend",
            style = MaterialTheme.typography.headlineMedium,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DetailRow(label = "Wann", value = dateTime)
                DetailRow(label = "Gastgeber", value = hostName)
                DetailRow(label = "Wo", value = gameNight.location)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Text(
            text = value,
            modifier = Modifier.padding(start = 16.dp),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun MessageContent(
    title: String,
    message: String,
    modifier: Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 12.dp),
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                modifier = Modifier.padding(top = 20.dp),
            ) {
                Text(actionLabel)
            }
        }
    }
}
