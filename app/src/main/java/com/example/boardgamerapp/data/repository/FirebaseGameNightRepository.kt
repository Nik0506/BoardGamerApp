package com.example.boardgamerapp.data.repository

import com.example.boardgamerapp.data.group.GroupMember
import com.example.boardgamerapp.domain.model.AttendanceStatusType
import com.example.boardgamerapp.domain.model.GameNightAttendance
import com.example.boardgamerapp.domain.model.BoardGame
import com.example.boardgamerapp.domain.model.FoodCategory
import com.example.boardgamerapp.domain.model.FoodOrder
import com.example.boardgamerapp.domain.model.FoodVote
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.LateNotice
import com.example.boardgamerapp.domain.model.Player
import com.example.boardgamerapp.domain.model.Restaurant
import com.example.boardgamerapp.domain.model.Review
import com.example.boardgamerapp.domain.model.Vote
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class FirebaseGameNightRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : BoardGamerRepository {

    private suspend fun currentGameNightDocId(groupId: String): String? {
        val snapshot = firestore.collection("groups")
            .document(groupId)
            .collection("gameNights")
            .orderBy("startsAt", Query.Direction.ASCENDING)
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.id
    }

    private suspend fun resolveHostAddress(groupId: String, uid: String): String {
        val memberSnapshot = firestore.collection("groups").document(groupId).collection("members").document(uid).get().await()
        if (memberSnapshot.exists()) {
            val member = GroupMember.fromMap(memberSnapshot.data ?: emptyMap())
            if (member.address.isNotBlank()) return member.address
        }

        val userSnapshot = firestore.collection("users").document(uid).get().await()
        val address = userSnapshot.getString("address")?.trim()
        return address?.takeIf { it.isNotBlank() } ?: "Keine Adresse hinterlegt"
    }

    override suspend fun getUpcomingGameNight(): Result<UpcomingGameNight?> = withContext(Dispatchers.IO) {
        runCatching {
            val groupId = currentGroupId() ?: return@runCatching null
            val snapshot = firestore.collection("groups")
                .document(groupId)
                .collection("gameNights")
                .orderBy("startsAt", Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .await()

            val doc = snapshot.documents.firstOrNull() ?: return@runCatching null
            val gameNight = doc.toGameNight() ?: return@runCatching null
            val fallbackHost = Player(id = 0L, name = "Gastgeber", address = "", hostOrder = 0)
            val host = doc.getString("hostUid")?.let { uid ->
                val memberSnapshot = firestore.collection("groups").document(groupId).collection("members").document(uid).get().await()
                val member = if (memberSnapshot.exists()) GroupMember.fromMap(memberSnapshot.data ?: emptyMap()) else null
                val name = member?.displayName?.takeIf { it.isNotBlank() } ?: uid
                val address = member?.address?.takeIf { it.isNotBlank() } ?: resolveHostAddress(groupId, uid)
                Player(
                    id = uid.hashCode().toLong(),
                    name = name,
                    address = address,
                    hostOrder = member?.hostOrder ?: 0,
                )
            } ?: fallbackHost

            UpcomingGameNight(gameNight = gameNight.copy(location = host.address.ifBlank { gameNight.location }), host = host)
        }
    }

    override suspend fun createNextGameNight(
        startsAt: LocalDateTime?,
        preferredHostUid: String?,
        memberOrderOverride: List<String>?,
    ): Result<UpcomingGameNight> = withContext(Dispatchers.IO) {
        runCatching {
            val groupId = currentGroupId() ?: error("Du bist in keiner Gruppe angemeldet.")
            val groupRef = firestore.collection("groups").document(groupId)
            val membersSnapshot = groupRef.collection("members").get().await()
            val currentGroupMembers = membersSnapshot.documents.mapNotNull { doc ->
                runCatching { GroupMember.fromMap(doc.data ?: emptyMap()) }.getOrNull()
            }
            val defaultOrder = currentGroupMembers.map { it.uid }
            val savedOrder: List<String> = memberOrderOverride
                ?: ((groupRef.get().await().get("memberOrder") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?: defaultOrder)
            val orderedMembers: List<GroupMember> = when {
                preferredHostUid.isNullOrBlank() -> savedOrder.mapNotNull { uid ->
                    currentGroupMembers.firstOrNull { it.uid == uid }
                }
                savedOrder.contains(preferredHostUid) -> {
                    val preferredMember = currentGroupMembers.firstOrNull { it.uid == preferredHostUid }
                    val rest = savedOrder.filter { it != preferredHostUid }
                        .mapNotNull { uid -> currentGroupMembers.firstOrNull { it.uid == uid } }
                    listOfNotNull(preferredMember) + rest
                }
                else -> currentGroupMembers.firstOrNull { it.uid == preferredHostUid }?.let { preferredMember ->
                    listOf(preferredMember) + currentGroupMembers.filter { it.uid != preferredHostUid }
                } ?: currentGroupMembers
            }.ifEmpty { currentGroupMembers }
            val nextHost = orderedMembers.firstOrNull() ?: error("In der Gruppe gibt es noch keine Mitglieder.")

            val finalOrder = orderedMembers.map { it.uid }
            if (finalOrder != (savedOrder.takeIf { it.isNotEmpty() } ?: defaultOrder)) {
                groupRef.update("memberOrder", finalOrder, "updatedAt", Timestamp.now()).await()
            }

            val existingNight = groupRef.collection("gameNights")
                .orderBy("startsAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()

            val resolvedDate = startsAt ?: existingNight?.let {
                val ts = it.getTimestamp("startsAt") ?: Timestamp.now()
                ts.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().plusWeeks(2)
            } ?: LocalDateTime.now().plusWeeks(2).withHour(19).withMinute(0).withSecond(0).withNano(0)

            val hostAddress = resolveHostAddress(groupId, nextHost.uid)
            val location = hostAddress.ifBlank { "Keine Adresse hinterlegt" }
            val saved = mapOf(
                "groupId" to groupId,
                "id" to 0L,
                "hostUid" to nextHost.uid,
                "location" to location,
                "status" to GameNightStatus.PLANNED.name,
                "startsAt" to Timestamp(Date.from(resolvedDate.atZone(ZoneId.systemDefault()).toInstant())),
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now(),
            )

            val newDoc = groupRef.collection("gameNights").document()
            newDoc.set(saved).await()
            val stableId = newDoc.id.hashCode().toLong()
            newDoc.update("id", stableId).await()

            UpcomingGameNight(
                gameNight = GameNight(
                    id = stableId,
                    startsAt = resolvedDate,
                    hostId = nextHost.uid.hashCode().toLong(),
                    location = location,
                    status = GameNightStatus.PLANNED,
                ),
                host = Player(
                    id = nextHost.uid.hashCode().toLong(),
                    name = nextHost.displayName.ifBlank { "Host" },
                    address = location,
                    hostOrder = nextHost.hostOrder,
                ),
            )
        }
    }

    override suspend fun updateGameNight(
        gameNightId: Long,
        startsAt: LocalDateTime,
        hostPlayerId: Long,
    ): Result<UpcomingGameNight> = withContext(Dispatchers.IO) {
        runCatching {
            val groupId = currentGroupId() ?: error("Du bist in keiner Gruppe angemeldet.")
            val groupRef = firestore.collection("groups").document(groupId)
            val gameNightDoc = currentGameNightDocument(groupId, gameNightId)
                ?: error("Der Spieleabend wurde nicht gefunden.")

            val membersSnapshot = groupRef.collection("members").get().await()
            val currentGroupMembers = membersSnapshot.documents.mapNotNull { doc ->
                runCatching { GroupMember.fromMap(doc.data ?: emptyMap()) }.getOrNull()
            }
            val targetMember = currentGroupMembers.firstOrNull { it.uid.hashCode().toLong() == hostPlayerId }
                ?: error("Der ausgewählte Gastgeber wurde in der Gruppe nicht gefunden.")

            val hostAddress = resolveHostAddress(groupId, targetMember.uid)
            val location = hostAddress.ifBlank { "Keine Adresse hinterlegt" }

            val updateData = mapOf(
                "startsAt" to Timestamp(Date.from(startsAt.atZone(ZoneId.systemDefault()).toInstant())),
                "hostUid" to targetMember.uid,
                "location" to location,
                "updatedAt" to Timestamp.now(),
            )
            gameNightDoc.reference.update(updateData).await()

            val savedOrder = ((groupRef.get().await().get("memberOrder") as? List<*>)
                ?.mapNotNull { it as? String }
                ?: currentGroupMembers.map { it.uid })
            val rotatedOrder = rotateHostList(savedOrder, targetMember.uid)
            if (rotatedOrder != savedOrder) {
                groupRef.update("memberOrder", rotatedOrder, "updatedAt", Timestamp.now()).await()
            }

            val stableId = gameNightDoc.getLong("id") ?: gameNightDoc.id.hashCode().toLong()
            val updatedGameNight = GameNight(
                id = stableId,
                startsAt = startsAt,
                hostId = targetMember.uid.hashCode().toLong(),
                location = location,
                status = GameNightStatus.valueOf(gameNightDoc.getString("status") ?: GameNightStatus.PLANNED.name),
            )
            val hostPlayer = Player(
                id = targetMember.uid.hashCode().toLong(),
                name = targetMember.displayName.ifBlank { "Gastgeber" },
                address = location,
                hostOrder = targetMember.hostOrder,
            )
            UpcomingGameNight(gameNight = updatedGameNight, host = hostPlayer)
        }
    }

    private fun rotateHostList(memberUids: List<String>, preferredUid: String): List<String> {
        if (preferredUid.isBlank() || memberUids.isEmpty()) return memberUids
        val others = memberUids.filter { it != preferredUid }
        return listOf(preferredUid) + others
    }

    override suspend fun getGameSuggestions(): Result<GameNightSuggestions?> = withContext(Dispatchers.IO) {
        runCatching {
            val groupId = currentGroupId() ?: return@runCatching null
            val night = getUpcomingGameNight().getOrNull() ?: return@runCatching null
            val gameNightDocId = currentGameNightDocId(groupId) ?: return@runCatching null
            val suggestionsSnapshot = firestore.collection("groups")
                .document(groupId)
                .collection("gameNights")
                .document(gameNightDocId)
                .collection("suggestions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val playersById = currentGroupPlayers().associateBy { it.id }
            val suggestions = suggestionsSnapshot.documents.mapNotNull { doc ->
                val suggestionId = doc.getLong("id") ?: doc.id.hashCode().toLong()
                val name = doc.getString("name") ?: return@mapNotNull null
                val description = doc.getString("description") ?: ""
                val playerId = doc.getLong("suggestedByPlayerId") ?: return@mapNotNull null
                BoardGameSuggestion(
                    boardGame = BoardGame(
                        id = suggestionId,
                        name = name,
                        description = description,
                        suggestedByPlayerId = playerId,
                        gameNightId = night.gameNight.id,
                    ),
                    suggestedBy = playersById[playerId] ?: Player(playerId, "Unbekannt", "", 0),
                )
            }

            GameNightSuggestions(gameNight = night.gameNight, suggestions = suggestions)
        }
    }

    override suspend fun getAttendances(): Result<List<GameNightAttendance>> = withContext(Dispatchers.IO) {
        runCatching {
            val groupId = currentGroupId() ?: return@runCatching emptyList()
            val night = getUpcomingGameNight().getOrNull() ?: return@runCatching emptyList()
            val gameNightDocId = currentGameNightDocId(groupId) ?: return@runCatching emptyList()
            val snapshot = firestore.collection("groups")
                .document(groupId)
                .collection("gameNights")
                .document(gameNightDocId)
                .collection("attendance")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val playerId = doc.getLong("playerId") ?: return@mapNotNull null
                val statusStr = doc.getString("status") ?: AttendanceStatusType.PENDING.name
                val status = runCatching { AttendanceStatusType.valueOf(statusStr) }.getOrDefault(AttendanceStatusType.PENDING)
                val minutesLate = doc.getLong("minutesLate")?.toInt()
                val reason = doc.getString("reason")
                val createdAt = doc.getTimestamp("createdAt")?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime() ?: LocalDateTime.now()
                val updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime() ?: createdAt
                val id = doc.getLong("id") ?: doc.id.hashCode().toLong()
                GameNightAttendance(
                    id = id,
                    playerId = playerId,
                    gameNightId = night.gameNight.id,
                    status = status,
                    minutesLate = minutesLate,
                    reason = reason,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                )
            }
        }
    }

    override suspend fun setAttendance(
        playerId: Long,
        status: AttendanceStatusType,
        minutesLate: Int?,
        reason: String?,
    ): Result<GameNightAttendance> = withContext(Dispatchers.IO) {
        runCatching {
            requireCurrentPlayer(playerId)
            val groupId = currentGroupId() ?: error("Du bist in keiner Gruppe angemeldet.")
            val night = getUpcomingGameNight().getOrNull() ?: error("Es gibt noch keinen nächsten Spieleabend.")
            val gameNightDocId = currentGameNightDocId(groupId) ?: error("Es gibt noch keinen nächsten Spieleabend.")
            val docRef = firestore.collection("groups")
                .document(groupId)
                .collection("gameNights")
                .document(gameNightDocId)
                .collection("attendance")
                .document(playerId.toString())

            val now = LocalDateTime.now()
            val attendanceId = playerId
            val data = mutableMapOf<String, Any>(
                "id" to attendanceId,
                "playerId" to playerId,
                "gameNightId" to night.gameNight.id,
                "status" to status.name,
                "updatedAt" to Timestamp.now(),
            )
            if (minutesLate != null) {
                data["minutesLate"] = minutesLate
            }
            if (!reason.isNullOrBlank()) {
                data["reason"] = reason.trim()
            }

            docRef.set(data).await()

            if (status == AttendanceStatusType.LATE && minutesLate != null) {
                val lateDocRef = firestore.collection("groups")
                    .document(groupId)
                    .collection("gameNights")
                    .document(gameNightDocId)
                    .collection("lateNotices")
                    .document(playerId.toString())
                lateDocRef.set(
                    mapOf(
                        "id" to lateDocRef.id.hashCode().toLong(),
                        "playerId" to playerId,
                        "gameNightId" to night.gameNight.id,
                        "minutes" to minutesLate,
                        "createdAt" to Timestamp.now(),
                    ),
                ).await()
            }

            GameNightAttendance(
                id = attendanceId,
                playerId = playerId,
                gameNightId = night.gameNight.id,
                status = status,
                minutesLate = minutesLate,
                reason = reason?.trim(),
                createdAt = now,
                updatedAt = now,
            )
        }
    }

    override suspend fun getLateNotices(): Result<List<LateNotice>> = withContext(Dispatchers.IO) {
        runCatching {
            val groupId = currentGroupId() ?: return@runCatching emptyList()
            val night = getUpcomingGameNight().getOrNull() ?: return@runCatching emptyList()
            val gameNightDocId = currentGameNightDocId(groupId) ?: return@runCatching emptyList()
            val snapshot = firestore.collection("groups")
                .document(groupId)
                .collection("gameNights")
                .document(gameNightDocId)
                .collection("lateNotices")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val createdAt = doc.getTimestamp("createdAt")?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime()
                val playerId = doc.getLong("playerId") ?: return@mapNotNull null
                val minutes = doc.getLong("minutes")?.toInt() ?: return@mapNotNull null
                val noticeId = doc.getLong("id") ?: doc.id.hashCode().toLong()
                LateNotice(
                    id = noticeId,
                    playerId = playerId,
                    gameNightId = night.gameNight.id,
                    minutes = minutes,
                    createdAt = createdAt ?: LocalDateTime.now(),
                )
            }
        }
    }

    override suspend fun addLateNotice(playerId: Long, minutes: Int): Result<LateNotice> = withContext(Dispatchers.IO) {
        runCatching {
            requireCurrentPlayer(playerId)
            val groupId = currentGroupId() ?: error("Du bist in keiner Gruppe angemeldet.")
            val night = getUpcomingGameNight().getOrNull() ?: error("Es gibt noch keinen nächsten Spieleabend.")
            val gameNightDocId = currentGameNightDocId(groupId) ?: error("Es gibt noch keinen nächsten Spieleabend.")
            val docRef = firestore.collection("groups")
                .document(groupId)
                .collection("gameNights")
                .document(gameNightDocId)
                .collection("lateNotices")
                .document()
            val notice = LateNotice(
                id = docRef.id.hashCode().toLong(),
                playerId = playerId,
                gameNightId = night.gameNight.id,
                minutes = minutes,
                createdAt = LocalDateTime.now(),
            )
            docRef.set(
                mapOf(
                    "id" to notice.id,
                    "playerId" to playerId,
                    "gameNightId" to night.gameNight.id,
                    "minutes" to minutes,
                    "createdAt" to Timestamp.now(),
                ),
            ).await()

            // Update attendance record as well
            setAttendance(playerId, AttendanceStatusType.LATE, minutes, null)

            notice
        }
    }

    suspend fun saveFcmToken(token: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = auth.currentUser?.uid ?: error("Nicht angemeldet")
            firestore.collection("users").document(uid).update("fcmToken", token, "tokenUpdatedAt", Timestamp.now()).await()
            Unit
        }
    }

    override suspend fun getPlayers(): Result<List<Player>> = withContext(Dispatchers.IO) {
        runCatching {
            val groupId = currentGroupId() ?: return@runCatching emptyList()
            val membersSnapshot = firestore.collection("groups").document(groupId).collection("members").get().await()
            membersSnapshot.documents.mapNotNull { doc ->
                val member = runCatching { GroupMember.fromMap(doc.data ?: emptyMap()) }.getOrNull() ?: return@mapNotNull null
                Player(
                    id = member.uid.hashCode().toLong(),
                    name = member.displayName.ifBlank { "Unbekannt" },
                    address = member.address.ifBlank { "Keine Adresse hinterlegt" },
                    hostOrder = member.hostOrder,
                )
            }.sortedBy { it.hostOrder }
        }
    }

    override suspend fun addGameSuggestion(
        name: String,
        description: String,
        suggestedByPlayerId: Long,
    ): Result<BoardGameSuggestion> = withContext(Dispatchers.IO) {
        runCatching {
            requireCurrentPlayer(suggestedByPlayerId)
            val groupId = currentGroupId() ?: error("Du bist in keiner Gruppe angemeldet.")
            val night = getUpcomingGameNight().getOrNull() ?: error("Es gibt noch keinen nächsten Spieleabend.")
            val gameNightDocId = currentGameNightDocId(groupId) ?: error("Es gibt noch keinen nächsten Spieleabend.")
            val allPlayers = currentGroupPlayers()
            val player = allPlayers.firstOrNull { it.id == suggestedByPlayerId }
                ?: error("Der Spieler wurde nicht in der Gruppe gefunden.")
            val suggestionId = System.currentTimeMillis()
            val docRef = firestore.collection("groups")
                .document(groupId)
                .collection("gameNights")
                .document(gameNightDocId)
                .collection("suggestions")
                .document()
            docRef.set(
                mapOf(
                    "id" to suggestionId,
                    "name" to name.trim(),
                    "description" to description.trim(),
                    "suggestedByPlayerId" to suggestedByPlayerId,
                    "gameNightId" to night.gameNight.id,
                    "createdAt" to Timestamp.now(),
                ),
            ).await()
            BoardGameSuggestion(
                boardGame = BoardGame(
                    id = suggestionId,
                    name = name.trim(),
                    description = description.trim(),
                    suggestedByPlayerId = suggestedByPlayerId,
                    gameNightId = night.gameNight.id,
                ),
                suggestedBy = player,
            )
        }
    }

    override suspend fun deleteGameSuggestion(boardGameId: Long, requestingPlayerId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireCurrentPlayer(requestingPlayerId)
            val groupId = currentGroupId() ?: error("Du bist in keiner Gruppe angemeldet.")
            val night = getUpcomingGameNight().getOrNull() ?: error("Es gibt noch keinen nächsten Spieleabend.")
            val gameNightDocId = currentGameNightDocId(groupId) ?: error("Es gibt noch keinen nächsten Spieleabend.")
            val suggestionsRef = firestore.collection("groups")
                .document(groupId)
                .collection("gameNights")
                .document(gameNightDocId)
                .collection("suggestions")
            val docs = suggestionsRef.whereEqualTo("id", boardGameId).limit(1).get().await()
            require(docs.documents.firstOrNull()?.getLong("suggestedByPlayerId") == requestingPlayerId) {
                "Du kannst nur deinen eigenen Spielvorschlag löschen."
            }
            docs.documents.forEach { doc ->
                doc.reference.delete().await()
            }
            val votesRef = firestore.collection("groups")
                .document(groupId)
                .collection("gameNights")
                .document(gameNightDocId)
                .collection("votes")
            val voteDocs = votesRef.whereEqualTo("boardGameId", boardGameId).get().await()
            voteDocs.documents.forEach { doc ->
                doc.reference.delete().await()
            }
        }
    }

    override suspend fun getVotingSnapshot(): Result<VotingSnapshot?> = withContext(Dispatchers.IO) {
        runCatching {
            val suggestionsResult = getGameSuggestions().getOrNull() ?: return@runCatching null
            val groupId = currentGroupId() ?: return@runCatching null
            val night = getUpcomingGameNight().getOrNull() ?: return@runCatching null
            val gameNightDocId = currentGameNightDocId(groupId) ?: return@runCatching null
            val voteSnapshot = firestore.collection("groups")
                .document(groupId)
                .collection("gameNights")
                .document(gameNightDocId)
                .collection("votes")
                .get()
                .await()
            val results = suggestionsResult.suggestions.map { suggestion ->
                val voterIds = voteSnapshot.documents
                    .asSequence()
                    .filter { it.getLong("boardGameId") == suggestion.boardGame.id }
                    .mapNotNull { it.getLong("playerId") }
                    .toSet()
                BoardGameVoteResult(
                    suggestion = suggestion,
                    voterIds = voterIds,
                )
            }
            VotingSnapshot(
                gameNight = suggestionsResult.gameNight,
                results = results,
                playerCount = currentGroupPlayers().size,
            )
        }
    }

    override suspend fun castVote(playerId: Long, boardGameId: Long): Result<Vote> = withContext(Dispatchers.IO) {
        runCatching {
            requireCurrentPlayer(playerId)
            val groupId = currentGroupId() ?: error("Du bist in keiner Gruppe angemeldet.")
            val night = getUpcomingGameNight().getOrNull() ?: error("Es gibt noch keinen nächsten Spieleabend.")
            val gameNightDocId = currentGameNightDocId(groupId) ?: error("Es gibt noch keinen nächsten Spieleabend.")
            val voteRef = firestore.collection("groups")
                .document(groupId)
                .collection("gameNights")
                .document(gameNightDocId)
                .collection("votes")
                .document(playerId.toString())
            voteRef.set(
                mapOf(
                    "playerId" to playerId,
                    "boardGameId" to boardGameId,
                    "gameNightId" to night.gameNight.id,
                    "createdAt" to Timestamp.now(),
                ),
            ).await()
            Vote(
                id = playerId,
                playerId = playerId,
                boardGameId = boardGameId,
                gameNightId = night.gameNight.id,
            )
        }
    }

    override suspend fun addPlayer(name: String, address: String): Result<Player> = Result.failure(
        UnsupportedOperationException("Mitglieder treten der Gruppe mit ihrem eigenen Würfelrunde-Konto bei."),
    )

    override suspend fun updatePlayer(id: Long, name: String, address: String): Result<Player> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = auth.currentUser?.uid ?: error("Du bist nicht angemeldet.")
            require(uid.hashCode().toLong() == id) { "Du kannst nur dein eigenes Profil bearbeiten." }
            val groupId = currentGroupId() ?: error("Du bist in keiner Gruppe angemeldet.")
            val cleanName = name.trim().also { require(it.isNotEmpty()) { "Name darf nicht leer sein." } }
            val cleanAddress = address.trim().also { require(it.isNotEmpty()) { "Adresse darf nicht leer sein." } }
            val values = mapOf("displayName" to cleanName, "address" to cleanAddress, "updatedAt" to Timestamp.now())
            firestore.collection("users").document(uid).update(values).await()
            firestore.collection("groups").document(groupId).collection("members").document(uid).update(values).await()
            currentGroupPlayers().first { it.id == id }
        }
    }

    override suspend fun movePlayer(id: Long, direction: MoveDirection): Result<List<Player>> = withContext(Dispatchers.IO) {
        runCatching {
            val groupId = currentGroupId() ?: error("Du bist in keiner Gruppe angemeldet.")
            val groupRef = firestore.collection("groups").document(groupId)
            val group = groupRef.get().await()
            val order = (group.get("memberOrder") as? List<*>)?.mapNotNull { it as? String }?.toMutableList() ?: mutableListOf()
            val memberDocs = groupRef.collection("members").get().await().documents
            val uid = memberDocs.firstOrNull { it.id.hashCode().toLong() == id }?.id ?: error("Mitglied nicht gefunden.")
            val current = order.indexOf(uid)
            val target = if (direction == MoveDirection.UP) current - 1 else current + 1
            if (current >= 0 && target in order.indices) {
                val moved = order.removeAt(current)
                order.add(target, moved)
                groupRef.update("memberOrder", order, "updatedAt", Timestamp.now()).await()
            }
            currentGroupPlayers()
        }
    }

    override suspend fun getReviewSnapshot(): Result<ReviewSnapshot?> = withContext(Dispatchers.IO) {
        runCatching {
            val groupId = currentGroupId() ?: return@runCatching null
            val gameNightDoc = currentGameNightDocument(groupId) ?: return@runCatching null
            val gameNight = gameNightDoc.toGameNight() ?: return@runCatching null
            val hostUid = gameNightDoc.getString("hostUid") ?: return@runCatching null
            val host = currentGroupPlayers().firstOrNull { it.id == hostUid.hashCode().toLong() }
                ?: Player(hostUid.hashCode().toLong(), hostUid, "", 0)
            val reviewsSnapshot = gameNightDoc.reference.collection("reviews").get().await()
            val reviews = reviewsSnapshot.documents.mapNotNull { reviewDoc ->
                val reviewId = reviewDoc.getLong("id") ?: reviewDoc.id.hashCode().toLong()
                val playerId = reviewDoc.getLong("playerId") ?: return@mapNotNull null
                val hostRating = reviewDoc.getLong("hostRating")?.toInt() ?: return@mapNotNull null
                val foodRating = reviewDoc.getLong("foodRating")?.toInt() ?: return@mapNotNull null
                val eveningRating = reviewDoc.getLong("eveningRating")?.toInt() ?: return@mapNotNull null
                val comment = reviewDoc.getString("comment") ?: ""
                Review(
                    id = reviewId,
                    playerId = playerId,
                    gameNightId = gameNight.id,
                    hostRating = hostRating,
                    foodRating = foodRating,
                    eveningRating = eveningRating,
                    comment = comment,
                )
            }
            ReviewSnapshot(
                gameNight = gameNight,
                host = host,
                reviews = reviews,
                averages = reviews.takeIf { it.isNotEmpty() }?.let {
                    ReviewAverages(
                        host = it.map(Review::hostRating).average(),
                        food = it.map(Review::foodRating).average(),
                        evening = it.map(Review::eveningRating).average(),
                    )
                },
            )
        }
    }

    override suspend fun finishGameNight(gameNightId: Long): Result<GameNight> = withContext(Dispatchers.IO) {
        runCatching {
            val groupId = currentGroupId() ?: error("Du bist in keiner Gruppe angemeldet.")
            val gameNightDoc = currentGameNightDocument(groupId, gameNightId) ?: error("Der Spieleabend wurde nicht gefunden.")
            gameNightDoc.reference.update(
                mapOf(
                    "status" to GameNightStatus.FINISHED.name,
                    "updatedAt" to Timestamp.now(),
                ),
            ).await()
            gameNightDoc.toGameNight()?.copy(status = GameNightStatus.FINISHED) ?: error("Der Spieleabend konnte nicht geladen werden.")
        }
    }

    override suspend fun submitReview(
        playerId: Long,
        gameNightId: Long,
        hostRating: Int,
        foodRating: Int,
        eveningRating: Int,
        comment: String,
    ): Result<Review> = withContext(Dispatchers.IO) {
        runCatching {
            requireCurrentPlayer(playerId)
            val groupId = currentGroupId() ?: error("Du bist in keiner Gruppe angemeldet.")
            val gameNightDoc = currentGameNightDocument(groupId, gameNightId) ?: error("Der Spieleabend wurde nicht gefunden.")
            val currentGameNight = gameNightDoc.toGameNight() ?: error("Der Spieleabend konnte nicht geladen werden.")
            require(currentGameNight.status == GameNightStatus.FINISHED) {
                "Nur abgeschlossene Spieleabende können bewertet werden."
            }
            val players = currentGroupPlayers()
            require(players.any { it.id == playerId }) { "Der ausgewählte Spieler wurde nicht gefunden." }
            val reviewsSnapshot = gameNightDoc.reference.collection("reviews").get().await()
            require(reviewsSnapshot.documents.none { it.getLong("playerId") == playerId }) {
                "Dieser Spieler hat den Spieleabend bereits bewertet."
            }
            require(listOf(hostRating, foodRating, eveningRating).all { it in 1..5 }) {
                "Alle Bewertungen müssen zwischen 1 und 5 liegen."
            }
            val reviewId = System.currentTimeMillis()
            val reviewDoc = gameNightDoc.reference.collection("reviews").document()
            reviewDoc.set(
                mapOf(
                    "id" to reviewId,
                    "playerId" to playerId,
                    "gameNightId" to gameNightId,
                    "hostRating" to hostRating,
                    "foodRating" to foodRating,
                    "eveningRating" to eveningRating,
                    "comment" to comment.trim(),
                    "createdAt" to Timestamp.now(),
                ),
            ).await()
            Review(
                id = reviewId,
                playerId = playerId,
                gameNightId = gameNightId,
                hostRating = hostRating,
                foodRating = foodRating,
                eveningRating = eveningRating,
                comment = comment.trim(),
            )
        }
    }

    override suspend fun getFoodVotingSnapshot(): Result<FoodVotingSnapshot?> = withContext(Dispatchers.IO) {
        runCatching {
            val groupId = currentGroupId() ?: return@runCatching null
            val night = getUpcomingGameNight().getOrNull() ?: return@runCatching null
            val gameNightDocId = currentGameNightDocId(groupId) ?: return@runCatching null
            val categoriesRef = firestore.collection("groups")
                .document(groupId)
                .collection("gameNights")
                .document(gameNightDocId)
                .collection("foodCategories")
            var categoryDocs = categoriesRef.get().await().documents
            if (categoryDocs.isEmpty()) {
                val defaults = listOf("Asiatisch", "Burger", "Pizza")
                defaults.forEachIndexed { index, categoryName ->
                    val docRef = categoriesRef.document()
                    docRef.set(
                        mapOf(
                            "id" to (System.currentTimeMillis() + index).toLong(),
                            "name" to categoryName,
                            "gameNightId" to night.gameNight.id,
                            "createdAt" to Timestamp.now(),
                        ),
                    ).await()
                }
                categoryDocs = categoriesRef.get().await().documents
            }

            val voteSnapshot = firestore.collection("groups")
                .document(groupId)
                .collection("gameNights")
                .document(gameNightDocId)
                .collection("foodVotes")
                .get()
                .await()
            val voterMap = mutableMapOf<Long, MutableSet<Long>>()
            voteSnapshot.documents.forEach { doc ->
                val categoryId = doc.getLong("foodCategoryId") ?: return@forEach
                val playerId = doc.getLong("playerId") ?: return@forEach
                voterMap.getOrPut(categoryId) { mutableSetOf() }.add(playerId)
            }

            val players = currentGroupPlayers()
            FoodVotingSnapshot(
                gameNight = night.gameNight,
                results = categoryDocs.mapNotNull { doc ->
                    val categoryId = doc.getLong("id") ?: doc.id.hashCode().toLong()
                    val categoryName = doc.getString("name") ?: return@mapNotNull null
                    FoodVoteResult(
                        category = FoodCategory(categoryId, categoryName, night.gameNight.id),
                        voterIds = voterMap[categoryId].orEmpty(),
                    )
                }.sortedWith(compareByDescending<FoodVoteResult> { it.voteCount }.thenBy { it.category.name.lowercase() }),
                players = players,
            )
        }
    }

    override suspend fun addFoodCategory(name: String): Result<FoodCategory> = withContext(Dispatchers.IO) {
        runCatching {
            val groupId = currentGroupId() ?: error("Du bist in keiner Gruppe angemeldet.")
            val night = getUpcomingGameNight().getOrNull() ?: error("Es gibt keinen kommenden Spieleabend.")
            val gameNightDocId = currentGameNightDocId(groupId) ?: error("Es gibt keinen kommenden Spieleabend.")
            val cleanedName = name.trim()
            require(cleanedName.isNotEmpty()) { "Der Name der Kategorie darf nicht leer sein." }
            val categoriesRef = firestore.collection("groups")
                .document(groupId)
                .collection("gameNights")
                .document(gameNightDocId)
                .collection("foodCategories")
            val existing = categoriesRef.get().await().documents
            require(existing.none { it.getString("name")?.equals(cleanedName, ignoreCase = true) == true }) {
                "Diese Essenskategorie gibt es bereits."
            }
            val categoryId = System.currentTimeMillis()
            val docRef = categoriesRef.document()
            docRef.set(
                mapOf(
                    "id" to categoryId,
                    "name" to cleanedName,
                    "gameNightId" to night.gameNight.id,
                    "createdAt" to Timestamp.now(),
                ),
            ).await()
            FoodCategory(categoryId, cleanedName, night.gameNight.id)
        }
    }

    override suspend fun deleteFoodCategory(categoryId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val groupId = currentGroupId() ?: error("Du bist in keiner Gruppe angemeldet.")
            val night = getUpcomingGameNight().getOrNull() ?: error("Es gibt keinen kommenden Spieleabend.")
            val gameNightDocId = currentGameNightDocId(groupId) ?: error("Es gibt keinen kommenden Spieleabend.")
            val categoriesRef = firestore.collection("groups")
                .document(groupId)
                .collection("gameNights")
                .document(gameNightDocId)
                .collection("foodCategories")
            val matchingDocs = categoriesRef.whereEqualTo("id", categoryId).limit(1).get().await()
            matchingDocs.documents.forEach { it.reference.delete().await() }

            val votesRef = firestore.collection("groups")
                .document(groupId)
                .collection("gameNights")
                .document(gameNightDocId)
                .collection("foodVotes")
            val voteDocs = votesRef.whereEqualTo("foodCategoryId", categoryId).get().await()
            voteDocs.documents.forEach { it.reference.delete().await() }
        }
    }

    override suspend fun castFoodVote(playerId: Long, categoryId: Long): Result<FoodVote> = withContext(Dispatchers.IO) {
        runCatching {
            requireCurrentPlayer(playerId)
            val groupId = currentGroupId() ?: error("Du bist in keiner Gruppe angemeldet.")
            val night = getUpcomingGameNight().getOrNull() ?: error("Es gibt keinen kommenden Spieleabend.")
            val gameNightDocId = currentGameNightDocId(groupId) ?: error("Es gibt keinen kommenden Spieleabend.")
            val players = currentGroupPlayers()
            require(players.any { it.id == playerId }) { "Der ausgewählte Spieler wurde nicht gefunden." }
            val categoriesRef = firestore.collection("groups")
                .document(groupId)
                .collection("gameNights")
                .document(gameNightDocId)
                .collection("foodCategories")
            val categoryExists = categoriesRef.whereEqualTo("id", categoryId).limit(1).get().await()
                .documents.isNotEmpty()
            require(categoryExists) { "Die ausgewählte Essenskategorie gehört nicht zum kommenden Spieleabend." }

            val voteRef = firestore.collection("groups")
                .document(groupId)
                .collection("gameNights")
                .document(gameNightDocId)
                .collection("foodVotes")
                .document(playerId.toString())
            val voteId = System.currentTimeMillis()
            voteRef.set(
                mapOf(
                    "id" to voteId,
                    "playerId" to playerId,
                    "foodCategoryId" to categoryId,
                    "gameNightId" to night.gameNight.id,
                    "createdAt" to Timestamp.now(),
                ),
            ).await()
            FoodVote(voteId, playerId, categoryId, night.gameNight.id)
        }
    }

    override suspend fun getOrderingSnapshot(): Result<OrderingSnapshot?> = withContext(Dispatchers.IO) {
        runCatching {
            val groupId = currentGroupId() ?: return@runCatching null
            val night = getUpcomingGameNight().getOrNull() ?: return@runCatching null
            val gameNightDocId = currentGameNightDocId(groupId) ?: return@runCatching null
            val gameNightDoc = firestore.collection("groups").document(groupId).collection("gameNights").document(gameNightDocId).get().await()
            val hostName = currentGroupPlayers().firstOrNull { it.id == night.gameNight.hostId } ?: Player(night.gameNight.hostId, "Gastgeber", "", 0)
            val restaurant = gameNightDoc.getString("restaurantName")?.takeIf { it.isNotBlank() }?.let { name ->
                Restaurant(
                    id = gameNightDoc.getLong("restaurantId") ?: night.gameNight.id,
                    gameNightId = night.gameNight.id,
                    name = name,
                    menuUrl = gameNightDoc.getString("restaurantMenuUrl") ?: "",
                )
            }
            val ordersSnapshot = firestore.collection("groups")
                .document(groupId)
                .collection("gameNights")
                .document(gameNightDocId)
                .collection("orders")
                .get()
                .await()
            val orders = ordersSnapshot.documents.mapNotNull { doc ->
                val playerId = doc.getLong("playerId") ?: return@mapNotNull null
                val orderId = doc.getLong("id") ?: doc.id.hashCode().toLong()
                val dish = doc.getString("dish") ?: return@mapNotNull null
                val note = doc.getString("note") ?: ""
                val priceCents = doc.getLong("priceCents") ?: 0L
                val player = currentGroupPlayers().firstOrNull { it.id == playerId } ?: Player(playerId, "Unbekannt", "", 0)
                OrderWithPlayer(
                    order = FoodOrder(orderId, night.gameNight.id, playerId, dish, note, priceCents),
                    player = player,
                )
            }.sortedBy { it.player.name.lowercase() }
            OrderingSnapshot(
                gameNight = night.gameNight,
                host = hostName,
                restaurant = restaurant,
                orders = orders,
            )
        }
    }

    override suspend fun saveRestaurant(requestingPlayerId: Long, name: String, menuUrl: String): Result<Restaurant> = withContext(Dispatchers.IO) {
        runCatching {
            requireCurrentPlayer(requestingPlayerId)
            val groupId = currentGroupId() ?: error("Du bist in keiner Gruppe angemeldet.")
            val night = getUpcomingGameNight().getOrNull() ?: error("Es gibt keinen kommenden Spieleabend.")
            val gameNightDocId = currentGameNightDocId(groupId) ?: error("Es gibt keinen kommenden Spieleabend.")
            require(requestingPlayerId == night.gameNight.hostId) { "Nur der Gastgeber kann das Restaurant bearbeiten." }
            val cleanUrl = menuUrl.trim()
            require(cleanUrl.startsWith("https://") || cleanUrl.startsWith("http://")) { "Der Menü-Link muss mit http:// oder https:// beginnen." }
            val docRef = firestore.collection("groups").document(groupId).collection("gameNights").document(gameNightDocId)
            docRef.update(
                mapOf(
                    "restaurantName" to name.trim(),
                    "restaurantMenuUrl" to cleanUrl,
                    "restaurantId" to System.currentTimeMillis(),
                    "updatedAt" to Timestamp.now(),
                ),
            ).await()
            Restaurant(
                id = docRef.id.hashCode().toLong(),
                gameNightId = night.gameNight.id,
                name = name.trim(),
                menuUrl = cleanUrl,
            )
        }
    }

    override suspend fun saveFoodOrder(playerId: Long, dish: String, note: String, priceCents: Long): Result<FoodOrder> = withContext(Dispatchers.IO) {
        runCatching {
            requireCurrentPlayer(playerId)
            val groupId = currentGroupId() ?: error("Du bist in keiner Gruppe angemeldet.")
            val night = getUpcomingGameNight().getOrNull() ?: error("Es gibt keinen kommenden Spieleabend.")
            val gameNightDocId = currentGameNightDocId(groupId) ?: error("Es gibt keinen kommenden Spieleabend.")
            require(currentGroupPlayers().any { it.id == playerId }) { "Der ausgewählte Spieler wurde nicht gefunden." }
            require(priceCents >= 0) { "Der Preis darf nicht negativ sein." }
            val cleanedDish = dish.trim()
            require(cleanedDish.isNotEmpty()) { "Gericht darf nicht leer sein." }
            val ordersRef = firestore.collection("groups")
                .document(groupId)
                .collection("gameNights")
                .document(gameNightDocId)
                .collection("orders")
            val existing = ordersRef.whereEqualTo("playerId", playerId).limit(1).get().await()
            val orderId = existing.documents.firstOrNull()?.getLong("id") ?: System.currentTimeMillis()
            val docRef = existing.documents.firstOrNull()?.reference ?: ordersRef.document()
            docRef.set(
                mapOf(
                    "id" to orderId,
                    "playerId" to playerId,
                    "gameNightId" to night.gameNight.id,
                    "dish" to cleanedDish,
                    "note" to note.trim(),
                    "priceCents" to priceCents,
                    "createdAt" to Timestamp.now(),
                ),
            ).await()
            FoodOrder(orderId, night.gameNight.id, playerId, cleanedDish, note.trim(), priceCents)
        }
    }

    override suspend fun deleteFoodOrder(orderId: Long, requestingPlayerId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireCurrentPlayer(requestingPlayerId)
            val groupId = currentGroupId() ?: error("Du bist in keiner Gruppe angemeldet.")
            val night = getUpcomingGameNight().getOrNull() ?: error("Es gibt keinen kommenden Spieleabend.")
            val gameNightDocId = currentGameNightDocId(groupId) ?: error("Es gibt keinen kommenden Spieleabend.")
            val ordersRef = firestore.collection("groups")
                .document(groupId)
                .collection("gameNights")
                .document(gameNightDocId)
                .collection("orders")
            val existing = ordersRef.whereEqualTo("id", orderId).limit(1).get().await()
            val match = existing.documents.firstOrNull() ?: error("Die Bestellung wurde nicht gefunden.")
            require(match.getLong("playerId") == requestingPlayerId) { "Nur die eigene Bestellung kann gelöscht werden." }
            match.reference.delete().await()
            Unit
        }
    }

    private suspend fun currentGameNightDocument(groupId: String, gameNightId: Long? = null): DocumentSnapshot? {
        val gameNightsRef = firestore.collection("groups").document(groupId).collection("gameNights")
        val matching: List<DocumentSnapshot> = if (gameNightId != null && gameNightId != 0L) {
            val byId = gameNightsRef.whereEqualTo("id", gameNightId).limit(1).get().await().documents
            if (byId.isNotEmpty()) {
                byId
            } else {
                val byFallback = gameNightsRef.orderBy("startsAt", Query.Direction.DESCENDING).limit(50).get().await().documents
                listOfNotNull(
                    byFallback.firstOrNull { doc ->
                        val docId = doc.getLong("id") ?: doc.id.hashCode().toLong()
                        docId == gameNightId
                    },
                )
            }
        } else {
            gameNightsRef.orderBy("startsAt", Query.Direction.ASCENDING).limit(1).get().await().documents
        }
        return matching.firstOrNull()
    }

    private fun requireCurrentPlayer(requestedPlayerId: Long): Long {
        val currentPlayerId = auth.currentUser?.uid?.hashCode()?.toLong()
            ?: error("Du bist nicht angemeldet.")
        require(requestedPlayerId == currentPlayerId) {
            "Du kannst nur Angaben für dein eigenes Konto ändern. Melde dich mit dem passenden Konto an."
        }
        return currentPlayerId
    }

    private suspend fun currentGroupPlayers(): List<Player> {
        val groupId = currentGroupId() ?: return emptyList()
        val membersSnapshot = firestore.collection("groups").document(groupId).collection("members").get().await()
        return membersSnapshot.documents.mapNotNull { doc ->
            val member = runCatching { GroupMember.fromMap(doc.data ?: emptyMap()) }.getOrNull() ?: return@mapNotNull null
            Player(
                id = member.uid.hashCode().toLong(),
                name = member.displayName.ifBlank { "Unbekannt" },
                address = member.address.ifBlank { "Keine Adresse hinterlegt" },
                hostOrder = member.hostOrder,
            )
        }.sortedBy { it.hostOrder }
    }

    private suspend fun currentGroupId(): String? {
        val uid = auth.currentUser?.uid ?: return null
        val user = firestore.collection("users").document(uid).get().await()
        val activeGroupId = user.getString("activeGroupId")
        if (!activeGroupId.isNullOrBlank()) {
            val membership = firestore.collection("groups").document(activeGroupId).collection("members").document(uid).get().await()
            if (membership.exists()) return activeGroupId
        }
        val userGroups = firestore.collection("users").document(uid).collection("groups").get().await()
        val groupId = userGroups.documents.firstOrNull()?.id
        return groupId
    }

    private fun DocumentSnapshot.toGameNight(): GameNight? {
        val startsAt = getTimestamp("startsAt")?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime()
            ?: return null
        val hostUid = getString("hostUid") ?: return null
        val location = getString("location") ?: ""
        val statusValue = getString("status") ?: GameNightStatus.PLANNED.name
        val stableId = getLong("id") ?: id.hashCode().toLong()
        return GameNight(
            id = stableId,
            startsAt = startsAt,
            hostId = hostUid.hashCode().toLong(),
            location = location,
            status = GameNightStatus.valueOf(statusValue),
        )
    }
}
