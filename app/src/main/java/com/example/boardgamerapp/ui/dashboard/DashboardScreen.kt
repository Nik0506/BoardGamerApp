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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.boardgamerapp.R
import com.example.boardgamerapp.ui.theme.BoardGamerAppTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onEditGameNight: () -> Unit = {},
    onGameNightDateChange: (LocalDate) -> Unit = {},
    onGameNightTimeChange: (LocalTime) -> Unit = {},
    onGameNightHostChange: (Long) -> Unit = {},
    onSaveEditedGameNight: () -> Unit = {},
    onDismissGameNightEditor: () -> Unit = {},
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
            onEditGameNight = onEditGameNight,
            onGameNightDateChange = onGameNightDateChange,
            onGameNightTimeChange = onGameNightTimeChange,
            onGameNightHostChange = onGameNightHostChange,
            onSaveEditedGameNight = onSaveEditedGameNight,
            onDismissGameNightEditor = onDismissGameNightEditor,
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
    onEditGameNight: () -> Unit,
    onGameNightDateChange: (LocalDate) -> Unit,
    onGameNightTimeChange: (LocalTime) -> Unit,
    onGameNightHostChange: (Long) -> Unit,
    onSaveEditedGameNight: () -> Unit,
    onDismissGameNightEditor: () -> Unit,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Würfelrunde",
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

                var menuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.semantics { contentDescription = "Optionen" },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_more_vert),
                            contentDescription = "Optionen",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Spieleabend editieren") },
                            onClick = {
                                menuExpanded = false
                                onEditGameNight()
                            },
                        )
                    }
                }
            }
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
                        text = uiState.gameNight.location.ifBlank { "Keine Adresse hinterlegt" },
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
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
                        text = uiState.players.firstOrNull { it.id == uiState.selectedPlayerId }
                            ?.let { "Die Meldung wird für dein Konto ${it.name} gespeichert." }
                            ?: "Dein Konto ist kein Mitglied der aktiven Gruppe.",
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
                text = "Die Verspätungsmeldungen sind für die aktuelle Spielrunde relevant und werden mit der Gruppe geteilt.",
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    uiState.gameNightEditor?.let { editor ->
        EditGameNightDialog(
            editor = editor,
            players = uiState.players,
            onDateChange = onGameNightDateChange,
            onTimeChange = onGameNightTimeChange,
            onHostChange = onGameNightHostChange,
            onSave = onSaveEditedGameNight,
            onDismiss = onDismissGameNightEditor,
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditGameNightDialog(
    editor: GameNightEditorUiState,
    players: List<DashboardPlayerUiModel>,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (LocalTime) -> Unit,
    onHostChange: (Long) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = editor.selectedDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli(),
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = datePickerState.selectedDateMillis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        } ?: editor.selectedDate
                        onDateChange(picked)
                        showDatePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Abbrechen")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = editor.selectedTime.hour,
            initialMinute = editor.selectedTime.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Uhrzeit auswählen") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeChange(LocalTime.of(timePickerState.hour, timePickerState.minute))
                        showTimePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Abbrechen")
                }
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Spieleabend editieren") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Passe Datum, Uhrzeit und Gastgeber für diesen Spieleabend an.")

                Text("Datum", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = editor.selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Datum") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Datum ändern")
                }

                Text("Uhrzeit", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = editor.selectedTime.format(DateTimeFormatter.ofPattern("HH:mm 'Uhr'", Locale.GERMAN)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Uhrzeit") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Uhrzeit ändern")
                }

                Text("Gastgeber", style = MaterialTheme.typography.titleSmall)
                val selectedMember = players.firstOrNull { it.id == editor.selectedHostId }
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                ) {
                    OutlinedTextField(
                        value = selectedMember?.name ?: "Bitte wählen",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gastgeber auswählen") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        players.forEach { player ->
                            DropdownMenuItem(
                                text = { Text(player.name) },
                                onClick = {
                                    onHostChange(player.id)
                                    expanded = false
                                },
                            )
                        }
                    }
                }

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
            TextButton(
                onClick = onSave,
                enabled = !editor.isSaving,
            ) {
                Text(if (editor.isSaving) "Speichern..." else "Speichern")
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
                    hostName = "Gastgeber",
                    location = "Keine Adresse hinterlegt",
                ),
            ),
            onRetry = {},
        )
    }
}
