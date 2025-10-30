package zoro.benojir.callrecorder.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CallListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CallRecordRepository(application)

    private val _callRecords = MutableStateFlow<List<CallRecordEntity>>(emptyList())
    val callRecords: StateFlow<List<CallRecordEntity>> = _callRecords

    fun loadAllRecords() {
        viewModelScope.launch {
            val data = repository.getAll()
            _callRecords.value = data
        }
    }
}
