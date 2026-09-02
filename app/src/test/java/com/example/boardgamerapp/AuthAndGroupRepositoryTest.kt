package com.example.boardgamerapp

import com.example.boardgamerapp.data.auth.AuthRepository
import com.example.boardgamerapp.data.group.GroupRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthAndGroupRepositoryTest {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var authRepository: AuthRepository
    private lateinit var groupRepository: GroupRepository

    @Before
    fun setUp() {
        auth = mockk(relaxed = true)
        firestore = mockk(relaxed = true)
        authRepository = AuthRepository(auth = auth, firestore = firestore)
        groupRepository = GroupRepository(auth = auth, firestore = firestore)
    }

    @Test
    fun `AuthRepository isSignedIn returns true when user is present`() {
        val user = mockk<FirebaseUser>()
        every { auth.currentUser } returns user

        assertTrue(authRepository.isSignedIn())
        assertEquals(user, authRepository.currentUser())
    }

    @Test
    fun `AuthRepository isSignedIn returns false when user is null`() {
        every { auth.currentUser } returns null

        assertFalse(authRepository.isSignedIn())
        assertNull(authRepository.currentUser())
    }

    @Test
    fun `AuthRepository signOut delegates to FirebaseAuth`() {
        authRepository.signOut()
        verify { auth.signOut() }
    }

    @Test
    fun `GroupRepository createGroup returns failure when user is not authenticated`() = runTest {
        every { auth.currentUser } returns null

        val result = groupRepository.createGroup("Neue Testgruppe")

        assertTrue(result.isFailure)
        assertEquals("User not authenticated.", result.exceptionOrNull()?.message)
    }
}
