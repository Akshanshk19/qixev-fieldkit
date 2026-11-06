package com.qixev.qfieldkit.ui.onboarding

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.qixev.qfieldkit.data.preferences.SharedPreferencesOnboardingPreferences
import com.qixev.qfieldkit.ui.theme.QFieldKitTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var onboardingPreferences: SharedPreferencesOnboardingPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        onboardingPreferences = SharedPreferencesOnboardingPreferences(context)
        // Reset state before every test
        onboardingPreferences.setCompletedOnboarding(false)
    }

    @Test
    fun freshState_isNotCompleted_andShowsStepOne() {
        assertFalse(onboardingPreferences.hasCompletedOnboarding())

        composeTestRule.setContent {
            QFieldKitTheme {
                OnboardingScreen(
                    onCreateFirstJob = {},
                    onCompleteAndGoToJobs = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Qixev FieldKit").assertIsDisplayed()
        composeTestRule.onNodeWithText("Run your workshop without the paperwork.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Get started").assertIsDisplayed()
        composeTestRule.onNodeWithText("Skip").assertIsDisplayed()
    }

    @Test
    fun stepOne_clickSkip_triggersCompletionCallback() {
        var skipped = false

        composeTestRule.setContent {
            QFieldKitTheme {
                OnboardingScreen(
                    onCreateFirstJob = {},
                    onCompleteAndGoToJobs = { skipped = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Skip").performClick()
        assertTrue("Clicking Skip must invoke completion callback", skipped)
    }

    @Test
    fun stepOne_clickGetStarted_progressesToStepTwo() {
        composeTestRule.setContent {
            QFieldKitTheme {
                OnboardingScreen(
                    onCreateFirstJob = {},
                    onCompleteAndGoToJobs = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Get started").performClick()

        // Verify Step 2 content
        composeTestRule.onNodeWithText("Everything stays on this device").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue").assertIsDisplayed()
    }

    @Test
    fun stepTwo_clickContinue_progressesToStepThree() {
        composeTestRule.setContent {
            QFieldKitTheme {
                OnboardingScreen(
                    onCreateFirstJob = {},
                    onCompleteAndGoToJobs = {}
                )
            }
        }

        // Advance to step 2
        composeTestRule.onNodeWithText("Get started").performClick()
        // Advance to step 3
        composeTestRule.onNodeWithText("Continue").performClick()

        // Verify Step 3 content
        composeTestRule.onNodeWithText("Start with a repair job").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create first job").assertIsDisplayed()
        composeTestRule.onNodeWithText("Go to Jobs").assertIsDisplayed()
    }

    @Test
    fun stepThree_clickCreateFirstJob_triggersCreateJobCallback() {
        var createJobTriggered = false

        composeTestRule.setContent {
            QFieldKitTheme {
                OnboardingScreen(
                    onCreateFirstJob = { createJobTriggered = true },
                    onCompleteAndGoToJobs = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Get started").performClick()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Create first job").performClick()

        assertTrue("Clicking 'Create first job' must invoke onCreateFirstJob", createJobTriggered)
    }

    @Test
    fun stepThree_clickGoToJobs_triggersJobsCallback() {
        var goToJobsTriggered = false

        composeTestRule.setContent {
            QFieldKitTheme {
                OnboardingScreen(
                    onCreateFirstJob = {},
                    onCompleteAndGoToJobs = { goToJobsTriggered = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Get started").performClick()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Go to Jobs").performClick()

        assertTrue("Clicking 'Go to Jobs' must invoke onCompleteAndGoToJobs", goToJobsTriggered)
    }
}

