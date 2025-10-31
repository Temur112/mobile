package zoro.benojir.callrecorder.networking

import android.R
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.POST
import retrofit2.http.FormUrlEncoded


data class LoginRequest(
    val username: String,
    val password: String,
    val server: String
)

data class LoginResponse(
    val success: Boolean,
    val api_key: String

)


interface ApiService {
    @POST("/api/call/v1/auth")
    fun login(
        @Body request: LoginRequest
    ): Call<LoginResponse>
}