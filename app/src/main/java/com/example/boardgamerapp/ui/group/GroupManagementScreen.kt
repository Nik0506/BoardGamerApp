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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.boardgamerapp.data.group.Group
import com.example.boardgamerapp.data.group.GroupMember
import com.example.boardgamerapp.data.group.GroupRepository
import com.example.boardgamerapp.data.repository.FirebaseGameNightRepository
import com.example.boardgamerapp.data.repository.MoveDirection
import com.example.boardgamerapp.data.repository.RoomGameNightRepository
import com.example.boardgamerapp.ui.players.PlayerUiModel
import com.example.boardgamerapp.ui.players.PlayersScreen
import com.example.boardgamerapp.ui.players.PlayersUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GroupManagementScreen(
    onGroupReady: () -> Unit,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val groupRepository = remember { GroupRepository() }
    val localGameNightRepository = remember(context) { RoomGameNightRepository.create(context) }
    val gameNightRepository = remember(localGameNightRepository) { FirebaseGameNightRepository(localGameNightRepository) }
    var groupName by remember { mutableStateOf("") }
    var groupId by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var userGroups by remember { mutableStateOf<List<Group>>(emptyList()) }
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    var members by remember { mutableStateOf<List<GroupMember>>(emptyList()) }
    var showCreateJoinOptions by remember { mutableStateOf(false) }

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

    LaunchedEffect(Unit) {
        loadUserGroups()
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
                                                selectedGroupId = group.id
                                                loadMembers(group.id)
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
                    loading = true
                    errorText = null
                    CoroutineScope(Dispatchers.Main).launch {
                        val result = withContext(Dispatchers.IO) {
                            gameNightRepository.createNextGameNight()
                        }
                        result.fold(
                            onSuccess = {
                                loading = false
                                selectedGroupId = null
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
}
