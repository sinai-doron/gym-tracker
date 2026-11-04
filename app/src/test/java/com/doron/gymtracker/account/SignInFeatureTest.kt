package com.doron.gymtracker.account

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignInFeatureTest {

    private suspend fun enabled(switchedOn: Boolean, signedIn: Boolean) =
        SignInFeature.enabled(flowOf(switchedOn), flowOf(if (signedIn) "someone" else null)).first()

    @Test
    fun `off on a fresh install`() = runTest {
        assertFalse(enabled(switchedOn = false, signedIn = false))
    }

    @Test
    fun `on for a phone already signed in, so its account keeps syncing and can sign out`() = runTest {
        assertTrue(enabled(switchedOn = false, signedIn = true))
    }

    @Test
    fun `on when switched on by hand`() = runTest {
        assertTrue(enabled(switchedOn = true, signedIn = false))
    }
}
