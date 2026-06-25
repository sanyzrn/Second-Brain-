package ir.dbsgraphic.secondbrain.feature.project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dbsgraphic.secondbrain.core.database.entity.Project
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbHairline
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbText
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbTextButton
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme
import ir.dbsgraphic.secondbrain.core.designsystem.util.relativeTimeFa

@Composable
fun ProjectsRoute(
    onOpenProject: (String) -> Unit,
    viewModel: ProjectsViewModel = hiltViewModel(),
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    ProjectsScreen(projects = projects, onOpenProject = onOpenProject)
}

@Composable
fun ProjectsScreen(
    projects: List<Project>,
    onOpenProject: (String) -> Unit,
) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            )
            .padding(horizontal = space.xl),
    ) {
        if (projects.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
            ) {
                SbText(text = "هنوز پروژه‌ای نساخته‌ای.", style = type.title)
                Spacer(Modifier.height(space.sm))
                SbText(
                    text = "موقع مرتب‌کردن یک آیتم در صندوق، می‌توانی پروژه بسازی.",
                    style = type.body,
                    color = colors.muted,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = projects, key = { it.id }) { project ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenProject(project.id) }
                            .padding(vertical = space.lg),
                    ) {
                        SbText(text = project.name, style = type.bodyLarge)
                        Spacer(Modifier.height(space.xs))
                        SbText(
                            text = relativeTimeFa(project.updatedAt),
                            style = type.monoSmall,
                            color = colors.muted,
                        )
                    }
                    SbHairline()
                }
            }
        }
    }
}

/** Shared top row (title + back) used by the Project detail screen. */
@Composable
internal fun TopRow(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SbText(text = title, style = SecondBrainTheme.type.title)
        SbTextButton(label = "بازگشت", onClick = onBack)
    }
}
