package com.example.boardgamerapp.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
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
import com.example.boardgamerapp.domain.model.AttendanceStatusType
import com.example.boardgamerapp.domain.model.GameNightStatus
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
    onBeginCancelGameNight: () -> Unit = {},
    onCancelReasonChange: (String) -> Unit = {},
    onConfirmCancelGameNight: () -> Unit = {},
    onDismissCancelGameNight: () -> Unit = {},
    onConfirmAttending: () -> Unit = {},
    onBeginStatusReport: () -> Unit = {},
    onSelectStatusReportType: (StatusReportType) -> Unit = {},
    onDeclineReasonChange: (String) -> Unit = {},
    onSelectLateNoticePreset: (Int) -> Unit = {},
    onLateNoticeCustomMinutesChange: (String) -> Unit = {},
    onSaveStatusReport: () -> Unit = {},
    onDismissStatusReport: () -> Unit = {},
    onSelectGameNight: (String, String) -> Unit = { _, _ -> },
    onPlanGameNight: () -> Unit = {},
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
            Button(
                onClick = onPlanGameNight,
                modifier = Modifier.padding(top = 20.dp),
            ) {
                Text("Spieleabend planen")
            }
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
            onBeginCancelGameNight = onBeginCancelGameNight,
            onCancelReasonChange = onCancelReasonChange,
            onConfirmCancelGameNight = onConfirmCancelGameNight,
            onDismissCancelGameNight = onDismissCancelGameNight,
            onConfirmAttending = onConfirmAttending,
            onBeginStatusReport = onBeginStatusReport,
            onSelectStatusReportType = onSelectStatusReportType,
            onDeclineReasonChange = onDeclineReasonChange,
            onSelectLateNoticePreset = onSelectLateNoticePreset,
            onLateNoticeCustomMinutesChange = onLateNoticeCustomMinutesChange,
            onSaveStatusReport = onSaveStatusReport,
            onDismissStatusReport = onDismissStatusReport,
            onSelectGameNight = onSelectGameNight,
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
    onBeginCancelGameNight: () -> Unit,
    onCancelReasonChange: (String) -> Unit,
    onConfirmCancelGameNight: () -> Unit,
    onDismissCancelGameNight: () -> Unit,
    onConfirmAttending: () -> Unit,
    onBeginStatusReport: () -> Unit,
    onSelectStatusReportType: (StatusReportType) -> Unit,
    onDeclineReasonChange: (String) -> Unit,
    onSelectLateNoticePreset: (Int) -> Unit,
    onLateNoticeCustomMinutesChange: (String) -> Unit,
    onSaveStatusReport: () -> Unit,
    onDismissStatusReport: () -> Unit,
    onSelectGameNight: (String, String) -> Unit,
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
                    if (uiState.gameNight.groupName.isNotBlank()) {
                        Text(
                            text = uiState.gameNight.groupName,
                            modifier = Modifier.padding(top = 2.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
                        if (uiState.gameNight.status == GameNightStatus.PLANNED) {
                            DropdownMenuItem(
                                text = { Text("Spieleabend absagen") },
                                onClick = {
                                    menuExpanded = false
                                    onBeginCancelGameNight()
                                },
                            )
                        }
                    }
                }
            }
        }

        if (uiState.gameNight.status == GameNightStatus.CANCELLED) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "❌ Dieser Spieleabend wurde abgesagt.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        uiState.gameNight.cancelReason?.takeIf { it.isNotBlank() }?.let { reason ->
                            Text(
                                text = "Grund: $reason",
                                modifier = Modifier.padding(top = 4.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }
        }

        if (uiState.upcomingGameNights.isNotEmpty()) {
            item {
                Text(
                    text = "Deine anstehenden Termine",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.upcomingGameNights, key = { "${it.groupId}_${it.gameNightDocId}" }) { entry ->
                        GameNightPickerCard(
                            entry = entry,
                            onClick = { onSelectGameNight(entry.groupId, entry.gameNightDocId) },
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
                        text = "Mein Teilnahmestatus",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val currentAtt = uiState.currentAttendance
                    val statusDescription = when (currentAtt?.status) {
                        AttendanceStatusType.ATTENDING -> "Du hast zugesagt (pünktlich dabei)."
                        AttendanceStatusType.LATE -> "Du kommst ca. ${currentAtt.minutesLate ?: 10} Minuten später."
                        AttendanceStatusType.DECLINED -> "Du hast abgesagt." + (currentAtt.reason?.let { " ($it)" } ?: "")
                        AttendanceStatusType.PENDING, null -> "Du hast für diesen Abend noch nicht reagiert."
                    }
                    Text(
                        text = if (uiState.selectedPlayerId != null) statusDescription else "Dein Konto ist kein Mitglied der aktiven Gruppe.",
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val rsvpEnabled = uiState.selectedPlayerId != null && uiState.gameNight.status == GameNightStatus.PLANNED
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onConfirmAttending,
                            modifier = Modifier.weight(1f),
                            enabled = rsvpEnabled,
                        ) {
                            Text("Zusagen")
                        }
                        Button(
                            onClick = onBeginStatusReport,
                            modifier = Modifier.weight(1f),
                            enabled = rsvpEnabled,
                        ) {
                            Text("Status melden")
                        }
                    }
                }
            }
        }

        val showCancelNotice = uiState.gameNight.status == GameNightStatus.CANCELLED
        if (uiState.recentNotices.isNotEmpty() || showCancelNotice) {
            item {
                Text(
                    text = "Aktuelle Meldungen",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (showCancelNotice) {
                item {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Der Spieleabend wurde abgesagt." +
                                    (uiState.gameNight.cancelReason?.takeIf { it.isNotBlank() }?.let { " Grund: $it" } ?: ""),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            uiState.gameNight.cancelledAt?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
            items(uiState.recentNotices, key = { "notice_${it.playerId}" }) { notice ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        val noticeText = when (notice.status) {
                            AttendanceStatusType.LATE ->
                                "${notice.playerName} verspätet sich um ${notice.minutesLate ?: 10} Minuten."
                            AttendanceStatusType.DECLINED ->
                                "${notice.playerName} hat abgesagt." +
                                    (notice.reason?.takeIf { it.isNotBlank() }?.let { " Grund: $it" } ?: "")
                            else -> ""
                        }
                        Text(text = noticeText, style = MaterialTheme.typography.bodyMedium)
                        notice.updatedAt?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Teilnahme der Gruppe",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = "${uiState.attendingCount} Zugesagt • ${uiState.lateCount} Verspätet • ${uiState.declinedCount} Abgesagt • ${uiState.pendingCount} Offen",
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }

        if (uiState.attendances.isEmpty()) {
            item {
                Text(
                    text = "Noch keine Gruppenmitglieder vorhanden.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(uiState.attendances, key = { it.playerId }) { att ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = att.playerName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (att.isCurrentPlayer) {
                                    Text(
                                        text = " (Du)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 4.dp),
                                    )
                                }
                            }
                            if (att.status == AttendanceStatusType.DECLINED && !att.reason.isNullOrBlank()) {
                                Text(
                                    text = "Grund: ${att.reason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            if (att.updatedAt != null) {
                                Text(
                                    text = "Aktualisiert: ${att.updatedAt}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }

                        val badgeText = when (att.status) {
                            AttendanceStatusType.ATTENDING -> "✅ Dabei"
                            AttendanceStatusType.LATE -> "⏰ +${att.minutesLate ?: 10} Min"
                            AttendanceStatusType.DECLINED -> "❌ Abgesagt"
                            AttendanceStatusType.PENDING -> "⏳ Offen"
                        }
                        val badgeColor = when (att.status) {
                            AttendanceStatusType.ATTENDING -> MaterialTheme.colorScheme.primaryContainer
                            AttendanceStatusType.LATE -> MaterialTheme.colorScheme.tertiaryContainer
                            AttendanceStatusType.DECLINED -> MaterialTheme.colorScheme.errorContainer
                            AttendanceStatusType.PENDING -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val textColor = when (att.status) {
                            AttendanceStatusType.ATTENDING -> MaterialTheme.colorScheme.onPrimaryContainer
                            AttendanceStatusType.LATE -> MaterialTheme.colorScheme.onTertiaryContainer
                            AttendanceStatusType.DECLINED -> MaterialTheme.colorScheme.onErrorContainer
                            AttendanceStatusType.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Surface(
                            color = badgeColor,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelMedium,
                                color = textColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Die Statusmeldungen werden mit der Gruppe geteilt und halten alle auf dem aktuellen Stand.",
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

    uiState.cancelGameNightEditor?.let { editor ->
        CancelGameNightDialog(
            editor = editor,
            onReasonChange = onCancelReasonChange,
            onConfirm = onConfirmCancelGameNight,
            onDismiss = onDismissCancelGameNight,
        )
    }

    uiState.statusReportEditor?.let { editor ->
        StatusReportDialog(
            editor = editor,
            onSelectType = onSelectStatusReportType,
            onSelectPreset = onSelectLateNoticePreset,
            onCustomMinutesChange = onLateNoticeCustomMinutesChange,
            onReasonChange = onDeclineReasonChange,
            onSave = onSaveStatusReport,
            onDismiss = onDismissStatusReport,
        )
    }
}

@Composable
private fun StatusReportDialog(
    editor: StatusReportEditorUiState,
    onSelectType: (StatusReportType) -> Unit,
    onSelectPreset: (Int) -> Unit,
    onCustomMinutesChange: (String) -> Unit,
    onReasonChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Status melden") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Wähle die Art der Meldung.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = editor.type == StatusReportType.LATE,
                        onClick = { onSelectType(StatusReportType.LATE) },
                        label = { Text("Verspätung") },
                    )
                    FilterChip(
                        selected = editor.type == StatusReportType.DECLINED,
                        onClick = { onSelectType(StatusReportType.DECLINED) },
                        label = { Text("Absage") },
                    )
                }

                when (editor.type) {
                    StatusReportType.LATE -> {
                        Text("Wie viele Minuten Verspätung sollen gemeldet werden?")
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
                    }
                    StatusReportType.DECLINED -> {
                        Text("Möchtest du deine Teilnahme für diesen Spieleabend absagen?")
                        OutlinedTextField(
                            value = editor.reason,
                            onValueChange = onReasonChange,
                            label = { Text("Grund für die Absage (optional)") },
                            placeholder = { Text("z. B. krank, Termin, unterwegs") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                }

                editor.errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !editor.isSaving,
            ) {
                if (editor.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(if (editor.type == StatusReportType.DECLINED) "Absage bestätigen" else "Verspätung melden")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !editor.isSaving,
            ) {
                Text("Abbrechen")
            }
        },
    )
}

@Composable
private fun CancelGameNightDialog(
    editor: CancelGameNightEditorUiState,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Spieleabend absagen") },
        text = {
            Column {
                Text(
                    text = "Möchtest du diesen Spieleabend wirklich absagen? Alle Mitglieder werden per Push-Nachricht informiert.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = editor.reason,
                    onValueChange = onReasonChange,
                    label = { Text("Meldungstext (optional)") },
                    placeholder = { Text("z. B. Gastgeber ist erkrankt") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    singleLine = true,
                )
                editor.errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !editor.isSaving,
            ) {
                if (editor.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Absagen bestätigen")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !editor.isSaving,
            ) {
                Text("Abbrechen")
            }
        },
    )
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
private fun GameNightPickerCard(
    entry: GameNightPickerUiModel,
    onClick: () -> Unit,
) {
    val borderColor = if (entry.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (entry.isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (entry.isSelected) 2.dp else 1.dp, borderColor),
        modifier = Modifier.semantics { contentDescription = "Termin ${entry.groupName} ${entry.date}" },
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .widthIn(min = 160.dp),
        ) {
            Text(
                text = entry.groupName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (entry.isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
            )
            Text(
                text = entry.date,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = entry.time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Gastgeber: ${entry.hostName}",
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
            )
            if (entry.hasCollision) {
                Text(
                    text = "⚠️ Terminkonflikt",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
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
