package zoro.benojir.callrecorder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import zoro.benojir.callrecorder.databinding.ActivityLoginBinding;
import zoro.benojir.callrecorder.helpers.CustomFunctions;
import zoro.benojir.callrecorder.networking.ApiClient;
import zoro.benojir.callrecorder.networking.ApiService;
import zoro.benojir.callrecorder.networking.LoginRequest;
import zoro.benojir.callrecorder.networking.LoginResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        if (CustomFunctions.isUserLoggedIn(this)) {
//            goToMain();
//            return;
//        }

        binding.loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = binding.usernameInput.getText().toString().trim();
                String password = binding.passwordInput.getText().toString().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    binding.errorText.setText("Please fill all fields");
                    return;
                }

                performLogin(username, password);
            }
        });
    }

    private void performLogin(String username, String password) {
        binding.errorText.setText("Logging in...");

        ApiService apiService = ApiClient.INSTANCE.getInstance();

        LoginRequest request = new LoginRequest(username, password);

        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().getToken();

                    Log.d("TOKEN", "onResponse: " + token + response);

                    // Save login + token
                    CustomFunctions.saveLoginState(LoginActivity.this, true);
                    CustomFunctions.saveToken(LoginActivity.this, token);

                    goToMain();
                } else {
                    binding.errorText.setText("Invalid credentials or server error");
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                binding.errorText.setText("Network error: " + t.getMessage());
            }
        });
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
