package ir.dbsgraphic.secondbrain.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.dbsgraphic.secondbrain.core.data.BackupManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataViewModel @Inject constructor(
    private val backupManager: BackupManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    fun export(uri: Uri) {
        viewModelScope.launch {
            val ok = runCatching {
                val bytes = backupManager.exportAll()
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    ?: error("no stream")
                true
            }.getOrDefault(false)
            _status.value = if (ok) "برون‌بری با موفقیت انجام شد" else "برون‌بری ناموفق بود"
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            val ok = runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("no stream")
                backupManager.importAll(bytes)
                true
            }.getOrDefault(false)
            _status.value = if (ok) "درون‌ریزی انجام شد" else "فایل نامعتبر یا متعلق به دستگاه دیگری بود"
        }
    }

    fun clearStatus() {
        _status.value = null
    }
}
