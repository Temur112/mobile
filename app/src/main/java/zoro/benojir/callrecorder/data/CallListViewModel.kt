package zoro.benojir.callrecorder.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CallListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CallRecordRepository(application)

    // 🔹 Define StateFlow first
    private val _callRecords = MutableStateFlow<List<CallRecordEntity>>(emptyList())
    val callRecords: StateFlow<List<CallRecordEntity>> = _callRecords

    // 🔹 Then convert it to LiveData (for Java compatibility)
    val callRecordsLiveData = callRecords.asLiveData()

    fun loadAllRecords() {
        viewModelScope.launch {
            val data = repository.getAll()
            _callRecords.value = data
        }
    }
}
