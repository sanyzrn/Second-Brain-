package ir.dbsgraphic.secondbrain.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.dbsgraphic.secondbrain.core.data.ItemRepository
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val repository: ItemRepository,
) : ViewModel() {

    val items: StateFlow<List<Item>> = repository.observeTrash()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun restore(id: String) = viewModelScope.launch { repository.restore(id) }
    fun deleteForever(id: String) = viewModelScope.launch { repository.deleteForever(id) }
    fun emptyTrash() = viewModelScope.launch { repository.emptyTrash() }
}
