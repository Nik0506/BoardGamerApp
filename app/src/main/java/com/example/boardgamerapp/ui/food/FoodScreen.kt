package com.example.boardgamerapp.ui.food

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
import androidx.compose.ui.unit.dp

@Composable
fun FoodScreen(
    uiState: FoodUiState,
    onSelectPlayer: (Long) -> Unit,
    onCastVote: (Long) -> Unit,
    onAddCategory: () -> Unit,
    onCategoryNameChange: (String) -> Unit,
    onSaveCategory: () -> Unit,
    onDismissCategoryEditor: () -> Unit,
    onDeleteCategory: (Long) -> Unit,
    onRemindMissingPlayers: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Essensabstimmung", Modifier.padding(top = 24.dp), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(uiState.gameNightDate?.let { "Für $it" } ?: "Aktuell ist kein Spieleabend geplant.", Modifier.padding(top = 6.dp))
        }
        if (uiState.players.isNotEmpty()) {
            item {
                Text("Wer stimmt ab?", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.players, key = { it.id }) { player ->
                        FilterChip(player.id == uiState.selectedPlayerId, { onSelectPlayer(player.id) }, label = { Text(player.name) })
                    }
                }
            }
        }
        uiState.message?.let { item { Message(it, false, onDismissMessage) } }
        uiState.errorMessage?.let { item { Message(it, true, onDismissMessage) } }
        if (uiState.isLoading) {
            item { Row(Modifier.fillMaxWidth().padding(32.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
        } else if (uiState.gameNightDate != null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(uiState.resultText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${uiState.totalVotes} von ${uiState.players.size} haben abgestimmt", Modifier.padding(top = 4.dp))
                    }
                }
            }
            items(uiState.categories, key = { it.id }) { category ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (category.isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(category.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Text(if (category.voteCount == 1) "1 Stimme" else "${category.voteCount} Stimmen")
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Button(onClick = { onCastVote(category.id) }, enabled = !category.isSelected) {
                                Text(if (category.isSelected) "Deine Stimme" else "Abstimmen")
                            }
                            TextButton(onClick = { onDeleteCategory(category.id) }) { Text("Löschen") }
                        }
                    }
                }
            }
            item {
                Button(onClick = onAddCategory, modifier = Modifier.fillMaxWidth()) { Text("Kategorie hinzufügen") }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Fehlende Stimmen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (uiState.missingPlayerNames.isEmpty()) "Alle haben abgestimmt." else uiState.missingPlayerNames.joinToString(),
                            Modifier.padding(top = 4.dp),
                        )
                        TextButton(onClick = onRemindMissingPlayers) { Text("Lokal erinnern") }
                        Text("Es wird keine Nachricht an andere Geräte gesendet.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item { Text("", Modifier.padding(bottom = 24.dp)) }
    }
    uiState.categoryEditor?.let { value ->
        AlertDialog(
            onDismissRequest = onDismissCategoryEditor,
            title = { Text("Essenskategorie") },
            text = {
                Column {
                    OutlinedTextField(value, onCategoryNameChange, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
                    uiState.editorError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp)) }
                }
            },
            confirmButton = { TextButton(onClick = onSaveCategory) { Text("Hinzufügen") } },
            dismissButton = { TextButton(onClick = onDismissCategoryEditor) { Text("Abbrechen") } },
        )
    }
}

@Composable
private fun Message(text: String, error: Boolean, onDismiss: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    }
}
