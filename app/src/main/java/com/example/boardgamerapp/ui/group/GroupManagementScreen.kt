package com.example.boardgamerapp.ui.group

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.boardgamerapp.data.group.Group
import com.example.boardgamerapp.data.group.GroupMember
import com.example.boardgamerapp.data.group.GroupRepository
import com.example.boardgamerapp.data.repository.FirebaseGameNightRepository
import com.example.boardgamerapp.data.repository.MoveDirection
import com.example.boardgamerapp.ui.players.PlayerUiModel
import com.example.boardgamerapp.ui.players.PlayersScreen
import com.example.boardgamerapp.ui.players.PlayersUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
fun GroupManagementScreen(
    onGroupReady: () -> Unit,
    userUid: String,
    modifier: Modifier = Modifier,
) {
    val groupRepository = remember { GroupRepository() }
    val gameNightRepository = remember { FirebaseGameNightRepository() }
    var groupName by rememberSaveable(userUid) { mutableStateOf("") }
    var groupId by rememberSaveable(userUid) { mutableStateOf("") }
    var errorText by rememberSaveable(userUid) { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var userGroups by remember { mutableStateOf<List<Group>>(emptyList()) }
    var selectedGroupId by rememberSaveable(userUid) { mutableStateOf<String?>(null) }
    var members by remember { mutableStateOf<List<GroupMember>>(emptyList()) }
    var showCreateJoinOptions by rememberSaveable(userUid) { mutableStateOf(false) }
    var showGameNightDialog by rememberSaveable(userUid) { mutableStateOf(false) }
    var plannedGameNightDate by rememberSaveable(userUid) { mutableStateOf(LocalDate.now().plusWeeks(2)) }
    var selectedGameNightHostUid by rememberSaveable(userUid) { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedGroupId, members) {
        if (selectedGameNightHostUid.isNullOrBlank() || members.none { it.uid == selectedGameNightHostUid }) {
            selectedGameNightHostUid = members.firstOrNull()?.uid
        }
    }

    fun loadMembers(groupIdToLoad: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) { groupRepository.getMembers(groupIdToLoad) }
            result.fold(
                onSuccess = { members = it },
                onFailure = {
                    members = emptyList()
                    errorText = it.message ?: "Mitglieder konnten nicht geladen werden."
                },
            )
        }
    }

    fun loadUserGroups() {
        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) { groupRepository.getGroupsForCurrentUser() }
            result.fold(
                onSuccess = { userGroups = it },
                onFailure = { userGroups = emptyList() },
            )
        }
    }

    LaunchedEffect(userUid) {
        loadUserGroups()
    }

    LaunchedEffect(selectedGroupId) {
        selectedGroupId?.let(::loadMembers)
    }

    val selectedGroup = userGroups.firstOrNull { it.id == selectedGroupId }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (selectedGroupId == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Meine Gruppen", style = MaterialTheme.typography.headlineMedium)
                    FloatingActionButton(
                        onClick = { showCreateJoinOptions = !showCreateJoinOptions },
                    ) {
                        Text("+")
                    }
                }

                if (showCreateJoinOptions) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("Gruppe erstellen", style = MaterialTheme.typography.headlineSmall)
                            OutlinedTextField(
                                value = groupName,
                                onValueChange = { groupName = it },
                                label = { Text("Gruppenname") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                onClick = {
                                    if (groupName.isBlank()) {
                                        errorText = "Bitte einen Gruppennamen eingeben."
                                        return@Button
                                    }
                                    loading = true
                                    errorText = null
                                    CoroutineScope(Dispatchers.Main).launch {
                                        val result = withContext(Dispatchers.IO) {
                                            groupRepository.createGroup(groupName)
                                        }
                                        result.fold(
                                            onSuccess = {
                                                loading = false
                                                groupName = ""
                                                showCreateJoinOptions = false
                                                selectedGroupId = it.id
                                                loadMembers(it.id)
                                                loadUserGroups()
                                            },
                                            onFailure = {
                                                loading = false
                                                errorText = it.message ?: "Gruppe konnte nicht erstellt werden."
                                            },
                                        )
                                    }
                                },
                                enabled = !loading,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (loading) "Bitte warten..." else "Gruppe erstellen")
                            }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("Gruppe beitreten", style = MaterialTheme.typography.headlineSmall)
                            OutlinedTextField(
                                value = groupId,
                                onValueChange = { groupId = it },
                                label = { Text("Group ID") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                onClick = {
                                    if (groupId.isBlank()) {
                                        errorText = "Bitte eine Group ID eingeben."
                                        return@Button
                                    }
                                    loading = true
                                    errorText = null
                                    CoroutineScope(Dispatchers.Main).launch {
                                        val result = withContext(Dispatchers.IO) {
                                            groupRepository.joinGroupById(groupId)
                                        }
                                        result.fold(
                                            onSuccess = {
                                                loading = false
                                                groupId = ""
                                                showCreateJoinOptions = false
                                                selectedGroupId = it.id
                                                loadMembers(it.id)
                                                loadUserGroups()
                                            },
                                            onFailure = {
                                                loading = false
                                                errorText = it.message ?: "Beitritt fehlgeschlagen."
                                            },
                                        )
                                    }
                                },
                                enabled = !loading,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (loading) "Bitte warten..." else "Gruppe beitreten")
                            }
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (userGroups.isEmpty()) {
                            Text("Noch keine Gruppen gefunden.")
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                items(userGroups) { group ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                CoroutineScope(Dispatchers.Main).launch {
                                                    groupRepository.selectGroup(group.id).fold(
                                                        onSuccess = {
                                                            selectedGroupId = group.id
                                                            loadMembers(group.id)
                                                        },
                                                        onFailure = { errorText = it.message ?: "Gruppe konnte nicht geöffnet werden." },
                                                    )
                                                }
                                            },
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                        ) {
                                            Text(group.name, style = MaterialTheme.typography.titleMedium)
                                            Text("ID: ${group.id}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (errorText != null) {
                    Text(text = errorText ?: "", color = MaterialTheme.colorScheme.error)
                }
            }
        } else {
            val detailUiState = PlayersUiState(
                players = members.mapIndexed { index, member ->
                    PlayerUiModel(
                        id = member.uid.hashCode().toLong(),
                        name = member.displayName.ifBlank { "Unbekannt" },
                        address = member.role.name,
                        hostOrder = index + 1,
                    )
                },
                isLoading = false,
            )

            PlayersScreen(
                uiState = detailUiState,
                onAddPlayer = {},
                onEditPlayer = {},
                onMovePlayer = { playerId, direction ->
                    val currentIndex = members.indexOfFirst { it.uid.hashCode().toLong() == playerId }
                    if (currentIndex == -1) return@PlayersScreen

                    val targetIndex = when (direction) {
                        MoveDirection.UP -> currentIndex - 1
                        MoveDirection.DOWN -> currentIndex + 1
                    }

                    if (targetIndex !in members.indices) return@PlayersScreen

                    val reordered = members.toMutableList()
                    val item = reordered.removeAt(currentIndex)
                    reordered.add(targetIndex, item)
                    members = reordered

                    val selectedGroupIdValue = selectedGroupId ?: return@PlayersScreen
                    CoroutineScope(Dispatchers.Main).launch {
                        val result = withContext(Dispatchers.IO) {
                            groupRepository.selectGroup(selectedGroupIdValue).getOrThrow()
                            groupRepository.updateMemberOrder(selectedGroupIdValue, reordered.map { it.uid })
                        }
                        result.fold(
                            onSuccess = {},
                            onFailure = {
                                errorText = it.message ?: "Reihenfolge konnte nicht gespeichert werden."
                            },
                        )
                    }
                },
                onCreateNextGameNight = {
                    val preparedHost = selectedGameNightHostUid ?: members.firstOrNull()?.uid
                    if (preparedHost.isNullOrBlank()) {
                        errorText = "Bitte erst ein Mitglied für den Spielabend auswählen."
                        return@PlayersScreen
                    }
                    showGameNightDialog = true
                },
                onNameChange = {},
                onAddressChange = {},
                onSavePlayer = {},
                onDismissEditor = {},
                onDismissMessage = {},
                screenTitle = selectedGroup?.name ?: "Gruppe",
                groupId = selectedGroupId,
                onBack = {
                    selectedGroupId = null
                    members = emptyList()
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    if (showGameNightDialog && selectedGroupId != null && members.isNotEmpty()) {
        GameNightPlanningDialog(
            members = members,
            selectedHostUid = selectedGameNightHostUid ?: members.first().uid,
            selectedDate = plannedGameNightDate,
            onHostSelected = { selectedGameNightHostUid = it },
            onDateSelected = { plannedGameNightDate = it },
            onConfirm = { hostUid, date ->
                val resolvedHostUid = hostUid ?: members.firstOrNull()?.uid
                if (resolvedHostUid.isNullOrBlank()) {
                    errorText = "Bitte einen Gastgeber auswählen."
                    return@GameNightPlanningDialog
                }
                showGameNightDialog = false
                loading = true
                errorText = null
                CoroutineScope(Dispatchers.Main).launch {
                    val result = withContext(Dispatchers.IO) {
                        selectedGroupId?.let { groupRepository.selectGroup(it).getOrThrow() }
                        gameNightRepository.createNextGameNight(
                            startsAt = date.atTime(19, 0),
                            preferredHostUid = resolvedHostUid,
                            memberOrderOverride = rotateHostList(members.map { it.uid }, resolvedHostUid),
                        )
                    }
                    result.fold(
                        onSuccess = {
                            loading = false
                            selectedGroupId = null
                            selectedGameNightHostUid = null
                            plannedGameNightDate = LocalDate.now().plusWeeks(2)
                            members = emptyList()
                            onGroupReady()
                        },
                        onFailure = {
                            loading = false
                            errorText = it.message ?: "Spieleabend konnte nicht geplant werden."
                        },
                    )
                }
            },
            onDismiss = {
                showGameNightDialog = false
            },
        )
    }
}

private fun rotateHostList(memberUids: List<String>, preferredUid: String): List<String> {
    if (preferredUid.isBlank() || memberUids.isEmpty()) return memberUids
    val others = memberUids.filter { it != preferredUid }
    return listOf(preferredUid) + others
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameNightPlanningDialog(
    members: List<GroupMember>,
    selectedHostUid: String,
    selectedDate: LocalDate,
    onHostSelected: (String) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onConfirm: (String?, LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = datePickerState.selectedDateMillis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        } ?: selectedDate
                        onDateSelected(picked)
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

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Spieleabend planen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Wähle das Datum und den bevorzugten Gastgeber für den nächsten Abend.")

                OutlinedTextField(
                    value = selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")),
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

                Text("Gastgeber", style = MaterialTheme.typography.titleSmall)
                val selectedMember = members.sortedBy { it.hostOrder }.firstOrNull { it.uid == selectedHostUid }
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                ) {
                    OutlinedTextField(
                        value = selectedMember?.displayName?.ifBlank { "Unbekannt" } ?: "Bitte wählen",
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
                        members.sortedBy { it.hostOrder }.forEach { member ->
                            DropdownMenuItem(
                                text = { Text(member.displayName.ifBlank { "Unbekannt" }) },
                                onClick = {
                                    onHostSelected(member.uid)
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedHostUid, selectedDate) }) {
                Text("Bestätigen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        },
    )
}
