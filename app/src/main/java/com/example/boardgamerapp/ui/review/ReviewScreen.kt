package com.example.boardgamerapp.ui.review

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ReviewScreen(
    uiState: ReviewUiState,
    onRetry: () -> Unit,
    onFinishGameNight: () -> Unit,
    onBeginReview: () -> Unit,
    onHostRating: (Int) -> Unit,
    onFoodRating: (Int) -> Unit,
    onEveningRating: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    onSaveReview: () -> Unit,
    onDismissEditor: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        ReviewUiState.Loading -> Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator() }
        ReviewUiState.Empty -> Column(modifier.padding(24.dp)) { Text("Noch kein Spieleabend vorhanden.") }
        is ReviewUiState.Error -> Column(modifier.padding(24.dp)) {
            Text(uiState.message, color = MaterialTheme.colorScheme.error)
            Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) { Text("Erneut versuchen") }
        }
        is ReviewUiState.Content -> {
            LazyColumn(
                modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text("Abschluss & Bewertung", modifier = Modifier.padding(top = 24.dp), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("${uiState.date} · Gastgeber: ${uiState.hostName}", modifier = Modifier.padding(top = 6.dp))
                }
                if (!uiState.isFinished) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Der Spieleabend ist noch nicht abgeschlossen.")
                                Button(onClick = onFinishGameNight, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("Spieleabend abschließen") }
                            }
                        }
                    }
                } else {
                    item {
                        Text("Bewertungen: ${uiState.reviewCount}", style = MaterialTheme.typography.titleMedium)
                        uiState.averages?.let {
                            OutlinedCard(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Durchschnitt", fontWeight = FontWeight.SemiBold)
                                    Text("Gastgeber: ${it.host} / 5")
                                    Text("Essen: ${it.food} / 5")
                                    Text("Gesamtabend: ${it.evening} / 5")
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            when {
                                uiState.currentPlayerHasReviewed -> "${uiState.currentPlayerName.orEmpty()}: Du hast bereits bewertet."
                                uiState.currentPlayerName != null -> "Bewertung für dein Konto ${uiState.currentPlayerName}"
                                else -> "Dein Konto ist kein Mitglied der aktiven Gruppe."
                            },
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    item {
                        Button(onClick = onBeginReview, enabled = uiState.selectedPlayerId != null, modifier = Modifier.fillMaxWidth()) { Text("Bewertung abgeben") }
                    }
                }
                uiState.message?.let { item { Message(it, false, onDismissMessage) } }
                uiState.errorMessage?.let { item { Message(it, true, onDismissMessage) } }
            }
            uiState.editor?.let {
                ReviewDialog(it, onHostRating, onFoodRating, onEveningRating, onCommentChange, onSaveReview, onDismissEditor)
            }
        }
    }
}

@Composable
private fun ReviewDialog(
    editor: RatingEditorUiState,
    onHostRating: (Int) -> Unit,
    onFoodRating: (Int) -> Unit,
    onEveningRating: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Spieleabend bewerten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RatingRow("Gastgeber", editor.hostRating, onHostRating)
                RatingRow("Essen", editor.foodRating, onFoodRating)
                RatingRow("Gesamtabend", editor.eveningRating, onEveningRating)
                OutlinedTextField(editor.comment, onCommentChange, Modifier.fillMaxWidth(), label = { Text("Kommentar (optional)") })
                editor.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Speichern") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@Composable
private fun RatingRow(label: String, selected: Int, onSelect: (Int) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items((1..5).toList()) { rating ->
                FilterChip(
                    selected = rating == selected,
                    onClick = { onSelect(rating) },
                    label = { Text(rating.toString()) },
                    modifier = Modifier.semantics {
                        contentDescription = "$label: $rating von 5 Punkten"
                    },
                )
            }
        }
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
