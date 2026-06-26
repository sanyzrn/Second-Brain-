package ir.dbsgraphic.secondbrain.feature.itemdetail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.dbsgraphic.secondbrain.core.data.ItemRepository
import ir.dbsgraphic.secondbrain.core.data.ItemType
import ir.dbsgraphic.secondbrain.core.data.ProjectRepository
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import ir.dbsgraphic.secondbrain.core.database.entity.Project
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ConnectedItem(val item: Item, val outgoing: Boolean)

@Immutable
data class ItemDetailUiState(
    val item: Item? = null,
    val projects: List<Project> = emptyList(),
    val connections: List<ConnectedItem> = emptyList(),
)

sealed interface DetailEvent {
    data object Saved : DetailEvent
    data object Trashed : DetailEvent
    data object Restored : DetailEvent
    data object Deleted : DetailEvent
    data class Failed(val message: String) : DetailEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val itemRepository: ItemRepository,
    projectRepository: ProjectRepository,
) : ViewModel() {

    private val itemId: String = checkNotNull(savedStateHandle["itemId"])

    private val connections: Flow<List<ConnectedItem>> = combine(
        itemRepository.observeOutgoing(itemId),
        itemRepository.observeBacklinks(itemId),
    ) { outgoing, backlinks ->
        (outgoing.map { ConnectedItem(it, outgoing = true) } +
            backlinks.map { ConnectedItem(it, outgoing = false) })
            .distinctBy { it.item.id }
    }

    val uiState: StateFlow<ItemDetailUiState> = combine(
        itemRepository.observeById(itemId),
        projectRepository.observeProjects(),
        connections,
    ) { item, projects, conns ->
        ItemDetailUiState(item = item, projects = projects, connections = conns)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ItemDetailUiState())

    // ── Link picker (search over FTS, excluding self) ───────────────────────
    private val _linkQuery = MutableStateFlow("")
    val linkQuery: StateFlow<String> = _linkQuery.asStateFlow()

    val linkResults: StateFlow<List<Item>> = _linkQuery
        .debounce(120)
        .flatMapLatest { q -> itemRepository.search(q) }
        .map { results -> results.filter { it.id != itemId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onLinkQueryChange(value: String) {
        _linkQuery.value = value
    }

    fun addLink(toId: String) {
        viewModelScope.launch {
            runCatching { itemRepository.link(itemId, toId) }
            _linkQuery.value = ""
        }
    }

    fun removeLink(connected: ConnectedItem) {
        viewModelScope.launch {
            if (connected.outgoing) itemRepository.unlink(itemId, connected.item.id)
            else itemRepository.unlink(connected.item.id, itemId)
        }
    }

    private val _events = Channel<DetailEvent>(Channel.BUFFERED)
    val events: Flow<DetailEvent> = _events.receiveAsFlow()

    fun save(content: String, type: ItemType?, projectId: String?, tags: List<String>) {
        viewModelScope.launch {
            runCatching {
                if (content.isNotBlank()) itemRepository.updateContent(itemId, content)
                if (type != null) itemRepository.triage(itemId, type.value, projectId, tags)
            }.onSuccess { _events.send(DetailEvent.Saved) }
                .onFailure { _events.send(DetailEvent.Failed("ذخیره نشد")) }
        }
    }

    fun trash() = viewModelScope.launch {
        runCatching { itemRepository.trash(itemId) }
            .onSuccess { _events.send(DetailEvent.Trashed) }
            .onFailure { _events.send(DetailEvent.Failed("حذف نشد")) }
    }

    fun restore() = viewModelScope.launch {
        runCatching { itemRepository.restore(itemId) }
            .onSuccess { _events.send(DetailEvent.Restored) }
            .onFailure { _events.send(DetailEvent.Failed("بازگردانی نشد")) }
    }

    fun deleteForever() = viewModelScope.launch {
        runCatching { itemRepository.deleteForever(itemId) }
            .onSuccess { _events.send(DetailEvent.Deleted) }
            .onFailure { _events.send(DetailEvent.Failed("حذف نشد")) }
    }
}
