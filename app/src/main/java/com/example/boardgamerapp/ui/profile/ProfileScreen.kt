package com.example.boardgamerapp.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    var displayName by rememberSaveable(currentUser?.uid) { mutableStateOf(currentUser?.displayName ?: "") }
    var address by rememberSaveable(currentUser?.uid) { mutableStateOf("") }

    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid ?: return@LaunchedEffect
        val profileDoc = withContext(Dispatchers.IO) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .await()
        }
        val profileName = profileDoc.getString("displayName")
        val profileAddress = profileDoc.getString("address")
        displayName = profileName?.ifBlank { "Kein Name gesetzt" } ?: currentUser.displayName ?: "Kein Name gesetzt"
        address = profileAddress?.ifBlank { "Keine Adresse hinterlegt" } ?: "Keine Adresse hinterlegt"
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Profil",
                style = MaterialTheme.typography.headlineMedium,
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Name")
                    Text(
                        text = displayName.ifBlank { "Kein Name gesetzt" },
                        style = MaterialTheme.typography.titleLarge,
                    )

                    Text("Adresse")
                    Text(
                        text = address,
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Text("E-Mail")
                    Text(
                        text = currentUser?.email ?: "Keine E-Mail verfügbar",
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Text("Benutzer-ID")
                    Text(
                        text = currentUser?.uid ?: "--",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Button(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    onSignOut()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Abmelden")
            }
        }
    }
}
