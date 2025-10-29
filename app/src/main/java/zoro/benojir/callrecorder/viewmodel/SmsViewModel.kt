package zoro.benojir.callrecorder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import zoro.benojir.callrecorder.data.SmsEntity
import zoro.benojir.callrecorder.data.SmsRepository

class SmsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SmsRepository(application)

    private val _smsList = MutableStateFlow<List<SmsEntity>>(emptyList())
    val smsList: StateFlow<List<SmsEntity>> = _smsList

    fun loadSms(limit: Int = 50, offset: Int = 0) {
        viewModelScope.launch {
            _smsList.value = repository.getAllSms(limit, offset)
        }
    }

    fun addSms(sender: String, receiver: String, text: String, timestamp: Long) {
        viewModelScope.launch {
            val sms = SmsEntity(
                sender = sender,
                receiver = receiver,
                text = text,
                timestamp = timestamp,
                status = "received"
            )
            repository.insertSms(sms)
            loadSms() // refresh list
        }
    }
}
