package com.example.boardgamerapp.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.boardgamerapp.ui.theme.BoardGamerAppTheme

@Composable
fun GamesScreen(
    uiState: GamesUiState,
    onSelectPlayer: (Long) -> Unit,
    onAddSuggestion: () -> Unit,
    onDeleteSuggestion: (Long) -> Unit,
    onCastVote: (Long) -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSaveSuggestion: () -> Unit,
    onDismissEditor: () -> Unit,
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
                text = "Spielvorschläge",
                modifier = Modifier.padding(top = 24.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = uiState.gameNightDate?.let { "Für $it" }
                    ?: "Aktuell ist kein Spieleabend geplant.",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (uiState.players.isNotEmpty()) {
            item {
                Text(
                    text = "Wer verwendet die App?",
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
            item { MessageCard(message, false, onDismissMessage) }
        }
        uiState.errorMessage?.let { message ->
            item { MessageCard(message, true, onDismissMessage) }
        }

        if (!uiState.isLoading && uiState.gameNightDate != null) {
            item {
                VotingSummaryCard(
                    resultText = uiState.resultText,
                    totalVotes = uiState.totalVotes,
                    playerCount = uiState.playerCount,
                )
            }
        }

        if (uiState.isLoading) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (uiState.suggestions.isEmpty()) {
            item {
                Text(
                    text = "Noch keine Spiele vorgeschlagen. Mach den Anfang!",
                    modifier = Modifier.padding(vertical = 24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            items(uiState.suggestions, key = { it.id }) { suggestion ->
                SuggestionCard(
                    suggestion = suggestion,
                    canDelete = suggestion.suggestedByPlayerId == uiState.selectedPlayerId,
                    onDelete = { onDeleteSuggestion(suggestion.id) },
                    onCastVote = { onCastVote(suggestion.id) },
                )
            }
        }

        item {
            Button(
                onClick = onAddSuggestion,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                enabled = uiState.selectedPlayerId != null && uiState.gameNightDate != null,
            ) {
                Text("Spiel vorschlagen")
            }
        }
    }

    uiState.editor?.let { editor ->
        SuggestionEditorDialog(
            editor = editor,
            onNameChange = onNameChange,
            onDescriptionChange = onDescriptionChange,
            onSave = onSaveSuggestion,
            onDismiss = onDismissEditor,
        )
    }
}

@Composable
private fun SuggestionCard(
    suggestion: GameSuggestionUiModel,
    canDelete: Boolean,
    onDelete: () -> Unit,
    onCastVote: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (suggestion.isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = suggestion.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (suggestion.description.isNotEmpty()) {
                Text(
                    text = suggestion.description,
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = "Vorgeschlagen von ${suggestion.suggestedByName}",
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Termin: ${suggestion.gameNightDate}",
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (suggestion.voteCount == 1) "1 Stimme" else "${suggestion.voteCount} Stimmen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Button(
                    onClick = onCastVote,
                    enabled = !suggestion.isSelected,
                ) {
                    Text(if (suggestion.isSelected) "Deine Stimme" else "Abstimmen")
                }
            }
            if (canDelete) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Löschen")
                }
            }
        }
    }
}

@Composable
private fun VotingSummaryCard(
    resultText: String,
    totalVotes: Int,
    playerCount: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Abstimmung",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = resultText,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "$totalVotes von $playerCount haben abgestimmt",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SuggestionEditorDialog(
    editor: GameSuggestionEditorUiState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Spiel vorschlagen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Spielname") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = editor.description,
                    onValueChange = onDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Beschreibung (optional)") },
                    minLines = 3,
                    maxLines = 5,
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
            TextButton(onClick = onSave) { Text("Hinzufügen") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}

@Composable
private fun MessageCard(
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

@Preview(showBackground = true)
@Composable
private fun GamesScreenPreview() {
    BoardGamerAppTheme(dynamicColor = false) {
        GamesScreen(
            uiState = GamesUiState(
                isLoading = false,
                players = emptyList(),
                selectedPlayerId = null,
                gameNightDate = "Freitag, 28. August 2026",
                suggestions = emptyList(),
            ),
            onSelectPlayer = {},
            onAddSuggestion = {},
            onDeleteSuggestion = {},
            onCastVote = {},
            onNameChange = {},
            onDescriptionChange = {},
            onSaveSuggestion = {},
            onDismissEditor = {},
            onDismissMessage = {},
        )
    }
}
