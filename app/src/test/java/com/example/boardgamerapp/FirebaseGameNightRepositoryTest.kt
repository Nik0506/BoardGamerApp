package com.example.boardgamerapp

import com.example.boardgamerapp.data.repository.FirebaseGameNightRepository
import com.example.boardgamerapp.domain.model.AttendanceStatusType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FirebaseGameNightRepositoryTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var repository: FirebaseGameNightRepository

    @Before
    fun setUp() {
        firestore = mockk(relaxed = true)
        auth = mockk(relaxed = true)
        repository = FirebaseGameNightRepository(firestore = firestore, auth = auth)
    }

    @Test
    fun `addPlayer returns failure with unsupported operation explaining group membership`() = runTest {
        val result = repository.addPlayer("Max Mustermann", "Musterstraße 12")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
        assertTrue(result.exceptionOrNull()?.message?.contains("Würfelrunde-Konto") == true)
    }

    @Test
    fun `getUpcomingGameNights returns failure when user is not authenticated`() = runTest {
        every { auth.currentUser } returns null

        val result = repository.getUpcomingGameNights()

        assertTrue(result.isFailure)
        assertEquals("Du bist nicht angemeldet.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `setAttendance returns failure when user is not authenticated`() = runTest {
        every { auth.currentUser } returns null

        val result = repository.setAttendance(123L, AttendanceStatusType.ATTENDING)

        assertTrue(result.isFailure)
        assertEquals("Du bist nicht angemeldet.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `setAttendance rejects modifying attendance for another user`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { mockUser.uid } returns "user_abc"
        every { auth.currentUser } returns mockUser

        val loggedInPlayerId = "user_abc".hashCode().toLong()
        val otherPlayerId = 99999L // Different ID

        val result = repository.setAttendance(otherPlayerId, AttendanceStatusType.ATTENDING)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("eigenes Konto") == true)
    }

    @Test
    fun `castVote returns failure when user is not authenticated`() = runTest {
        every { auth.currentUser } returns null

        val result = repository.castVote(123L, 456L)

        assertTrue(result.isFailure)
        assertEquals("Du bist nicht angemeldet.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `castFoodVote returns failure when user is not authenticated`() = runTest {
        every { auth.currentUser } returns null

        val result = repository.castFoodVote(123L, 789L)

        assertTrue(result.isFailure)
        assertEquals("Du bist nicht angemeldet.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `deleteFoodOrder rejects deletion when requesting user is not owner`() = runTest {
        every { auth.currentUser } returns null

        val result = repository.deleteFoodOrder(1L, 2L)

        assertTrue(result.isFailure)
        assertEquals("Du bist nicht angemeldet.", result.exceptionOrNull()?.message)
    }
}
