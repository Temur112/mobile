package zoro.benojir.callrecorder.networking

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.POST
import retrofit2.http.FormUrlEncoded


data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val status: String,
    val token: String
)


interface ApiService {
    @POST("/api/call/v1/auth")
    fun login(
        @Body request: LoginRequest
    ): Call<LoginResponse>
}