package ir.dbsgraphic.secondbrain

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbScreenHeader
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme
import ir.dbsgraphic.secondbrain.feature.finance.FinanceRoute
import ir.dbsgraphic.secondbrain.feature.goals.GoalsRoute
import ir.dbsgraphic.secondbrain.feature.habits.HabitsRoute
import ir.dbsgraphic.secondbrain.feature.itemdetail.ItemDetailRoute
import ir.dbsgraphic.secondbrain.feature.medicine.MedicineRoute
import ir.dbsgraphic.secondbrain.feature.onboarding.OnboardingRoute
import ir.dbsgraphic.secondbrain.feature.project.ProjectRoute
import ir.dbsgraphic.secondbrain.feature.reminders.RemindersRoute
import ir.dbsgraphic.secondbrain.feature.search.SearchRoute
import ir.dbsgraphic.secondbrain.feature.goals.WeeklyReviewRoute
import ir.dbsgraphic.secondbrain.feature.settings.AboutRoute
import ir.dbsgraphic.secondbrain.feature.settings.AiSettingsRoute
import ir.dbsgraphic.secondbrain.feature.settings.CalendarRoute
import ir.dbsgraphic.secondbrain.feature.settings.DataRoute
import ir.dbsgraphic.secondbrain.feature.settings.SettingsRoute
import ir.dbsgraphic.secondbrain.feature.settings.TrashRoute

object Routes {
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val PROJECTS = "projects"
    const val PROJECT = "project/{projectId}"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val AI = "ai_settings"
    const val TRASH = "trash"
    const val DATA = "data"
    const val CALENDAR = "calendar"
    const val ITEM = "item/{itemId}"
    const val REMINDERS = "reminders"
    const val REVIEW = "review"
    const val HABITS = "habits"
    const val FINANCE = "finance"
    const val MEDICINE = "medicine"
    const val GOALS = "goals"
    fun project(id: String) = "project/$id"
    fun item(id: String) = "item/$id"

    fun tracker(dest: TrackerDest): String = when (dest) {
        TrackerDest.HABITS -> HABITS
        TrackerDest.FINANCE -> FINANCE
        TrackerDest.MEDICINE -> MEDICINE
        TrackerDest.GOALS -> GOALS
    }
}

/**
 * App navigation. Onboarding (first run) → the main swipeable shell. Project
 * detail, search, settings and about are pushed on top with horizontal slides
 * — a quiet, professional spatial model (design spine: motion is purposeful).
 */
@Composable
fun SecondBrainNavHost(
    startDestination: String,
    onOnboardingComplete: () -> Unit,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingRoute(
                onComplete = {
                    onOnboardingComplete()
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.MAIN) {
            MainShell(
                onOpenProject = { id -> navController.navigate(Routes.project(id)) },
                onOpenItem = { id -> navController.navigate(Routes.item(id)) },
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onOpenReminders = { navController.navigate(Routes.REMINDERS) },
                onOpenReview = { navController.navigate(Routes.REVIEW) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenTracker = { dest -> navController.navigate(Routes.tracker(dest)) },
            )
        }

        composable(
            route = Routes.PROJECT,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
        ) {
            ProjectRoute(
                onBack = { navController.popBackStack() },
                onOpenItem = { id -> navController.navigate(Routes.item(id)) },
            )
        }

        composable(
            route = Routes.ITEM,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType }),
        ) {
            ItemDetailRoute(
                onBack = { navController.popBackStack() },
                onOpenItem = { id -> navController.navigate(Routes.item(id)) },
            )
        }

        composable(Routes.SEARCH) {
            SearchRoute(
                onBack = { navController.popBackStack() },
                onOpenItem = { id -> navController.navigate(Routes.item(id)) },
            )
        }

        composable(Routes.REMINDERS) {
            RemindersRoute(
                onBack = { navController.popBackStack() },
                onOpenItem = { id -> navController.navigate(Routes.item(id)) },
            )
        }

        composable(Routes.REVIEW) {
            WeeklyReviewRoute(onBack = { navController.popBackStack() })
        }

        composable(Routes.HABITS) {
            TrackerScaffold(title = "عادت‌ها", onBack = { navController.popBackStack() }) {
                HabitsRoute(onOpenItem = { id -> navController.navigate(Routes.item(id)) })
            }
        }

        composable(Routes.FINANCE) {
            TrackerScaffold(title = "هزینه‌ها", onBack = { navController.popBackStack() }) {
                FinanceRoute(onOpenItem = { id -> navController.navigate(Routes.item(id)) })
            }
        }

        composable(Routes.MEDICINE) {
            TrackerScaffold(title = "داروها", onBack = { navController.popBackStack() }) {
                MedicineRoute(onOpenItem = { id -> navController.navigate(Routes.item(id)) })
            }
        }

        composable(Routes.GOALS) {
            TrackerScaffold(title = "هدف‌ها", onBack = { navController.popBackStack() }) {
                GoalsRoute(onOpenItem = { id -> navController.navigate(Routes.item(id)) })
            }
        }

        composable(Routes.SETTINGS) {
            SettingsRoute(
                onBack = { navController.popBackStack() },
                onOpenAbout = { navController.navigate(Routes.ABOUT) },
                onOpenAi = { navController.navigate(Routes.AI) },
                onOpenTrash = { navController.navigate(Routes.TRASH) },
                onOpenData = { navController.navigate(Routes.DATA) },
                onOpenCalendar = { navController.navigate(Routes.CALENDAR) },
            )
        }

        composable(Routes.ABOUT) {
            AboutRoute(onBack = { navController.popBackStack() })
        }

        composable(Routes.AI) {
            AiSettingsRoute(onBack = { navController.popBackStack() })
        }

        composable(Routes.TRASH) {
            TrashRoute(onBack = { navController.popBackStack() })
        }

        composable(Routes.DATA) {
            DataRoute(onBack = { navController.popBackStack() })
        }

        composable(Routes.CALENDAR) {
            CalendarRoute(onBack = { navController.popBackStack() })
        }
    }
}

/**
 * Wraps a life-vertical (built to fill the pager page) as a pushed full-screen
 * surface: a [SbScreenHeader] on top, the tracker filling the rest. The header
 * owns the top inset; the tracker keeps its own horizontal + bottom insets.
 */
@Composable
private fun TrackerScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SecondBrainTheme.colors.background),
    ) {
        SbScreenHeader(title = title, onBack = onBack)
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}
