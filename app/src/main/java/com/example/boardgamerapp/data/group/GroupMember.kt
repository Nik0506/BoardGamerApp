package com.example.boardgamerapp.data.group

import com.google.firebase.Timestamp

enum class GroupRole {
    HOST,
    MEMBER,
}

data class GroupMember(
    val uid: String = "",
    val displayName: String = "",
    val address: String = "",
    val role: GroupRole = GroupRole.MEMBER,
    val joinedAt: Timestamp? = null,
    val hostOrder: Int = 0,
) {
    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "displayName" to displayName,
        "address" to address,
        "role" to role.name,
        "joinedAt" to joinedAt,
        "hostOrder" to hostOrder,
    )

    companion object {
        fun fromMap(data: Map<String, Any?>): GroupMember = GroupMember(
            uid = data["uid"] as? String ?: "",
            displayName = data["displayName"] as? String ?: "",
            address = data["address"] as? String ?: "",
            role = GroupRole.valueOf((data["role"] as? String) ?: GroupRole.MEMBER.name),
            joinedAt = data["joinedAt"] as? Timestamp,
            hostOrder = (data["hostOrder"] as? Number)?.toInt() ?: 0,
        )
    }
}
