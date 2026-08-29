package com.example.boardgamerapp.data.auth

import com.google.firebase.Timestamp

data class UserProfile(
    val uid: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "email" to email,
        "displayName" to displayName,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
    )

    companion object {
        fun fromMap(data: Map<String, Any?>): UserProfile = UserProfile(
            uid = data["uid"] as? String ?: "",
            email = data["email"] as? String,
            displayName = data["displayName"] as? String,
            createdAt = data["createdAt"] as? Timestamp,
            updatedAt = data["updatedAt"] as? Timestamp,
        )
    }
}
