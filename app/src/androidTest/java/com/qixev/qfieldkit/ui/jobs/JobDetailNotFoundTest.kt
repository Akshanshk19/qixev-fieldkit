package com.qixev.qfieldkit.ui.jobs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.qixev.qfieldkit.ui.theme.QFieldKitTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JobDetailNotFoundTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun nonExistentJobShowsNotFoundUiAndNavigatesBack() {
        var navigatedBack = false

        val notFoundState = JobDetailUiState(
            job = null,
            isLoading = false
        )

        composeTestRule.setContent {
            QFieldKitTheme {
                JobDetailScreen(
                    uiState = notFoundState,
                    onNavigateBack = { navigatedBack = true },
                    onTransitionStatus = {},
                    onOpenAddPartDialog = {},
                    onDismissAddPartDialog = {},
                    onAddPart = { _, _ -> },
                    onRequestRemovePart = {},
                    onDismissRemovePart = {},
                    onConfirmRemovePart = {},
                    onOpenCancelConfirmation = {},
                    onDismissCancelConfirmation = {},
                    onConfirmCancelJob = {},
                    onClearUserMessage = {}
                )
            }
        }

        // Verify "Repair job not found" is displayed
        composeTestRule.onNodeWithText("Repair job not found").assertIsDisplayed()
        composeTestRule.onNodeWithText("The requested repair job ticket does not exist or has been removed.").assertIsDisplayed()

        // Click "Back to Jobs"
        composeTestRule.onNodeWithText("Back to Jobs").performClick()
        assertTrue("Clicking Back to Jobs must invoke onNavigateBack", navigatedBack)
    }
}

