package com.zts.sanshi.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import com.zts.sanshi.domain.Task
import com.zts.sanshi.ui.theme.SanshiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SanshiScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun emptyStateOffersAddAndHidesCompletion() {
        rule.setContent {
            SanshiTheme {
                screen(TodayUiState(dateLabel = "10月28日 · 周三"))
            }
        }
        rule.onNodeWithText(Copy.AppName).assertIsDisplayed()
        rule.onNodeWithText("10月28日 · 周三").assertIsDisplayed()
        rule.onNodeWithText(Copy.EmptyTitle).assertIsDisplayed()
        rule.onNodeWithText(Copy.EmptySubtitle).assertIsDisplayed()
        rule.onNodeWithText(Copy.Add).assertIsEnabled()
        rule.onNodeWithText(Copy.FullHint).assertDoesNotExist()
        rule.onNodeWithText(Copy.Celebration).assertDoesNotExist()
    }

    @Test
    fun emptyCopyStaysVisibleAtLargerFontScale() {
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.FontScale(1.5f) then
                    DeviceConfigurationOverride.WindowSize(DpSize(400.dp, 800.dp)),
            ) {
                SanshiTheme {
                    screen(TodayUiState(dateLabel = "10月28日 · 周三"))
                }
            }
        }
        rule.onNodeWithText(Copy.EmptyTitle).assertIsDisplayed()
        rule.onNodeWithText(Copy.Add).assertIsDisplayed()
    }

    @Test
    fun threeItemsShowPersistentHintAndDisableAdd() {
        var dismissed = false
        var addClicks = 0
        rule.setContent {
            SanshiTheme {
                screen(
                    state = TodayUiState(
                        dateLabel = "10月28日 · 周三",
                        tasks = listOf(
                            Task("1", "买牛奶", false),
                            Task("2", "回邮件", false),
                            Task("3", "散步", false),
                        ),
                    ),
                    onAddClick = { addClicks += 1 },
                    onDismissFullHint = { dismissed = true },
                )
            }
        }
        rule.onNodeWithText("买牛奶").assertIsDisplayed()
        rule.onNodeWithText("散步").assertIsDisplayed()
        rule.onNodeWithText(Copy.FullHint).assertIsDisplayed()
        rule.onNodeWithText(Copy.Add).assertIsNotEnabled()
        rule.onNodeWithText(Copy.Add).performClick()
        assertEquals(0, addClicks)
        rule.onNodeWithContentDescription(Copy.DismissHint).performClick()
        assertTrue(dismissed)
    }

    @Test
    fun completedListShowsStaticCelebrationBar() {
        rule.setContent {
            SanshiTheme {
                screen(
                    TodayUiState(
                        dateLabel = "10月28日 · 周三",
                        tasks = listOf(Task("1", "写完", true), Task("2", "交包", true)),
                        celebrationGeneration = 0,
                    ),
                )
            }
        }
        rule.onNodeWithText(Copy.Celebration).assertIsDisplayed()
        rule.onNodeWithText(Copy.EmptyTitle).assertDoesNotExist()
    }

    @Test
    fun rowClickCheckboxAndDeleteAreWired() {
        var edited: String? = null
        var toggled: Pair<String, Boolean>? = null
        var deleted: String? = null
        var confirmed = false
        rule.setContent {
            SanshiTheme {
                screen(
                    state = TodayUiState(
                        dateLabel = "10月28日 · 周三",
                        tasks = listOf(Task("1", "买牛奶", false)),
                        pendingDeleteId = "1",
                    ),
                    onTaskClick = { edited = it },
                    onToggle = { id, done -> toggled = id to done },
                    onDeleteClick = { deleted = it },
                    onConfirmDelete = { confirmed = true },
                )
            }
        }
        rule.onNodeWithText("买牛奶").performClick()
        assertEquals("1", edited)
        rule.onAllNodes(isToggleable())[0].performClick()
        assertEquals("1" to true, toggled)
        rule.onAllNodesWithContentDescription(Copy.Delete)[0].performClick()
        assertEquals("1", deleted)
        rule.onNodeWithText(Copy.DeleteTitle).assertIsDisplayed()
        rule.onNodeWithText(Copy.Delete).assertIsDisplayed()
        rule.onNodeWithText(Copy.Delete).performClick()
        assertTrue(confirmed)
    }

    @Test
    fun blankEditorCannotSaveUntilThereIsText() {
        var saved: String? = null
        rule.setContent {
            SanshiTheme {
                screen(
                    state = TodayUiState(
                        dateLabel = "10月28日 · 周三",
                        editor = EditorRequest(taskId = null, initialText = ""),
                    ),
                    onSaveEditor = { saved = it },
                )
            }
        }
        rule.onNodeWithText(Copy.Placeholder).assertIsDisplayed()
        rule.onNodeWithText(Copy.Save).assertIsNotEnabled()
        rule.onNode(hasSetTextAction()).performTextInput("买牛奶")
        rule.onNodeWithText(Copy.Save).assertIsEnabled()
        rule.onNodeWithText(Copy.Save).performClick()
        assertEquals("买牛奶", saved)
        rule.onNode(hasSetTextAction()).assertTextContains("买牛奶")
    }

    @Composable
    private fun screen(
        state: TodayUiState,
        onAddClick: () -> Unit = {},
        onTaskClick: (String) -> Unit = {},
        onToggle: (String, Boolean) -> Unit = { _, _ -> },
        onDeleteClick: (String) -> Unit = {},
        onConfirmDelete: () -> Unit = {},
        onDismissDelete: () -> Unit = {},
        onDismissFullHint: () -> Unit = {},
        onDismissEditor: () -> Unit = {},
        onSaveEditor: (String) -> Unit = {},
    ) {
        SanshiScreen(
            state = state,
            reduceMotion = true,
            onAddClick = onAddClick,
            onTaskClick = onTaskClick,
            onToggle = onToggle,
            onDeleteClick = onDeleteClick,
            onConfirmDelete = onConfirmDelete,
            onDismissDelete = onDismissDelete,
            onDismissFullHint = onDismissFullHint,
            onDismissEditor = onDismissEditor,
            onSaveEditor = onSaveEditor,
        )
    }
}
