package com.example.boardgamerapp.data.group

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class GroupRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private suspend fun resolveDisplayName(uid: String, fallback: String = "User"): String {
        val profileSnapshot = firestore.collection("users").document(uid).get().await()
        val profileName = profileSnapshot.getString("displayName")
        if (!profileName.isNullOrBlank() && profileName != "User") {
            return profileName
        }

        val email = profileSnapshot.getString("email")
        val emailName = email?.substringBefore('@')?.trim()
        if (!emailName.isNullOrBlank()) {
            return emailName
        }

        val firebaseUser = auth.currentUser
        if (firebaseUser?.uid == uid) {
            val authName = firebaseUser.displayName
            if (!authName.isNullOrBlank() && authName != "User") {
                return authName
            }
        }

        return fallback
    }

    suspend fun createGroup(name: String): Result<Group> = runCatching {
        val currentUser = auth.currentUser ?: error("User not authenticated.")
        val profileSnapshot = firestore.collection("users").document(currentUser.uid).get().await()
        val currentUserAddress = profileSnapshot.getString("address")?.trim().orEmpty()
        val groupId = firestore.collection("groups").document().id
        val group = Group(
            id = groupId,
            name = name.trim(),
            createdBy = currentUser.uid,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now(),
            memberOrder = listOf(currentUser.uid),
        )
        val groupRef = firestore.collection("groups").document(groupId)
        groupRef.set(group.toFirestoreMap(), SetOptions.merge()).await()

        val currentUserDisplayName = resolveDisplayName(currentUser.uid)
        val member = GroupMember(
            uid = currentUser.uid,
            displayName = currentUserDisplayName,
            address = currentUserAddress,
            role = GroupRole.HOST,
            joinedAt = Timestamp.now(),
            hostOrder = 0,
        )
        groupRef.collection("members").document(currentUser.uid).set(member.toFirestoreMap()).await()

        firestore.collection("users")
            .document(currentUser.uid)
            .collection("groups")
            .document(groupId)
            .set(mapOf("groupId" to groupId, "joinedAt" to Timestamp.now(), "role" to GroupRole.HOST.name))
            .await()

        firestore.collection("users").document(currentUser.uid)
            .update("activeGroupId", groupId, "updatedAt", Timestamp.now()).await()

        group
    }

    suspend fun joinGroupById(groupId: String): Result<Group> = runCatching {
        val currentUser = auth.currentUser ?: error("User not authenticated.")
        val groupRef = firestore.collection("groups").document(groupId)
        val snapshot = groupRef.get().await()
        if (!snapshot.exists()) {
            error("Group not found.")
        }

        val memberDoc = groupRef.collection("members").document(currentUser.uid)
        val existingMember = memberDoc.get().await()
        if (existingMember.exists()) {
            error("Du bist bereits Mitglied dieser Gruppe.")
        }

        val displayName = resolveDisplayName(currentUser.uid)
        val profileSnapshot = firestore.collection("users").document(currentUser.uid).get().await()
        val address = profileSnapshot.getString("address")?.trim().orEmpty()
        val currentOrder = (snapshot.get("memberOrder") as? List<*>)
            ?.mapNotNull { it as? String }
            ?: emptyList()
        val nextOrder = currentOrder.size
        val member = GroupMember(
            uid = currentUser.uid,
            displayName = displayName,
            address = address,
            role = GroupRole.MEMBER,
            joinedAt = Timestamp.now(),
            hostOrder = nextOrder,
        )
        memberDoc.set(member.toFirestoreMap()).await()
        val updatedOrder = currentOrder + currentUser.uid
        groupRef.update("memberOrder", updatedOrder, "updatedAt", Timestamp.now()).await()

        firestore.collection("users")
            .document(currentUser.uid)
            .collection("groups")
            .document(groupId)
            .set(mapOf("groupId" to groupId, "joinedAt" to Timestamp.now(), "role" to GroupRole.MEMBER.name))
            .await()

        firestore.collection("users").document(currentUser.uid)
            .update("activeGroupId", groupId, "updatedAt", Timestamp.now()).await()

        val data = snapshot.data ?: error("Group data missing.")
        Group.fromMap(data, groupId)
    }

    suspend fun getGroupById(groupId: String): Result<Group> = runCatching {
        val snapshot = firestore.collection("groups").document(groupId).get().await()
        val data = snapshot.data ?: error("Group not found.")
        Group.fromMap(data, groupId)
    }

    suspend fun selectGroup(groupId: String): Result<Unit> = runCatching {
        val currentUser = auth.currentUser ?: error("User not authenticated.")
        val membership = firestore.collection("groups").document(groupId)
            .collection("members").document(currentUser.uid).get().await()
        require(membership.exists()) { "Du bist kein Mitglied dieser Gruppe." }
        firestore.collection("users").document(currentUser.uid)
            .update("activeGroupId", groupId, "updatedAt", Timestamp.now()).await()
    }

    suspend fun getGroupsForCurrentUser(): Result<List<Group>> = runCatching {
        val currentUser = auth.currentUser ?: error("User not authenticated.")
        val matchingGroups = linkedSetOf<String>()

        val userGroupsSnapshot = firestore.collection("users")
            .document(currentUser.uid)
            .collection("groups")
            .get()
            .await()
        for (groupDoc in userGroupsSnapshot.documents) {
            val groupId = groupDoc.id
            if (groupId.isNotBlank()) {
                matchingGroups.add(groupId)
            }
        }

        val groups = mutableListOf<Group>()
        for (groupId in matchingGroups) {
            val groupSnapshot = firestore.collection("groups").document(groupId).get().await()
            if (groupSnapshot.exists()) {
                val data = groupSnapshot.data ?: continue
                groups.add(Group.fromMap(data, groupId))
            }
        }

        groups
    }

    suspend fun getMembers(groupId: String): Result<List<GroupMember>> = runCatching {
        val groupRef = firestore.collection("groups").document(groupId)
        val groupSnapshot = groupRef.get().await()
        val memberOrder = (groupSnapshot.get("memberOrder") as? List<*>)
            ?.mapNotNull { it as? String }
            ?: emptyList()
        val memberOrderIndex = memberOrder.withIndex().associate { it.value to it.index }

        val snapshots = groupRef.collection("members").get().await()
        snapshots.documents.map { doc ->
            val member = GroupMember.fromMap(doc.data ?: emptyMap())
            val uid = member.uid
            if (uid.isBlank()) {
                return@map member
            }

            val resolvedName = if (member.displayName.isNotBlank()) {
                member.displayName
            } else if (auth.currentUser?.uid == uid) {
                resolveDisplayName(uid, "User")
            } else {
                "User"
            }

            member.copy(displayName = resolvedName)
        }.sortedWith(compareBy<GroupMember> { memberOrderIndex[it.uid] ?: Int.MAX_VALUE }.thenBy { it.hostOrder })
    }

    suspend fun updateMemberOrder(groupId: String, orderedMemberUids: List<String>): Result<Unit> = runCatching {
        firestore.collection("groups")
            .document(groupId)
            .update("memberOrder", orderedMemberUids, "updatedAt", Timestamp.now())
            .await()
    }

    suspend fun isUserMemberOfGroup(groupId: String): Result<Boolean> = runCatching {
        val currentUser = auth.currentUser ?: error("User not authenticated.")
        val memberSnapshot = firestore.collection("groups")
            .document(groupId)
            .collection("members")
            .document(currentUser.uid)
            .get()
            .await()
        memberSnapshot.exists()
    }

    suspend fun isUserHostOfGroup(groupId: String): Result<Boolean> = runCatching {
        val currentUser = auth.currentUser ?: error("User not authenticated.")
        val memberSnapshot = firestore.collection("groups")
            .document(groupId)
            .collection("members")
            .document(currentUser.uid)
            .get()
            .await()

        if (!memberSnapshot.exists()) {
            return@runCatching false
        }

        val role = memberSnapshot.getString("role")
        role == GroupRole.HOST.name
    }

    suspend fun leaveGroup(groupId: String): Result<Unit> = runCatching {
        val currentUser = auth.currentUser ?: error("User not authenticated.")

        firestore.collection("groups")
            .document(groupId)
            .collection("members")
            .document(currentUser.uid)
            .delete()
            .await()

        firestore.collection("users")
            .document(currentUser.uid)
            .collection("groups")
            .document(groupId)
            .delete()
            .await()
    }
}
