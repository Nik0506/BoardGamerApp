package com.example.boardgamerapp.data.auth

import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun currentUser(): FirebaseUser? = auth.currentUser

    fun isSignedIn(): Boolean = auth.currentUser != null

    fun signOut() {
        auth.signOut()
    }

    suspend fun register(
        email: String,
        password: String,
        displayName: String,
        address: String = "",
    ): Result<FirebaseUser> = runCatching {
        val task = auth.createUserWithEmailAndPassword(email.trim(), password)
        val result = task.await()
        val user = result.user ?: error("User could not be created.")
        val trimmedName = displayName.trim()
        if (trimmedName.isNotBlank()) {
            val profileUpdate = UserProfileChangeRequest.Builder()
                .setDisplayName(trimmedName)
                .build()
            user.updateProfile(profileUpdate).await()
        }
        createUserProfileIfNeeded(user, email.trim(), trimmedName, address.trim())
        user
    }

    suspend fun login(email: String, password: String): Result<FirebaseUser> = runCatching {
        val task = auth.signInWithEmailAndPassword(email.trim(), password)
        val result = task.await()
        val user = result.user ?: error("Login failed.")
        val storedName = firestore.collection("users").document(user.uid).get().await()
            .getString("displayName")
        val storedAddress = firestore.collection("users").document(user.uid).get().await()
            .getString("address")
        val resolvedDisplayName = (storedName ?: user.displayName ?: "User").trim()
        createUserProfileIfNeeded(user, email.trim(), resolvedDisplayName, storedAddress ?: "")
        user
    }

    suspend fun createUserProfileIfNeeded(
        user: FirebaseUser,
        email: String?,
        displayName: String,
        address: String = "",
    ) {
        val docRef = firestore.collection("users").document(user.uid)
        val snapshot = docRef.get().await()

        if (!snapshot.exists()) {
            val profile = UserProfile(
                uid = user.uid,
                email = email ?: user.email,
                displayName = displayName.ifBlank { user.displayName ?: "User" },
                address = address.ifBlank { "" },
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now(),
            )
            docRef.set(profile.toFirestoreMap()).await()
        } else {
            val updateMap = mapOf(
                "email" to (email ?: user.email),
                "displayName" to displayName.ifBlank { user.displayName ?: "User" },
                "address" to address,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.update(updateMap).await()
        }
    }

    suspend fun getUserProfile(uid: String): Result<UserProfile> = runCatching {
        val snapshot = firestore.collection("users").document(uid).get().await()
        val data = snapshot.data ?: error("User profile not found.")
        UserProfile.fromMap(data)
    }
}
