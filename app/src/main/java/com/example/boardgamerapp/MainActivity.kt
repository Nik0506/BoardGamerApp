package com.example.boardgamerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.boardgamerapp.ui.BoardGamerApp
import com.example.boardgamerapp.ui.theme.BoardGamerAppTheme
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.android.gms.tasks.Tasks

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deleteDatabase("boardgamer.db")
        val firestore = FirebaseFirestore.getInstance()
        runCatching { Tasks.await(firestore.clearPersistence()) }
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
            .build()
        enableEdgeToEdge()
        setContent {
            BoardGamerAppTheme {
                BoardGamerApp()
            }
        }
    }
}
