package ir.dbsgraphic.secondbrain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import ir.dbsgraphic.secondbrain.core.data.ThemeMode
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }

        enableEdgeToEdge()
        setContent {
            val state by appViewModel.state.collectAsStateWithLifecycle()
            keepSplash = state.loading

            val dark = when (state.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            // Persian is the host: RTL is the layout direction, not a mirror (§14).
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                SecondBrainTheme(darkTheme = dark) {
                    if (!state.loading) {
                        SecondBrainNavHost(
                            startDestination = if (state.onboardingComplete) Routes.MAIN else Routes.ONBOARDING,
                            onOnboardingComplete = appViewModel::completeOnboarding,
                        )
                    }
                }
            }
        }
    }
}
