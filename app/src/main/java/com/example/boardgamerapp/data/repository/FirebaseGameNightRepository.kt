package com.example.boardgamerapp.data.repository

import com.example.boardgamerapp.data.group.GroupMember
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
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class FirebaseGameNightRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : BoardGamerRepository {

    private fun <T> firestoreCall(block: () -> T): T = runBlocking(Dispatchers.IO) { block() }

    private fun currentGameNightDocId(groupId: String): String? = firestoreCall {
        val snapshot = Tasks.await(
            firestore.collection("groups")
                .document(groupId)
                .collection("gameNights")
                .orderBy("startsAt", Query.Direction.ASCENDING)
                .limit(1)
                .get(),
        )
        snapshot.documents.firstOrNull()?.id
    }

    private fun resolveHostAddress(groupId: String, uid: String): String {
        val memberSnapshot = firestoreCall {
            Tasks.await(
                firestore.collection("groups").document(groupId).collection("members").document(uid).get(),
            )
        }
        if (memberSnapshot.exists()) {
            val member = GroupMember.fromMap(memberSnapshot.data ?: emptyMap())
            if (member.address.isNotBlank()) return member.address
        }

        val userSnapshot = firestoreCall {
            Tasks.await(firestore.collection("users").document(uid).get())
        }
        val address = userSnapshot.getString("address")?.trim()
        return address?.takeIf { it.isNotBlank() } ?: "Keine Adresse hinterlegt"
    }

    override fun getUpcomingGameNight(): Result<UpcomingGameNight?> = runCatching {
        val groupId = currentGroupId() ?: return@runCatching null
        val snapshot = firestoreCall {
            Tasks.await(
                firestore.collection("groups")
                    .document(groupId)
                    .collection("gameNights")
                    .orderBy("startsAt", Query.Direction.ASCENDING)
                    .limit(1)
                    .get(),
            )
        }

        val doc = snapshot.documents.firstOrNull() ?: return@runCatching null
        val gameNight = doc.toGameNight() ?: return@runCatching null
        val fallbackHost = Player(id = 0L, name = "Gastgeber", address = "", hostOrder = 0)
        val host = doc.getString("hostUid")?.let { uid ->
            val memberSnapshot = firestoreCall {
                Tasks.await(
                    firestore.collection("groups").document(groupId).collection("members").document(uid).get(),
                )
            }
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

    override fun createNextGameNight(): Result<UpcomingGameNight> = runCatching {
        firestoreCall {
            val groupId = currentGroupId() ?: error("Du bist in keiner Gruppe angemeldet.")
            val groupRef = firestore.collection("groups").document(groupId)
            val membersSnapshot = Tasks.await(groupRef.collection("members").get())
            val orderedMembers = membersSnapshot.documents.mapNotNull { doc ->
                runCatching { GroupMember.fromMap(doc.data ?: emptyMap()) }.getOrNull()
            }.sortedBy { it.hostOrder }

            val existingNight = Tasks.await(
                groupRef.collection("gameNights")
                    .orderBy("startsAt", Query.Direction.DESCENDING)
                    .limit(1)
                    .get(),
            ).documents.firstOrNull()

            val previousHostUid = existingNight?.getString("hostUid")
            val nextHost = if (previousHostUid == null) {
                orderedMembers.firstOrNull() ?: error("In der Gruppe gibt es noch keine Mitglieder.")
            } else {
                val currentIndex = orderedMembers.indexOfFirst { it.uid == previousHostUid }
                val nextIndex = if (currentIndex >= 0 && currentIndex + 1 < orderedMembers.size) currentIndex + 1 else 0
                orderedMembers.getOrNull(nextIndex) ?: orderedMembers.first()
            }

            val startsAt = existingNight?.let {
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
                "startsAt" to Timestamp(Date.from(startsAt.atZone(ZoneId.systemDefault()).toInstant())),
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now(),
            )

            val newDoc = groupRef.collection("gameNights").document()
            Tasks.await(newDoc.set(saved))
            val stableId = newDoc.id.hashCode().toLong()
            Tasks.await(newDoc.update("id", stableId))

            UpcomingGameNight(
                gameNight = GameNight(
                    id = stableId,
                    startsAt = startsAt,
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

    override fun getGameSuggestions(): Result<GameNightSuggestions?> = runCatching {
        val groupId = currentGroupId() ?: return@runCatching null
        val night = getUpcomingGameNight().getOrNull() ?: return@runCatching null
        val gameNightDocId = currentGameNightDocId(groupId) ?: return@runCatching null
        val suggestionsSnapshot = firestoreCall {
            Tasks.await(
                firestore.collection("groups")
                    .document(groupId)
                    .collection("gameNights")
                    .document(gameNightDocId)
                    .collection("suggestions")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get(),
            )
        }
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

    override fun getLateNotices(): Result<List<LateNotice>> = runCatching {
        val groupId = currentGroupId() ?: return@runCatching emptyList()
        val night = getUpcomingGameNight().getOrNull() ?: return@runCatching emptyList()
        val gameNightDocId = currentGameNightDocId(groupId) ?: return@runCatching emptyList()
        val snapshot = firestoreCall {
            Tasks.await(
                firestore.collection("groups")
                    .document(groupId)
                    .collection("gameNights")
                    .document(gameNightDocId)
                    .collection("lateNotices")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get(),
            )
        }

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

    override fun addLateNotice(playerId: Long, minutes: Int): Result<LateNotice> = runCatching {
        firestoreCall {
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
            Tasks.await(
                docRef.set(
                    mapOf(
                        "id" to notice.id,
                        "playerId" to playerId,
                        "gameNightId" to night.gameNight.id,
                        "minutes" to minutes,
                        "createdAt" to Timestamp.now(),
                    ),
                ),
            )
            notice
        }
    }

    override fun getPlayers(): Result<List<Player>> = runCatching {
        val groupId = currentGroupId() ?: return@runCatching emptyList()
        val membersSnapshot = firestoreCall {
            Tasks.await(firestore.collection("groups").document(groupId).collection("members").get())
        }
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

    override fun addGameSuggestion(
        name: String,
        description: String,
        suggestedByPlayerId: Long,
    ): Result<BoardGameSuggestion> = runCatching {
        firestoreCall {
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
            Tasks.await(
                docRef.set(
                    mapOf(
                        "id" to suggestionId,
                        "name" to name.trim(),
                        "description" to description.trim(),
                        "suggestedByPlayerId" to suggestedByPlayerId,
                        "gameNightId" to night.gameNight.id,
                        "createdAt" to Timestamp.now(),
                    ),
                ),
            )
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

    override fun deleteGameSuggestion(boardGameId: Long, requestingPlayerId: Long): Result<Unit> = runCatching {
        firestoreCall {
            requireCurrentPlayer(requestingPlayerId)
            val groupId = currentGroupId() ?: error("Du bist in keiner Gruppe angemeldet.")
            val night = getUpcomingGameNight().getOrNull() ?: error("Es gibt noch keinen nächsten Spieleabend.")
            val gameNightDocId = currentGameNightDocId(groupId) ?: error("Es gibt noch keinen nächsten Spieleabend.")
            val suggestionsRef = firestore.collection("groups")
                .document(groupId)
                .collection("gameNights")
                .document(gameNightDocId)
                .collection("suggestions")
            val docs = Tasks.await(
                suggestionsRef.whereEqualTo("id", boardGameId).limit(1).get(),
            )
            require(docs.documents.firstOrNull()?.getLong("suggestedByPlayerId") == requestingPlayerId) {
                "Du kannst nur deinen eigenen Spielvorschlag löschen."
            }
            docs.documents.forEach { doc ->
                Tasks.await(doc.reference.delete())
            }
            val votesRef = firestore.collection("groups")
                .document(groupId)
                .collection("gameNights")
                .document(gameNightDocId)
                .collection("votes")
            val voteDocs = Tasks.await(votesRef.whereEqualTo("boardGameId", boardGameId).get())
            voteDocs.documents.forEach { doc ->
                Tasks.await(doc.reference.delete())
            }
        }
    }

    override fun getVotingSnapshot(): Result<VotingSnapshot?> = runCatching {
        val suggestionsResult = getGameSuggestions().getOrNull() ?: return@runCatching null
        val groupId = currentGroupId() ?: return@runCatching null
        val night = getUpcomingGameNight().getOrNull() ?: return@runCatching null
        val gameNightDocId = currentGameNightDocId(groupId) ?: return@runCatching null
        val voteSnapshot = firestoreCall {
            Tasks.await(
                firestore.collection("groups")
                    .document(groupId)
                    .collection("gameNights")
                    .document(gameNightDocId)
                    .collection("votes")
                    .get(),
            )
        }
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

    override fun castVote(playerId: Long, boardGameId: Long): Result<Vote> = runCatching {
        firestoreCall {
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
            Tasks.await(
                voteRef.set(
                    mapOf(
                        "playerId" to playerId,
                        "boardGameId" to boardGameId,
                        "gameNightId" to night.gameNight.id,
                        "createdAt" to Timestamp.now(),
                    ),
                ),
            )
            Vote(
                id = playerId,
                playerId = playerId,
                boardGameId = boardGameId,
                gameNightId = night.gameNight.id,
            )
        }
    }

    override fun addPlayer(name: String, address: String): Result<Player> = Result.failure(
        UnsupportedOperationException("Mitglieder treten der Gruppe mit ihrem eigenen Würfelrunde-Konto bei."),
    )

    override fun updatePlayer(id: Long, name: String, address: String): Result<Player> = runCatching {
        firestoreCall {
            val uid = auth.currentUser?.uid ?: error("Du bist nicht angemeldet.")
            require(uid.hashCode().toLong() == id) { "Du kannst nur dein eigenes Profil bearbeiten." }
            val groupId = currentGroupId() ?: error("Du bist in keiner Gruppe angemeldet.")
            val cleanName = name.trim().also { require(it.isNotEmpty()) { "Name darf nicht leer sein." } }
            val cleanAddress = address.trim().also { require(it.isNotEmpty()) { "Adresse darf nicht leer sein." } }
            val values = mapOf("displayName" to cleanName, "address" to cleanAddress, "updatedAt" to Timestamp.now())
            Tasks.await(firestore.collection("users").document(uid).update(values))
            Tasks.await(firestore.collection("groups").document(groupId).collection("members").document(uid).update(values))
            currentGroupPlayers().first { it.id == id }
        }
    }

    override fun movePlayer(id: Long, direction: MoveDirection): Result<List<Player>> = runCatching {
        firestoreCall {
            val groupId = currentGroupId() ?: error("Du bist in keiner Gruppe angemeldet.")
            val groupRef = firestore.collection("groups").document(groupId)
            val group = Tasks.await(groupRef.get())
            val order = (group.get("memberOrder") as? List<*>)?.mapNotNull { it as? String }?.toMutableList() ?: mutableListOf()
            val memberDocs = Tasks.await(groupRef.collection("members").get()).documents
            val uid = memberDocs.firstOrNull { it.id.hashCode().toLong() == id }?.id ?: error("Mitglied nicht gefunden.")
            val current = order.indexOf(uid)
            val target = if (direction == MoveDirection.UP) current - 1 else current + 1
            if (current >= 0 && target in order.indices) {
                val moved = order.removeAt(current)
                order.add(target, moved)
                Tasks.await(groupRef.update("memberOrder", order, "updatedAt", Timestamp.now()))
            }
            currentGroupPlayers()
        }
    }

    override fun getReviewSnapshot(): Result<ReviewSnapshot?> = runCatching {
        val groupId = currentGroupId() ?: return@runCatching null
        val gameNightDoc = currentGameNightDocument(groupId) ?: return@runCatching null
        val gameNight = gameNightDoc.toGameNight() ?: return@runCatching null
        val hostUid = gameNightDoc.getString("hostUid") ?: return@runCatching null
        val host = currentGroupPlayers().firstOrNull { it.id == hostUid.hashCode().toLong() }
            ?: Player(hostUid.hashCode().toLong(), hostUid, "", 0)
        val reviewsSnapshot = firestoreCall {
            Tasks.await(gameNightDoc.reference.collection("reviews").get())
        }
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

    override fun finishGameNight(gameNightId: Long): Result<GameNight> = runCatching {
        val groupId = currentGroupId() ?: error("Du bist in keiner Gruppe angemeldet.")
        val gameNightDoc = currentGameNightDocument(groupId, gameNightId) ?: error("Der Spieleabend wurde nicht gefunden.")
        Tasks.await(
            gameNightDoc.reference.update(
                mapOf(
                    "status" to GameNightStatus.FINISHED.name,
                    "updatedAt" to Timestamp.now(),
                ),
            ),
        )
        gameNightDoc.toGameNight()?.copy(status = GameNightStatus.FINISHED) ?: error("Der Spieleabend konnte nicht geladen werden.")
    }

    override fun submitReview(
        playerId: Long,
        gameNightId: Long,
        hostRating: Int,
        foodRating: Int,
        eveningRating: Int,
        comment: String,
    ): Result<Review> = runCatching {
        requireCurrentPlayer(playerId)
        val groupId = currentGroupId() ?: error("Du bist in keiner Gruppe angemeldet.")
        val gameNightDoc = currentGameNightDocument(groupId, gameNightId) ?: error("Der Spieleabend wurde nicht gefunden.")
        val currentGameNight = gameNightDoc.toGameNight() ?: error("Der Spieleabend konnte nicht geladen werden.")
        require(currentGameNight.status == GameNightStatus.FINISHED) {
            "Nur abgeschlossene Spieleabende können bewertet werden."
        }
        val players = currentGroupPlayers()
        require(players.any { it.id == playerId }) { "Der ausgewählte Spieler wurde nicht gefunden." }
        val reviewsSnapshot = firestoreCall { Tasks.await(gameNightDoc.reference.collection("reviews").get()) }
        require(reviewsSnapshot.documents.none { it.getLong("playerId") == playerId }) {
            "Dieser Spieler hat den Spieleabend bereits bewertet."
        }
        require(listOf(hostRating, foodRating, eveningRating).all { it in 1..5 }) {
            "Alle Bewertungen müssen zwischen 1 und 5 liegen."
        }
        val reviewId = System.currentTimeMillis()
        val reviewDoc = gameNightDoc.reference.collection("reviews").document()
        Tasks.await(
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
            ),
        )
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

    override fun getFoodVotingSnapshot(): Result<FoodVotingSnapshot?> = runCatching {
        val groupId = currentGroupId() ?: return@runCatching null
        val night = getUpcomingGameNight().getOrNull() ?: return@runCatching null
        val gameNightDocId = currentGameNightDocId(groupId) ?: return@runCatching null
        val categoriesRef = firestore.collection("groups")
            .document(groupId)
            .collection("gameNights")
            .document(gameNightDocId)
            .collection("foodCategories")
        var categoryDocs = firestoreCall { Tasks.await(categoriesRef.get()) }.documents
        if (categoryDocs.isEmpty()) {
            val defaults = listOf("Asiatisch", "Burger", "Pizza")
            defaults.forEachIndexed { index, categoryName ->
                val docRef = categoriesRef.document()
                Tasks.await(
                    docRef.set(
                        mapOf(
                            "id" to (System.currentTimeMillis() + index).toLong(),
                            "name" to categoryName,
                            "gameNightId" to night.gameNight.id,
                            "createdAt" to Timestamp.now(),
                        ),
                    ),
                )
            }
            categoryDocs = firestoreCall { Tasks.await(categoriesRef.get()) }.documents
        }

        val voteSnapshot = firestoreCall {
            Tasks.await(
                firestore.collection("groups")
                    .document(groupId)
                    .collection("gameNights")
                    .document(gameNightDocId)
                    .collection("foodVotes")
                    .get(),
            )
        }
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

    override fun addFoodCategory(name: String): Result<FoodCategory> = runCatching {
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
        val existing = firestoreCall { Tasks.await(categoriesRef.get()) }.documents
        require(existing.none { it.getString("name")?.equals(cleanedName, ignoreCase = true) == true }) {
            "Diese Essenskategorie gibt es bereits."
        }
        val categoryId = System.currentTimeMillis()
        val docRef = categoriesRef.document()
        Tasks.await(
            docRef.set(
                mapOf(
                    "id" to categoryId,
                    "name" to cleanedName,
                    "gameNightId" to night.gameNight.id,
                    "createdAt" to Timestamp.now(),
                ),
            ),
        )
        FoodCategory(categoryId, cleanedName, night.gameNight.id)
    }

    override fun deleteFoodCategory(categoryId: Long): Result<Unit> = runCatching {
        val groupId = currentGroupId() ?: error("Du bist in keiner Gruppe angemeldet.")
        val night = getUpcomingGameNight().getOrNull() ?: error("Es gibt keinen kommenden Spieleabend.")
        val gameNightDocId = currentGameNightDocId(groupId) ?: error("Es gibt keinen kommenden Spieleabend.")
        val categoriesRef = firestore.collection("groups")
            .document(groupId)
            .collection("gameNights")
            .document(gameNightDocId)
            .collection("foodCategories")
        val matchingDocs = firestoreCall { Tasks.await(categoriesRef.whereEqualTo("id", categoryId).limit(1).get()) }
        matchingDocs.documents.forEach { Tasks.await(it.reference.delete()) }

        val votesRef = firestore.collection("groups")
            .document(groupId)
            .collection("gameNights")
            .document(gameNightDocId)
            .collection("foodVotes")
        val voteDocs = firestoreCall { Tasks.await(votesRef.whereEqualTo("foodCategoryId", categoryId).get()) }
        voteDocs.documents.forEach { Tasks.await(it.reference.delete()) }
    }

    override fun castFoodVote(playerId: Long, categoryId: Long): Result<FoodVote> = runCatching {
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
        val categoryExists = firestoreCall { Tasks.await(categoriesRef.whereEqualTo("id", categoryId).limit(1).get()) }
            .documents.isNotEmpty()
        require(categoryExists) { "Die ausgewählte Essenskategorie gehört nicht zum kommenden Spieleabend." }

        val voteRef = firestore.collection("groups")
            .document(groupId)
            .collection("gameNights")
            .document(gameNightDocId)
            .collection("foodVotes")
            .document(playerId.toString())
        val voteId = System.currentTimeMillis()
        Tasks.await(
            voteRef.set(
                mapOf(
                    "id" to voteId,
                    "playerId" to playerId,
                    "foodCategoryId" to categoryId,
                    "gameNightId" to night.gameNight.id,
                    "createdAt" to Timestamp.now(),
                ),
            ),
        )
        FoodVote(voteId, playerId, categoryId, night.gameNight.id)
    }

    override fun getOrderingSnapshot(): Result<OrderingSnapshot?> = runCatching {
        val groupId = currentGroupId() ?: return@runCatching null
        val night = getUpcomingGameNight().getOrNull() ?: return@runCatching null
        val gameNightDocId = currentGameNightDocId(groupId) ?: return@runCatching null
        val gameNightDoc = firestoreCall {
            Tasks.await(firestore.collection("groups").document(groupId).collection("gameNights").document(gameNightDocId).get())
        }
        val hostName = currentGroupPlayers().firstOrNull { it.id == night.gameNight.hostId } ?: Player(night.gameNight.hostId, "Gastgeber", "", 0)
        val restaurant = gameNightDoc.getString("restaurantName")?.takeIf { it.isNotBlank() }?.let { name ->
            Restaurant(
                id = gameNightDoc.getLong("restaurantId") ?: night.gameNight.id,
                gameNightId = night.gameNight.id,
                name = name,
                menuUrl = gameNightDoc.getString("restaurantMenuUrl") ?: "",
            )
        }
        val ordersSnapshot = firestoreCall {
            Tasks.await(
                firestore.collection("groups")
                    .document(groupId)
                    .collection("gameNights")
                    .document(gameNightDocId)
                    .collection("orders")
                    .get(),
            )
        }
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

    override fun saveRestaurant(requestingPlayerId: Long, name: String, menuUrl: String): Result<Restaurant> = runCatching {
        requireCurrentPlayer(requestingPlayerId)
        val groupId = currentGroupId() ?: error("Du bist in keiner Gruppe angemeldet.")
        val night = getUpcomingGameNight().getOrNull() ?: error("Es gibt keinen kommenden Spieleabend.")
        val gameNightDocId = currentGameNightDocId(groupId) ?: error("Es gibt keinen kommenden Spieleabend.")
        require(requestingPlayerId == night.gameNight.hostId) { "Nur der Gastgeber kann das Restaurant bearbeiten." }
        val cleanUrl = menuUrl.trim()
        require(cleanUrl.startsWith("https://") || cleanUrl.startsWith("http://")) { "Der Menü-Link muss mit http:// oder https:// beginnen." }
        val docRef = firestore.collection("groups").document(groupId).collection("gameNights").document(gameNightDocId)
        Tasks.await(
            docRef.update(
                mapOf(
                    "restaurantName" to name.trim(),
                    "restaurantMenuUrl" to cleanUrl,
                    "restaurantId" to System.currentTimeMillis(),
                    "updatedAt" to Timestamp.now(),
                ),
            ),
        )
        Restaurant(
            id = docRef.id.hashCode().toLong(),
            gameNightId = night.gameNight.id,
            name = name.trim(),
            menuUrl = cleanUrl,
        )
    }

    override fun saveFoodOrder(playerId: Long, dish: String, note: String, priceCents: Long): Result<FoodOrder> = runCatching {
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
        val existing = firestoreCall { Tasks.await(ordersRef.whereEqualTo("playerId", playerId).limit(1).get()) }
        val orderId = existing.documents.firstOrNull()?.getLong("id") ?: System.currentTimeMillis()
        val docRef = existing.documents.firstOrNull()?.reference ?: ordersRef.document()
        Tasks.await(
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
            ),
        )
        FoodOrder(orderId, night.gameNight.id, playerId, cleanedDish, note.trim(), priceCents)
    }

    override fun deleteFoodOrder(orderId: Long, requestingPlayerId: Long): Result<Unit> = runCatching {
        requireCurrentPlayer(requestingPlayerId)
        val groupId = currentGroupId() ?: error("Du bist in keiner Gruppe angemeldet.")
        val night = getUpcomingGameNight().getOrNull() ?: error("Es gibt keinen kommenden Spieleabend.")
        val gameNightDocId = currentGameNightDocId(groupId) ?: error("Es gibt keinen kommenden Spieleabend.")
        val ordersRef = firestore.collection("groups")
            .document(groupId)
            .collection("gameNights")
            .document(gameNightDocId)
            .collection("orders")
        val existing = firestoreCall { Tasks.await(ordersRef.whereEqualTo("id", orderId).limit(1).get()) }
        val match = existing.documents.firstOrNull() ?: error("Die Bestellung wurde nicht gefunden.")
        require(match.getLong("playerId") == requestingPlayerId) { "Nur die eigene Bestellung kann gelöscht werden." }
        Tasks.await(match.reference.delete())
    }

    private fun currentGameNightDocument(groupId: String, gameNightId: Long? = null): com.google.firebase.firestore.DocumentSnapshot? = firestoreCall {
        val gameNightsRef = firestore.collection("groups").document(groupId).collection("gameNights")
        val matching: List<com.google.firebase.firestore.DocumentSnapshot> = if (gameNightId != null) {
            val byId = Tasks.await(gameNightsRef.whereEqualTo("id", gameNightId).limit(1).get()).documents
            if (byId.isNotEmpty()) {
                byId
            } else {
                val byFallback = Tasks.await(gameNightsRef.orderBy("startsAt", Query.Direction.DESCENDING).limit(50).get()).documents
                listOfNotNull(
                    byFallback.firstOrNull { doc ->
                        val docId = doc.getLong("id") ?: doc.id.hashCode().toLong()
                        docId == gameNightId
                    },
                )
            }
        } else {
            Tasks.await(gameNightsRef.orderBy("startsAt", Query.Direction.DESCENDING).limit(1).get()).documents
        }
        matching.firstOrNull()
    }

    private fun requireCurrentPlayer(requestedPlayerId: Long): Long {
        val currentPlayerId = auth.currentUser?.uid?.hashCode()?.toLong()
            ?: error("Du bist nicht angemeldet.")
        require(requestedPlayerId == currentPlayerId) {
            "Du kannst nur Angaben für dein eigenes Konto ändern. Melde dich mit dem passenden Konto an."
        }
        return currentPlayerId
    }

    private fun currentGroupPlayers(): List<Player> = firestoreCall {
        val groupId = currentGroupId() ?: return@firestoreCall emptyList()
        val membersSnapshot = Tasks.await(firestore.collection("groups").document(groupId).collection("members").get())
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

    private fun currentGroupId(): String? = firestoreCall {
        val uid = auth.currentUser?.uid ?: return@firestoreCall null
        val user = Tasks.await(firestore.collection("users").document(uid).get())
        val activeGroupId = user.getString("activeGroupId")
        if (!activeGroupId.isNullOrBlank()) {
            val membership = Tasks.await(firestore.collection("groups").document(activeGroupId).collection("members").document(uid).get())
            if (membership.exists()) return@firestoreCall activeGroupId
        }
        val userGroups = Tasks.await(firestore.collection("users").document(uid).collection("groups").get())
        val groupId = userGroups.documents.firstOrNull()?.id
        groupId
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toGameNight(): GameNight? {
        val startsAt = getTimestamp("startsAt")?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime()
            ?: return null
        val hostUid = getString("hostUid") ?: return null
        val location = getString("location") ?: ""
        val statusValue = getString("status") ?: GameNightStatus.PLANNED.name
        return GameNight(
            id = id.hashCode().toLong(),
            startsAt = startsAt,
            hostId = hostUid.hashCode().toLong(),
            location = location,
            status = GameNightStatus.valueOf(statusValue),
        )
    }
}
