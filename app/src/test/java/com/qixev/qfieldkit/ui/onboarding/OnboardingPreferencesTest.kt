package com.qixev.qfieldkit.ui.onboarding

import com.qixev.qfieldkit.data.preferences.OnboardingPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingPreferencesTest {

    private class FakeOnboardingPreferences : OnboardingPreferences {
        private var completed = false
        override fun hasCompletedOnboarding(): Boolean = completed
        override fun setCompletedOnboarding(completed: Boolean) {
            this.completed = completed
        }
    }

    @Test
    fun defaultState_isNotCompleted() {
        val prefs = FakeOnboardingPreferences()
        assertFalse(prefs.hasCompletedOnboarding())
    }

    @Test
    fun setCompleted_updatesStateToTrue() {
        val prefs = FakeOnboardingPreferences()
        prefs.setCompletedOnboarding(true)
        assertTrue(prefs.hasCompletedOnboarding())
    }

    @Test
    fun resetCompleted_restoresFirstRunState() {
        val prefs = FakeOnboardingPreferences()
        prefs.setCompletedOnboarding(true)
        assertTrue(prefs.hasCompletedOnboarding())

        prefs.setCompletedOnboarding(false)
        assertFalse(prefs.hasCompletedOnboarding())
    }
}

