package com.zts.sanshi.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.zts.sanshi.domain.Task
import com.zts.sanshi.ui.theme.SanshiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SanshiScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyStateShowsCopy() {
        composeRule.setContent {
            SanshiTheme {
                SanshiScreen(
                    state = TodayUiState(dateLabel = "10月28日 · 周三"),
                    reduceMotion = true,
                    onAddClick = {},
                    onTaskClick = {},
                    onToggle = { _, _ -> },
                    onDeleteClick = {},
                    onConfirmDelete = {},
                    onDismissDelete = {},
                    onDismissFullHint = {},
                    onDismissEditor = {},
                    onSaveEditor = { _, _ -> },
                    onOpenPool = {},
                    onOpenHistory = {},
                    onRollTomorrow = {},
                    onMoveToPool = {},
                    onAdmitWaiting = {},
                    onDisposeAllRoll = {},
                    onDisposeEach = {},
                    onDisposeAllDeleteAsk = {},
                    onDisposeAllDeleteConfirm = {},
                    onDismissDeleteAllConfirm = {},
                    onClearSnack = {},
                )
            }
        }
        composeRule.onNodeWithText(Copy.EmptyTitle).assertIsDisplayed()
        composeRule.onNodeWithText(Copy.EmptySubtitle).assertIsDisplayed()
    }

    @Test
    fun fullStateDisablesAdd() {
        composeRule.setContent {
            SanshiTheme {
                SanshiScreen(
                    state = TodayUiState(
                        dateLabel = "10月28日 · 周三",
                        tasks = listOf(
                            Task("1", "一", false),
                            Task("2", "二", false),
                            Task("3", "三", false),
                        ),
                    ),
                    reduceMotion = true,
                    onAddClick = {},
                    onTaskClick = {},
                    onToggle = { _, _ -> },
                    onDeleteClick = {},
                    onConfirmDelete = {},
                    onDismissDelete = {},
                    onDismissFullHint = {},
                    onDismissEditor = {},
                    onSaveEditor = { _, _ -> },
                    onOpenPool = {},
                    onOpenHistory = {},
                    onRollTomorrow = {},
                    onMoveToPool = {},
                    onAdmitWaiting = {},
                    onDisposeAllRoll = {},
                    onDisposeEach = {},
                    onDisposeAllDeleteAsk = {},
                    onDisposeAllDeleteConfirm = {},
                    onDismissDeleteAllConfirm = {},
                    onClearSnack = {},
                )
            }
        }
        composeRule.onNodeWithText(Copy.FullHint).assertIsDisplayed()
        composeRule.onNodeWithText(Copy.Add).assertIsNotEnabled()
    }
}
