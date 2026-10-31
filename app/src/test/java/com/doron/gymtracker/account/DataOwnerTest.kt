package com.doron.gymtracker.account

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** One person's data per phone: who may sync it. */
class DataOwnerTest {
    private val doron = SignedInUser("user-1", "doron@example.com", "Doron", null)
    private val guest = SignedInUser("user-2", "guest@example.com", "Guest", null)

    @Test
    fun `the first account to sign in claims an unowned phone`() =
        assertTrue(DataOwner.allows(null, doron))

    @Test
    fun `the owner can always sign in again`() =
        assertTrue(DataOwner.allows(Owner("user-1", "doron@example.com"), doron))

    @Test
    fun `another account is refused, even with the same email`() {
        assertFalse(DataOwner.allows(Owner("user-1", "doron@example.com"), guest))
        assertFalse(DataOwner.allows(Owner("user-1", "guest@example.com"), guest))
    }
}
