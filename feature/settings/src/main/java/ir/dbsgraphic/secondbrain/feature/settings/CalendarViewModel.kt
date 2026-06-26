package ir.dbsgraphic.secondbrain.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.dbsgraphic.secondbrain.core.calendar.CalendarInfo
import ir.dbsgraphic.secondbrain.core.calendar.DeviceEvent
import ir.dbsgraphic.secondbrain.core.data.CalendarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CalendarUiState(
    val canWrite: Boolean = false,
    val calendars: List<CalendarInfo> = emptyList(),
    val selectedCalendarId: Long? = null,
    val upcoming: List<DeviceEvent> = emptyList(),
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    /** Re-read permission, calendars and upcoming events. Call on entry and after a grant. */
    fun refresh() {
        viewModelScope.launch {
            val canWrite = calendarRepository.canWriteCalendar()
            val calendars = if (calendarRepository.canReadCalendar()) calendarRepository.writableCalendars() else emptyList()
            val upcoming = if (calendarRepository.canReadCalendar()) calendarRepository.upcomingEvents() else emptyList()
            _state.update { current ->
                current.copy(
                    canWrite = canWrite,
                    calendars = calendars,
                    selectedCalendarId = current.selectedCalendarId ?: calendars.firstOrNull()?.id,
                    upcoming = upcoming,
                )
            }
        }
    }

    fun selectCalendar(id: Long) {
        _state.update { it.copy(selectedCalendarId = id) }
    }

    fun mirrorReminders() {
        val calendarId = _state.value.selectedCalendarId ?: run {
            _status.value = "ابتدا یک تقویم انتخاب کن"
            return
        }
        viewModelScope.launch {
            val result = runCatching { calendarRepository.mirrorReminders(calendarId) }.getOrNull()
            _status.value = if (result == null) {
                "هم‌گام‌سازی ناموفق بود"
            } else {
                "‫${result.created} رویداد ساخته شد، ${result.updated} به‌روزرسانی شد‬"
            }
            refresh()
        }
    }

    fun importEvent(event: DeviceEvent) {
        viewModelScope.launch {
            runCatching { calendarRepository.importDeviceEvent(event) }
            _status.value = "به صندوق اضافه شد"
        }
    }

    fun exportIcs(uri: Uri) {
        viewModelScope.launch {
            val ok = runCatching {
                val text = calendarRepository.exportIcs()
                context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                    ?: error("no stream")
                true
            }.getOrDefault(false)
            _status.value = if (ok) "فایل تقویم ساخته شد" else "برون‌بری ناموفق بود"
        }
    }

    fun importIcs(uri: Uri) {
        viewModelScope.launch {
            val count = runCatching {
                val text = context.contentResolver.openInputStream(uri)?.use {
                    it.readBytes().toString(Charsets.UTF_8)
                } ?: error("no stream")
                calendarRepository.importIcs(text)
            }.getOrNull()
            _status.value = if (count == null) "فایل نامعتبر بود" else "‫$count رویداد به صندوق اضافه شد‬"
        }
    }

    fun clearStatus() {
        _status.value = null
    }
}
