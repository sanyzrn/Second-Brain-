package ir.dbsgraphic.secondbrain.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbPrimaryButton
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbText
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbTextButton
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme
import kotlinx.coroutines.launch

private data class Page(val title: String, val body: String)

private val pages = listOf(
    Page("ذهنت برای فکر کردن است،", "نه برای نگه‌داری. هر چیزی را در چند ثانیه ثبت کن و خیالت راحت باشد."),
    Page("اول ثبت کن، بعد مرتب.", "همه‌چیز اول به صندوق ورودی می‌رود. هر وقت خواستی، آرام مرتبش کن."),
    Page("همه‌چیز پیدا می‌شود.", "جستجوی فارسی و خط زمان، هر چیزی را که ثبت کرده‌ای برمی‌گرداند."),
)

@Composable
fun OnboardingRoute(onComplete: () -> Unit) {
    OnboardingScreen(onComplete = onComplete)
}

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = space.xl),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            SbTextButton(label = "رد کردن", onClick = onComplete, color = colors.muted)
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { index ->
            val page = pages[index]
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
            ) {
                SbText(text = page.title, style = type.display)
                Spacer(Modifier.height(space.lg))
                SbText(text = page.body, style = type.bodyLarge, color = colors.muted)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = space.lg),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(pages.size) { i ->
                val active = i == pagerState.currentPage
                Box(
                    Modifier
                        .padding(end = space.xs)
                        .size(if (active) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (active) colors.accent else colors.hairline),
                )
            }
        }

        SbPrimaryButton(
            label = if (isLast) "شروع" else "بعدی",
            onClick = {
                if (isLast) {
                    onComplete()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(space.xl))
    }
}
