package ir.dbsgraphic.secondbrain

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import ir.dbsgraphic.secondbrain.core.designsystem.R as DsR
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbChip
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbIconButton
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbText
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme
import ir.dbsgraphic.secondbrain.feature.inbox.InboxRoute
import ir.dbsgraphic.secondbrain.feature.project.ProjectsRoute
import ir.dbsgraphic.secondbrain.feature.timeline.TimelineRoute
import kotlinx.coroutines.launch

private val sections = listOf("خط زمان", "صندوق", "پروژه‌ها")

/**
 * The home: a swipeable pager across the three primary sections, with a quiet
 * top bar (search + settings) and a segmented switch. Inbox is the start page —
 * everything begins there (§3) — while the Timeline sits to its right (the
 * natural "first" in RTL reading order).
 */
@Composable
fun MainShell(
    onOpenProject: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { sections.size })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Two-step exit: a first back warns, a second within 2s leaves (§13 spirit —
    // protect against accidental exit).
    var lastBackAt by remember { mutableLongStateOf(0L) }
    BackHandler {
        if (pagerState.currentPage != 1) {
            scope.launch { pagerState.animateScrollToPage(1) }
        } else {
            val now = System.currentTimeMillis()
            if (now - lastBackAt < 2_000) {
                (context as? Activity)?.finish()
            } else {
                lastBackAt = now
                Toast.makeText(context, "برای خروج، دوباره برگرد", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                )
                .padding(horizontal = space.xl, vertical = space.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SbText(text = "مغز دوم", style = type.title)
            Row(verticalAlignment = Alignment.CenterVertically) {
                SbIconButton(icon = DsR.drawable.ic_search, contentDescription = "جستجو", onClick = onOpenSearch)
                Spacer(Modifier.width(space.xs))
                SbIconButton(icon = DsR.drawable.ic_settings, contentDescription = "تنظیمات", onClick = onOpenSettings)
            }
        }

        // Section switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = space.xl, vertical = space.sm),
            horizontalArrangement = Arrangement.spacedBy(space.sm),
        ) {
            sections.forEachIndexed { index, label ->
                SbChip(
                    label = label,
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                )
            }
        }

        Spacer(Modifier.height(space.sm))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            key = { it },
        ) { page ->
            when (page) {
                0 -> TimelineRoute()
                1 -> InboxRoute()
                else -> ProjectsRoute(onOpenProject = onOpenProject)
            }
        }
    }
}
