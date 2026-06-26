package ir.dbsgraphic.secondbrain

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
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

            // Ask for notification permission once (Android 13+) so reminders can show.
            val context = LocalContext.current
            val notifPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { /* result ignored; reminders still schedule */ }
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            val dark = when (state.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            // Status/nav bar icon contrast follows the resolved theme (review fix).
            SideEffect {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
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
