package ir.dbsgraphic.secondbrain

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ir.dbsgraphic.secondbrain.feature.inbox.InboxRoute
import ir.dbsgraphic.secondbrain.feature.project.ProjectRoute
import ir.dbsgraphic.secondbrain.feature.project.ProjectsRoute

private object Routes {
    const val INBOX = "inbox"
    const val PROJECTS = "projects"
    const val PROJECT = "project/{projectId}"
    fun project(id: String) = "project/$id"
}

/**
 * App navigation. The Inbox is the start — everything begins there (§3).
 * Triage is an in-place sheet (no route); Projects and a Project hub are the
 * two destinations this phase adds.
 */
@Composable
fun SecondBrainNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.INBOX) {
        composable(Routes.INBOX) {
            InboxRoute(
                onOpenProjects = { navController.navigate(Routes.PROJECTS) },
            )
        }
        composable(Routes.PROJECTS) {
            ProjectsRoute(
                onOpenProject = { id -> navController.navigate(Routes.project(id)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.PROJECT,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
        ) {
            ProjectRoute(onBack = { navController.popBackStack() })
        }
    }
}
