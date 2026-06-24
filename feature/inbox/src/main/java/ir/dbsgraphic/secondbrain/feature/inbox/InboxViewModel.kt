package ir.dbsgraphic.secondbrain.feature.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.dbsgraphic.secondbrain.core.data.ItemRepository
import ir.dbsgraphic.secondbrain.core.data.ProjectRepository
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One-off feedback the screen consumes (toasts/haptics); never replayed. */
sealed interface InboxEvent {
    data object Captured : InboxEvent
    data object Triaged : InboxEvent
    data class Failed(val message: String) : InboxEvent
}

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val repository: ItemRepository,
    private val projectRepository: ProjectRepository,
) : ViewModel() {

    private val draft = MutableStateFlow("")
    private val isSaving = MutableStateFlow(false)
    private val triageTarget = MutableStateFlow<Item?>(null)

    private val content: Flow<InboxContent> = repository.observeInbox()
        .map { items ->
            if (items.isEmpty()) InboxContent.Empty else InboxContent.Items(items)
        }
        .catch { emit(InboxContent.Error("خواندن صندوق ممکن نشد")) }

    val uiState: StateFlow<InboxUiState> =
        combine(
            content,
            draft,
            isSaving,
            projectRepository.observeProjects(),
            triageTarget,
        ) { c, d, s, projects, target ->
            InboxUiState(
                content = c,
                draft = d,
                isSaving = s,
                projects = projects,
                triageTarget = target,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = InboxUiState(),
        )

    private val _events = Channel<InboxEvent>(Channel.BUFFERED)
    val events: Flow<InboxEvent> = _events.receiveAsFlow()

    fun onDraftChange(text: String) {
        draft.value = text
    }

    /** Capture the current draft. Faster than forgetting (Constitution §2). */
    fun capture() {
        val text = draft.value.trim()
        if (text.isEmpty() || isSaving.value) return

        viewModelScope.launch {
            isSaving.value = true
            try {
                repository.capture(text)
                draft.value = ""
                _events.send(InboxEvent.Captured)
            } catch (e: Exception) {
                _events.send(InboxEvent.Failed("ثبت نشد، دوباره تلاش کن"))
            } finally {
                isSaving.value = false
            }
        }
    }

    fun openTriage(item: Item) {
        triageTarget.value = item
    }

    fun dismissTriage() {
        triageTarget.value = null
    }

    /** Triage the open item, then let it move out of the Inbox (§3). */
    fun confirmTriage(type: ItemType, projectId: String?, tags: List<String>) {
        val target = triageTarget.value ?: return
        viewModelScope.launch {
            try {
                repository.triage(target.id, type.value, projectId, tags)
                triageTarget.value = null
                _events.send(InboxEvent.Triaged)
            } catch (e: Exception) {
                _events.send(InboxEvent.Failed("مرتب‌سازی ممکن نشد"))
            }
        }
    }

    fun createProject(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            runCatching { projectRepository.createProject(trimmed) }
        }
    }
}
