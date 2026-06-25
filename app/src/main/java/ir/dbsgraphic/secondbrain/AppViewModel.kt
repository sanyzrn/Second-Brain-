package ir.dbsgraphic.secondbrain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.dbsgraphic.secondbrain.core.data.SettingsRepository
import ir.dbsgraphic.secondbrain.core.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppState(
    val loading: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val onboardingComplete: Boolean = false,
)

@HiltViewModel
class AppViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<AppState> = combine(
        settingsRepository.observeThemeMode(),
        settingsRepository.observeOnboardingComplete(),
    ) { theme, onboarding ->
        AppState(loading = false, themeMode = theme, onboardingComplete = onboarding)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppState())

    fun completeOnboarding() {
        viewModelScope.launch { settingsRepository.setOnboardingComplete(true) }
    }
}
