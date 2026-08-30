package com.example.boardgamerapp.data.group

import com.google.firebase.Timestamp

data class Group(
    val id: String = "",
    val name: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val memberOrder: List<String> = emptyList(),
) {
    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "createdBy" to createdBy,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "memberOrder" to memberOrder,
    )

    companion object {
        fun fromMap(data: Map<String, Any?>, id: String): Group = Group(
            id = id,
            name = data["name"] as? String ?: "",
            createdBy = data["createdBy"] as? String ?: "",
            createdAt = data["createdAt"] as? Timestamp,
            updatedAt = data["updatedAt"] as? Timestamp,
            memberOrder = (data["memberOrder"] as? List<*>)
                ?.mapNotNull { it as? String }
                ?: emptyList(),
        )
    }
}
