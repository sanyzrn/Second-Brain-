package ir.dbsgraphic.secondbrain

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ir.dbsgraphic.secondbrain.feature.itemdetail.ItemDetailRoute
import ir.dbsgraphic.secondbrain.feature.onboarding.OnboardingRoute
import ir.dbsgraphic.secondbrain.feature.project.ProjectRoute
import ir.dbsgraphic.secondbrain.feature.reminders.RemindersRoute
import ir.dbsgraphic.secondbrain.feature.search.SearchRoute
import ir.dbsgraphic.secondbrain.feature.settings.AboutRoute
import ir.dbsgraphic.secondbrain.feature.settings.AiSettingsRoute
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
    const val ITEM = "item/{itemId}"
    const val REMINDERS = "reminders"
    fun project(id: String) = "project/$id"
    fun item(id: String) = "item/$id"
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
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
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

        composable(Routes.SETTINGS) {
            SettingsRoute(
                onBack = { navController.popBackStack() },
                onOpenAbout = { navController.navigate(Routes.ABOUT) },
                onOpenAi = { navController.navigate(Routes.AI) },
                onOpenTrash = { navController.navigate(Routes.TRASH) },
                onOpenData = { navController.navigate(Routes.DATA) },
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
    }
}
