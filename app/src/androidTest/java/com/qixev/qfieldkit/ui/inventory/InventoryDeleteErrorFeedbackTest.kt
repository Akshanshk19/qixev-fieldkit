package com.qixev.qfieldkit.ui.inventory

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.qixev.qfieldkit.ui.theme.QFieldKitTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InventoryDeleteErrorFeedbackTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun inventoryScreenDisplaysUserMessageInSnackbar() {
        var cleared = false
        val message = "This item cannot be deleted because it is used by a repair job."

        val stateWithMessage = InventoryUiState(
            userMessage = message,
            isLoading = false
        )

        composeTestRule.setContent {
            QFieldKitTheme {
                InventoryScreen(
                    uiState = stateWithMessage,
                    onSearchQueryChanged = {},
                    onCategorySelected = {},
                    onOpenAddDialog = {},
                    onOpenEditDialog = {},
                    onDismissFormDialog = {},
                    onRequestDelete = {},
                    onDismissDeleteDialog = {},
                    onConfirmDelete = {},
                    onClearUserMessage = { cleared = true },
                    onSaveItem = { _, _, _, _, _, _, _ -> }
                )
            }
        }

        // Verify the message is shown in the snackbar
        composeTestRule.onNodeWithText(message).assertIsDisplayed()
    }
}
