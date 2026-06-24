package ir.dbsgraphic.secondbrain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import dagger.hilt.android.AndroidEntryPoint
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // The whole app is laid out for Persian first: RTL is the host,
            // not a mirror applied after the fact (Constitution §14, design spine).
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                SecondBrainTheme {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun AppRoot() {
    // The Inbox is the home — everything starts here (Constitution §3).
    SecondBrainNavHost()
}
