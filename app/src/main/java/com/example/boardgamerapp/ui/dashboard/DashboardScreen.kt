package com.example.boardgamerapp.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.boardgamerapp.ui.theme.BoardGamerAppTheme

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        DashboardUiState.Loading -> CenteredMessage(modifier = modifier) {
            CircularProgressIndicator()
            Text(
                text = "Spieleabend wird geladen …",
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        DashboardUiState.Empty -> CenteredMessage(modifier = modifier) {
            Text(
                text = "Noch kein Spieleabend geplant",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Sobald ein Termin feststeht, findest du ihn hier.",
                modifier = Modifier.padding(top = 12.dp),
                textAlign = TextAlign.Center,
            )
        }

        is DashboardUiState.Error -> CenteredMessage(modifier = modifier) {
            Text(
                text = "Das hat leider nicht geklappt",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = uiState.message,
                modifier = Modifier.padding(top = 12.dp),
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 20.dp),
            ) {
                Text("Erneut versuchen")
            }
        }

        is DashboardUiState.Content -> DashboardContent(
            gameNight = uiState.gameNight,
            modifier = modifier,
        )
    }
}

@Composable
private fun DashboardContent(
    gameNight: GameNightUiModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text(
            text = "Board Gamer",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Nächster Spieleabend",
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                DetailRow(label = "Datum", value = gameNight.date)
                Spacer(modifier = Modifier.height(18.dp))
                DetailRow(label = "Uhrzeit", value = gameNight.time)
            }
        }

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Gastgeber",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = gameNight.hostName,
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = gameNight.location,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        Text(
            text = "Spielvorschläge und Abstimmung folgen in den nächsten Iterationen.",
            modifier = Modifier.padding(top = 24.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun CenteredMessage(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardContentPreview() {
    BoardGamerAppTheme(dynamicColor = false) {
        DashboardScreen(
            uiState = DashboardUiState.Content(
                GameNightUiModel(
                    date = "Freitag, 28. August 2026",
                    time = "19:00 Uhr",
                    hostName = "Max Mustermann",
                    location = "Musterstraße 12, 33100 Paderborn",
                ),
            ),
            onRetry = {},
        )
    }
}
