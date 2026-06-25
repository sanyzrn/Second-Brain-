package ir.dbsgraphic.secondbrain.feature.project

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.dbsgraphic.secondbrain.core.data.ItemRepository
import ir.dbsgraphic.secondbrain.core.data.ProjectRepository
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import ir.dbsgraphic.secondbrain.core.database.entity.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A hub tab. ALL shows everything; the rest filter Items by type — derived,
 *  never duplicated (Constitution §4). */
enum class ProjectTab(val typeValue: String?, val label: String) {
    ALL(null, "همه"),
    TASK("task", "کارها"),
    NOTE("note", "یادداشت‌ها"),
    IDEA("idea", "ایده‌ها"),
    DOC("doc", "اسناد"),
}

@Immutable
data class ProjectUiState(
    val project: Project? = null,
    val tab: ProjectTab = ProjectTab.ALL,
    val items: List<Item> = emptyList(),
) {
    val visibleItems: List<Item>
        get() = tab.typeValue?.let { t -> items.filter { it.type == t } } ?: items
}

@HiltViewModel
class ProjectViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    projectRepository: ProjectRepository,
    private val itemRepository: ItemRepository,
) : ViewModel() {

    private val projectId: String = checkNotNull(savedStateHandle["projectId"])
    private val tab = MutableStateFlow(ProjectTab.ALL)

    val uiState: StateFlow<ProjectUiState> = combine(
        projectRepository.observeProject(projectId),
        itemRepository.observeByProject(projectId),
        tab,
    ) { project, items, selectedTab ->
        ProjectUiState(project = project, tab = selectedTab, items = items)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProjectUiState())

    fun selectTab(newTab: ProjectTab) {
        tab.value = newTab
    }

    fun trash(id: String) = viewModelScope.launch { itemRepository.trash(id) }
}
