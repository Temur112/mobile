package zoro.benojir.callrecorder.data

import android.content.Context

class SmsRepository(context: Context) {

    private val smsDao = AppDatabase.getInstance(context).smsDao()

    suspend fun insertSms(sms: SmsEntity): Long {
        return smsDao.insertSms(sms)
    }

    suspend fun getAllSms(limit: Int = 30, offset: Int = 0): List<SmsEntity> {
        return smsDao.getAllSms(limit, offset)
    }

    suspend fun getUnsyncedSms(): List<SmsEntity> {
        return smsDao.getUnsyncedSms()
    }

    suspend fun markAsSynced(id: Long) {
        smsDao.markAsSynced(id)
    }
}
