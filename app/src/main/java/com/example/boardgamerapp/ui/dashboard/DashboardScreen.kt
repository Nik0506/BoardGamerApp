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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
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
    onSelectPlayer: (Long) -> Unit = {},
    onAddLateNotice: () -> Unit = {},
    onSelectLateNoticePreset: (Int) -> Unit = {},
    onLateNoticeCustomMinutesChange: (String) -> Unit = {},
    onSaveLateNotice: () -> Unit = {},
    onDismissLateNoticeEditor: () -> Unit = {},
    onDismissMessage: () -> Unit = {},
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
            uiState = uiState,
            onSelectPlayer = onSelectPlayer,
            onAddLateNotice = onAddLateNotice,
            onSelectLateNoticePreset = onSelectLateNoticePreset,
            onLateNoticeCustomMinutesChange = onLateNoticeCustomMinutesChange,
            onSaveLateNotice = onSaveLateNotice,
            onDismissLateNoticeEditor = onDismissLateNoticeEditor,
            onDismissMessage = onDismissMessage,
            modifier = modifier,
        )
    }
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState.Content,
    onSelectPlayer: (Long) -> Unit,
    onAddLateNotice: () -> Unit,
    onSelectLateNoticePreset: (Int) -> Unit,
    onLateNoticeCustomMinutesChange: (String) -> Unit,
    onSaveLateNotice: () -> Unit,
    onDismissLateNoticeEditor: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Board Gamer",
                modifier = Modifier.padding(top = 24.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Nächster Spieleabend",
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    DetailRow(label = "Datum", value = uiState.gameNight.date)
                    Spacer(modifier = Modifier.height(18.dp))
                    DetailRow(label = "Uhrzeit", value = uiState.gameNight.time)
                }
            }
        }

        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Gastgeber",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = uiState.gameNight.hostName,
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = uiState.gameNight.location,
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

        if (uiState.players.isNotEmpty()) {
            item {
                Text(
                    text = "Wer meldet die Verspätung?",
                    style = MaterialTheme.typography.labelLarge,
                )
                LazyRow(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.players, key = { it.id }) { player ->
                        FilterChip(
                            selected = player.id == uiState.selectedPlayerId,
                            onClick = { onSelectPlayer(player.id) },
                            label = { Text(player.name) },
                        )
                    }
                }
            }
        }

        uiState.message?.let { message ->
            item { DashboardMessageCard(message, false, onDismissMessage) }
        }
        uiState.errorMessage?.let { message ->
            item { DashboardMessageCard(message, true, onDismissMessage) }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Verspätung melden",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Lokale Demo: Die Meldung wird nur gespeichert und simuliert keine echte Benachrichtigung.",
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = onAddLateNotice,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        enabled = uiState.selectedPlayerId != null,
                    ) {
                        Text("Verspätung melden")
                    }
                }
            }
        }

        item {
            Text(
                text = "Aktuelle Meldungen",
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (uiState.lateNotices.isEmpty()) {
            item {
                Text(
                    text = "Für diesen Spieleabend gibt es noch keine Verspätungsmeldungen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(uiState.lateNotices, key = { it.id }) { notice ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${notice.playerName} kommt etwa ${notice.minutes} Minuten später.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "Gespeichert: ${notice.createdAt}",
                            modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Die Daten bleiben lokal auf diesem Gerät. Es werden keine Nachrichten an andere Spieler versendet.",
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    uiState.editor?.let { editor ->
        LateNoticeDialog(
            editor = editor,
            onSelectPreset = onSelectLateNoticePreset,
            onCustomMinutesChange = onLateNoticeCustomMinutesChange,
            onSave = onSaveLateNotice,
            onDismiss = onDismissLateNoticeEditor,
        )
    }
}

@Composable
private fun LateNoticeDialog(
    editor: LateNoticeEditorUiState,
    onSelectPreset: (Int) -> Unit,
    onCustomMinutesChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Verspätung melden") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Wie viele Minuten Verspätung sollen lokal gespeichert werden?")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 20, 30).forEach { minutes ->
                        FilterChip(
                            selected = editor.selectedPreset == minutes,
                            onClick = { onSelectPreset(minutes) },
                            label = { Text("$minutes Min.") },
                        )
                    }
                }
                OutlinedTextField(
                    value = editor.customMinutes,
                    onValueChange = onCustomMinutesChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Freie Minutenangabe") },
                    singleLine = true,
                )
                editor.errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text("Speichern") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}

@Composable
private fun DashboardMessageCard(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            TextButton(onClick = onDismiss) { Text("OK") }
        }
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
