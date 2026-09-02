package com.example.boardgamerapp

import com.example.boardgamerapp.data.auth.UserProfile
import com.example.boardgamerapp.data.group.Group
import com.example.boardgamerapp.data.group.GroupMember
import com.example.boardgamerapp.data.group.GroupRole
import com.example.boardgamerapp.data.repository.BoardGameSuggestion
import com.example.boardgamerapp.data.repository.BoardGameVoteResult
import com.example.boardgamerapp.data.repository.FoodVoteResult
import com.example.boardgamerapp.data.repository.FoodVotingSnapshot
import com.example.boardgamerapp.data.repository.OrderingSnapshot
import com.example.boardgamerapp.data.repository.OrderWithPlayer
import com.example.boardgamerapp.data.repository.ReviewAverages
import com.example.boardgamerapp.data.repository.ReviewSnapshot
import com.example.boardgamerapp.data.repository.UpcomingGameNight
import com.example.boardgamerapp.data.repository.UpcomingGameNightSummary
import com.example.boardgamerapp.data.repository.VotingSnapshot
import com.example.boardgamerapp.domain.model.BoardGame
import com.example.boardgamerapp.domain.model.FoodCategory
import com.example.boardgamerapp.domain.model.FoodOrder
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.Player
import com.example.boardgamerapp.domain.model.Restaurant
import com.example.boardgamerapp.domain.model.Review
import com.google.firebase.Timestamp
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataModelTest {

    @Test
    fun `UserProfile serializes and deserializes Firestore map correctly`() {
        val now = Timestamp.now()
        val profile = UserProfile(
            uid = "user123",
            email = "user@example.com",
            displayName = "Max Mustermann",
            address = "Musterstraße 12",
            createdAt = now,
            updatedAt = now,
        )

        val map = profile.toFirestoreMap()
        assertEquals("user123", map["uid"])
        assertEquals("user@example.com", map["email"])
        assertEquals("Max Mustermann", map["displayName"])
        assertEquals("Musterstraße 12", map["address"])
        assertEquals(now, map["createdAt"])
        assertEquals(now, map["updatedAt"])

        val fromMap = UserProfile.fromMap(map)
        assertEquals(profile, fromMap)
    }

    @Test
    fun `Group serializes and deserializes Firestore map with memberOrder`() {
        val now = Timestamp.now()
        val group = Group(
            id = "grp1",
            name = "Würfelfreunde",
            createdBy = "user123",
            createdAt = now,
            updatedAt = now,
            memberOrder = listOf("uid1", "uid2", "uid3"),
        )

        val map = group.toFirestoreMap()
        assertEquals("grp1", map["id"])
        assertEquals("Würfelfreunde", map["name"])
        assertEquals(listOf("uid1", "uid2", "uid3"), map["memberOrder"])

        val restored = Group.fromMap(map, "grp1")
        assertEquals(group, restored)
    }

    @Test
    fun `GroupMember parses role and hostOrder correctly`() {
        val now = Timestamp.now()
        val member = GroupMember(
            uid = "uid1",
            displayName = "Erika Musterfrau",
            address = "Neustraße 5",
            role = GroupRole.HOST,
            joinedAt = now,
            hostOrder = 2,
        )

        val map = member.toFirestoreMap()
        assertEquals("HOST", map["role"])
        assertEquals(2, map["hostOrder"])

        val fromMap = GroupMember.fromMap(map)
        assertEquals(member, fromMap)
    }

    @Test
    fun `VotingSnapshot sums total votes correctly`() {
        val host = Player(1L, "Max", "Addr", 1)
        val p2 = Player(2L, "Erika", "Addr2", 2)
        val gameNight = GameNight(1L, LocalDateTime.now(), 1L, "Addr", GameNightStatus.PLANNED)
        val game1 = BoardGame(10L, "Catan", "", 1L, 1L)
        val game2 = BoardGame(20L, "Carcassonne", "", 2L, 1L)

        val r1 = BoardGameVoteResult(BoardGameSuggestion(game1, host), voterIds = setOf(1L, 2L))
        val r2 = BoardGameVoteResult(BoardGameSuggestion(game2, p2), voterIds = setOf(1L))

        assertEquals(2, r1.voteCount)
        assertEquals(1, r2.voteCount)

        val snapshot = VotingSnapshot(gameNight, listOf(r1, r2), playerCount = 2)
        assertEquals(3, snapshot.totalVotes)
    }

    @Test
    fun `FoodVotingSnapshot computes total votes and detects missing players`() {
        val p1 = Player(1L, "Max", "Addr", 1)
        val p2 = Player(2L, "Erika", "Addr2", 2)
        val p3 = Player(3L, "Jan", "Addr3", 3)
        val gameNight = GameNight(1L, LocalDateTime.now(), 1L, "Addr", GameNightStatus.PLANNED)

        val cat1 = FoodCategory(1L, "Burger", 1L)
        val cat2 = FoodCategory(2L, "Pizza", 1L)

        val res1 = FoodVoteResult(cat1, voterIds = setOf(1L))
        val res2 = FoodVoteResult(cat2, voterIds = setOf(2L))

        val snapshot = FoodVotingSnapshot(
            gameNight = gameNight,
            results = listOf(res1, res2),
            players = listOf(p1, p2, p3),
        )

        assertEquals(2, snapshot.totalVotes)
        assertEquals(1, snapshot.missingPlayers.size)
        assertEquals("Jan", snapshot.missingPlayers.first().name)
    }

    @Test
    fun `OrderingSnapshot accurately sums total cents of all food orders`() {
        val host = Player(1L, "Max", "Addr", 1)
        val p2 = Player(2L, "Erika", "Addr2", 2)
        val gameNight = GameNight(1L, LocalDateTime.now(), 1L, "Addr", GameNightStatus.PLANNED)
        val rest = Restaurant(1L, 1L, "Pizzeria Bella", "https://menu.example.com")

        val order1 = FoodOrder(1L, 1L, 1L, "Pizza Salami", "", 950L)
        val order2 = FoodOrder(2L, 1L, 2L, "Tiramisu", "", 480L)

        val snapshot = OrderingSnapshot(
            gameNight = gameNight,
            host = host,
            restaurant = rest,
            orders = listOf(OrderWithPlayer(order1, host), OrderWithPlayer(order2, p2)),
        )

        assertEquals(1430L, snapshot.totalCents)
    }

    @Test
    fun `UpcomingGameNightSummary maintains selection flag and group binding`() {
        val host = Player(1L, "Max", "Addr", 1)
        val gameNight = GameNight(1L, LocalDateTime.now(), 1L, "Addr", GameNightStatus.PLANNED)

        val summary = UpcomingGameNightSummary(
            groupId = "group-1",
            groupName = "Freitagsrunde",
            gameNightDocId = "doc-42",
            gameNight = gameNight,
            host = host,
            isSelected = true,
        )

        assertEquals("group-1", summary.groupId)
        assertEquals("Freitagsrunde", summary.groupName)
        assertEquals("doc-42", summary.gameNightDocId)
        assertTrue(summary.isSelected)

        val unselected = summary.copy(isSelected = false)
        assertFalse(unselected.isSelected)
    }

    @Test
    fun `ReviewSnapshot and ReviewAverages retain computed scores`() {
        val host = Player(1L, "Max", "Addr", 1)
        val gameNight = GameNight(1L, LocalDateTime.now(), 1L, "Addr", GameNightStatus.FINISHED)
        val reviews = listOf(
            Review(1L, 2L, 1L, 5, 4, 5, "Super"),
            Review(2L, 3L, 1L, 4, 4, 4, "Gut"),
        )
        val averages = ReviewAverages(host = 4.5, food = 4.0, evening = 4.5)
        val snapshot = ReviewSnapshot(gameNight, host, reviews, averages)

        assertEquals(2, snapshot.reviews.size)
        assertEquals(4.5, snapshot.averages?.host ?: 0.0, 0.01)
        assertEquals(4.0, snapshot.averages?.food ?: 0.0, 0.01)
        assertEquals(4.5, snapshot.averages?.evening ?: 0.0, 0.01)
    }

    @Test
    fun `PlayerEditorUiState sets title based on add or edit mode`() {
        val addState = com.example.boardgamerapp.ui.players.PlayerEditorUiState(playerId = null)
        assertEquals("Spieler hinzufügen", addState.title)

        val editState = com.example.boardgamerapp.ui.players.PlayerEditorUiState(playerId = 42L, name = "Max")
        assertEquals("Spieler bearbeiten", editState.title)
    }

    @Test
    fun `FoodOrderUiModel retains player details and formatted price`() {
        val model = com.example.boardgamerapp.ui.food.FoodOrderUiModel(
            id = 1L,
            playerId = 2L,
            playerName = "Erika",
            dish = "Pasta",
            note = "Ohne Knoblauch",
            price = "9,50 €",
        )
        assertEquals(1L, model.id)
        assertEquals(2L, model.playerId)
        assertEquals("Erika", model.playerName)
        assertEquals("Pasta", model.dish)
        assertEquals("Ohne Knoblauch", model.note)
        assertEquals("9,50 €", model.price)
    }
}
