package com.zts.sanshi.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zts.sanshi.ui.theme.Motion

object Routes {
    const val Today = "today"
    const val Pool = "pool"
    const val History = "history"
    const val Settings = "settings"
    const val DisposeEach = "dispose_each"
}

@Composable
fun SanshiRoute(viewModel: TodayViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val reduceMotion = rememberReduceMotion()
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = Routes.Today,
        modifier = modifier,
        enterTransition = {
            fadeIn(tween(Motion.ms(200, reduceMotion), easing = Motion.EnterEasing)) +
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(Motion.ms(200, reduceMotion), easing = Motion.EnterEasing),
                )
        },
        exitTransition = {
            fadeOut(tween(Motion.ms(180, reduceMotion), easing = Motion.ExitEasing))
        },
        popEnterTransition = {
            fadeIn(tween(Motion.ms(200, reduceMotion), easing = Motion.EnterEasing))
        },
        popExitTransition = {
            fadeOut(tween(Motion.ms(180, reduceMotion), easing = Motion.ExitEasing)) +
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(Motion.ms(180, reduceMotion), easing = Motion.ExitEasing),
                )
        },
    ) {
        composable(Routes.Today) {
            SanshiScreen(
                state = state,
                reduceMotion = reduceMotion,
                onAddClick = viewModel::onAddClick,
                onTaskClick = viewModel::onTaskClick,
                onToggle = viewModel::onToggle,
                onDeleteClick = viewModel::onDeleteClick,
                onConfirmDelete = { stop -> viewModel.onConfirmDelete(stop) },
                onDismissDelete = viewModel::onDismissDelete,
                onDismissFullHint = viewModel::onDismissFullHint,
                onDismissEditor = viewModel::onDismissEditor,
                onSaveEditor = viewModel::onSaveEditor,
                onOpenPool = { nav.navigate(Routes.Pool) },
                onOpenHistory = { nav.navigate(Routes.History) },
                onRollTomorrow = viewModel::onRollTomorrow,
                onMoveToPool = viewModel::onMoveToPool,
                onAdmitWaiting = viewModel::onAdmitFirstWaiting,
                onDisposeAllRoll = viewModel::onDisposeAllRoll,
                onDisposeEach = {
                    viewModel.onDisposeEach()
                    nav.navigate(Routes.DisposeEach)
                },
                onDisposeAllDeleteAsk = viewModel::onDisposeAllDeleteAsk,
                onDisposeAllDeleteConfirm = viewModel::onDisposeAllDeleteConfirm,
                onDismissDeleteAllConfirm = viewModel::onDismissDeleteAllConfirm,
                onClearSnack = viewModel::clearSnack,
            )
        }
        composable(Routes.Pool) {
            PoolScreen(
                state = state,
                reduceMotion = reduceMotion,
                onBack = { nav.popBackStack() },
                onAddClick = viewModel::onAddPoolClick,
                onItemClick = viewModel::onPoolClick,
                onPromote = viewModel::onPromotePool,
                onDelete = viewModel::onDeletePoolClick,
                onConfirmDelete = { viewModel.onConfirmDelete(false) },
                onDismissDelete = viewModel::onDismissDelete,
                onDismissEditor = viewModel::onDismissEditor,
                onSaveEditor = viewModel::onSaveEditor,
                onClearSnack = viewModel::clearSnack,
            )
        }
        composable(Routes.History) {
            HistoryScreen(
                state = state,
                onBack = { nav.popBackStack() },
                onOpenSettings = { nav.navigate(Routes.Settings) },
                onOpenDay = viewModel::onOpenHistoryDay,
                onCloseDay = viewModel::onCloseHistoryDay,
            )
        }
        composable(Routes.Settings) {
            SettingsScreen(
                reminder = state.reminder,
                onBack = { nav.popBackStack() },
                onSetReminder = viewModel::onSetReminder,
            )
        }
        composable(Routes.DisposeEach) {
            androidx.compose.runtime.LaunchedEffect(state.pendingIncomplete.isEmpty()) {
                if (state.pendingIncomplete.isEmpty()) {
                    nav.popBackStack(Routes.Today, inclusive = false)
                }
            }
            DisposeEachScreen(
                pending = state.pendingIncomplete,
                onBack = { nav.popBackStack() },
                onRoll = viewModel::onDisposeOneRoll,
                onPool = viewModel::onDisposeOnePool,
                onDelete = viewModel::onDisposeOneDelete,
            )
        }
    }
}
