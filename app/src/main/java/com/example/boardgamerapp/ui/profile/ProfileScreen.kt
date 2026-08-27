package com.example.boardgamerapp.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.boardgamerapp.domain.model.Player

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onGameNightPlanned: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    var editedPlayer by remember { mutableStateOf<Player?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Spielgruppe", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = {
            editedPlayer = null
            showDialog = true
        }) {
            Text("Spieler hinzufügen")
        }
        OutlinedButton(onClick = {
            viewModel.planNextGameNight(onGameNightPlanned)
        }) {
            Text("Nächsten Termin planen")
        }

        when {
            state.isLoading -> CircularProgressIndicator()
            state.errorMessage != null -> {
                state.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                OutlinedButton(onClick = viewModel::loadPlayers) {
                    Text("Erneut versuchen")
                }
            }
            state.players.isEmpty() -> Text("Noch keine Spieler vorhanden.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.players, key = { it.id }) { player ->
                    PlayerCard(player = player, onEdit = {
                        editedPlayer = player
                        showDialog = true
                    })
                }
            }
        }
    }

    if (showDialog) {
        PlayerDialog(
            player = editedPlayer,
            onDismiss = { showDialog = false },
            onSave = { id, name, address ->
                val error = viewModel.savePlayer(id, name, address)
                if (error == null) showDialog = false
                error
            },
        )
    }
}

@Composable
private fun PlayerCard(player: Player, onEdit: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(player.name, style = MaterialTheme.typography.titleMedium)
                Text("Gastgeberreihenfolge: ${player.hostOrder}")
                Text(player.address, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onEdit) {
                Text("Bearbeiten")
            }
        }
    }
}

@Composable
private fun PlayerDialog(
    player: Player?,
    onDismiss: () -> Unit,
    onSave: (String?, String, String) -> String?,
) {
    var name by remember(player) { mutableStateOf(player?.name.orEmpty()) }
    var address by remember(player) { mutableStateOf(player?.address.orEmpty()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (player == null) "Spieler hinzufügen" else "Spieler bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Adresse") },
                    singleLine = true,
                )
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = { errorMessage = onSave(player?.id, name, address) }) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        },
    )
}
