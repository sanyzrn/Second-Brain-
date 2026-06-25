package ir.dbsgraphic.secondbrain.feature.timeline

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
class TimelineViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
) : ViewModel() {

    val items: StateFlow<List<Item>> = itemRepository.observeTimeline()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun trash(id: String) = viewModelScope.launch { itemRepository.trash(id) }
}
