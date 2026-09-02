package com.example.boardgamerapp.ui.players

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.boardgamerapp.data.repository.MoveDirection
import com.example.boardgamerapp.ui.theme.BoardGamerAppTheme

@Composable
fun PlayersScreen(
    uiState: PlayersUiState,
    onAddPlayer: () -> Unit,
    onEditPlayer: (Long) -> Unit,
    onMovePlayer: (Long, MoveDirection) -> Unit,
    onCreateNextGameNight: () -> Unit,
    onNameChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onSavePlayer: () -> Unit,
    onDismissEditor: () -> Unit,
    onDismissMessage: () -> Unit,
    screenTitle: String = "Spielgruppe",
    groupId: String? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    TextButton(onClick = onBack) {
                        Text("Zurück")
                    }
                }
            }
            Text(
                text = screenTitle,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Die Reihenfolge bestimmt, wer den nächsten Spieleabend ausrichtet.",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            groupId?.takeIf { it.isNotBlank() }?.let { currentGroupId ->
                val clipboardManager = LocalClipboardManager.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = currentGroupId,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gruppen-ID") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(currentGroupId))
                        },
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Text("Kopieren")
                    }
                }
            }
        }

        uiState.message?.let { message ->
            item {
                MessageCard(
                    message = message,
                    isError = false,
                    onDismiss = onDismissMessage,
                )
            }
        }

        uiState.errorMessage?.let { message ->
            item {
                MessageCard(
                    message = message,
                    isError = true,
                    onDismiss = onDismissMessage,
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
        } else if (uiState.players.isEmpty()) {
            item {
                Text(
                    text = "Noch keine Spieler angelegt.",
                    modifier = Modifier.padding(vertical = 24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            itemsIndexed(
                items = uiState.players,
                key = { _, player -> player.id },
            ) { index, player ->
                PlayerCard(
                    player = player,
                    canMoveUp = index > 0,
                    canMoveDown = index < uiState.players.lastIndex,
                    onMoveUp = { onMovePlayer(player.id, MoveDirection.UP) },
                    onMoveDown = { onMovePlayer(player.id, MoveDirection.DOWN) },
                )
            }
        }

        item {
            OutlinedButton(
                onClick = onCreateNextGameNight,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 24.dp),
                enabled = uiState.players.isNotEmpty(),
            ) {
                Text("Nächsten Spieleabend planen")
            }
        }
    }

    uiState.editor?.let { editor ->
        PlayerEditorDialog(
            editor = editor,
            onNameChange = onNameChange,
            onAddressChange = onAddressChange,
            onSave = onSavePlayer,
            onDismiss = onDismissEditor,
        )
    }
}

@Composable
private fun PlayerCard(
    player: PlayerUiModel,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${player.hostOrder}. ${player.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = player.address,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Text("Nach oben")
                }
                TextButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Text("Nach unten")
                }
            }
        }
    }
}

@Composable
private fun PlayerEditorDialog(
    editor: PlayerEditorUiState,
    onNameChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(editor.title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = editor.address,
                    onValueChange = onAddressChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Adresse") },
                    minLines = 2,
                    maxLines = 3,
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
            TextButton(onClick = onSave) {
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
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlayersScreenPreview() {
    BoardGamerAppTheme(dynamicColor = false) {
        PlayersScreen(
            uiState = PlayersUiState(
                isLoading = false,
                players = emptyList(),
            ),
            onAddPlayer = {},
            onEditPlayer = {},
            onMovePlayer = { _, _ -> },
            onCreateNextGameNight = {},
            onNameChange = {},
            onAddressChange = {},
            onSavePlayer = {},
            onDismissEditor = {},
            onDismissMessage = {},
        )
    }
}
