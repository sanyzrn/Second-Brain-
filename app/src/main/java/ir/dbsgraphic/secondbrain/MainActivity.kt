package ir.dbsgraphic.secondbrain

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
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

        handleShare(intent)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShare(intent)
    }

    /** Shared-from-another-app capture (text or image) → Inbox. */
    private fun handleShare(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        if (appViewModel.ingestShareIntent(intent)) {
            Toast.makeText(this, "به صندوق ورودی اضافه شد", Toast.LENGTH_SHORT).show()
        }
    }
}
