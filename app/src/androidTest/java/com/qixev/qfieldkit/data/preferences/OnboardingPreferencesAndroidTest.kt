package com.qixev.qfieldkit.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingPreferencesAndroidTest {

    private lateinit var context: Context
    private lateinit var onboardingPreferences: SharedPreferencesOnboardingPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        onboardingPreferences = SharedPreferencesOnboardingPreferences(context)
        onboardingPreferences.setCompletedOnboarding(false)
    }

    @Test
    fun preferences_persistence_acrossInstances() {
        assertFalse(onboardingPreferences.hasCompletedOnboarding())

        onboardingPreferences.setCompletedOnboarding(true)
        assertTrue(onboardingPreferences.hasCompletedOnboarding())

        val newPrefsInstance = SharedPreferencesOnboardingPreferences(context)
        assertTrue("Completed state must persist across preference instances", newPrefsInstance.hasCompletedOnboarding())

        newPrefsInstance.setCompletedOnboarding(false)
        assertFalse("Resetting state must restore first-run experience", onboardingPreferences.hasCompletedOnboarding())
    }
}

