package zoro.benojir.callrecorder.networking

import android.content.Context
import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import zoro.benojir.callrecorder.helpers.CustomFunctions

object ApiClient {
    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String? = null

    fun getInstance(context: Context): ApiService {
        var baseUrl = CustomFunctions.getServerUrl(context)
        if (baseUrl.isNullOrEmpty()) {
            baseUrl = "https://default-server.com/"
        }
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/"
        }

        if (retrofit == null || currentBaseUrl != baseUrl) {
            currentBaseUrl = baseUrl
            Log.d("ApiClient", "Building Retrofit with baseUrl: $currentBaseUrl")

            retrofit = Retrofit.Builder()
                .baseUrl(currentBaseUrl!!)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        return retrofit!!.create(ApiService::class.java)
    }
}
