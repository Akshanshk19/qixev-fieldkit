package com.qixev.qfieldkit.data.preferences

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages first-run onboarding completion state.
 * Uses lightweight SharedPreferences to avoid storing UI flags in the Room business database
 * and avoid adding external heavy dependencies for a single boolean.
 */
interface OnboardingPreferences {
    fun hasCompletedOnboarding(): Boolean
    fun setCompletedOnboarding(completed: Boolean = true)
}

class SharedPreferencesOnboardingPreferences(
    context: Context
) : OnboardingPreferences {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun hasCompletedOnboarding(): Boolean {
        return prefs.getBoolean(KEY_HAS_COMPLETED_ONBOARDING, false)
    }

    override fun setCompletedOnboarding(completed: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_COMPLETED_ONBOARDING, completed).apply()
    }

    companion object {
        const val PREFS_NAME = "qfieldkit_user_prefs"
        const val KEY_HAS_COMPLETED_ONBOARDING = "has_completed_onboarding"
    }
}

