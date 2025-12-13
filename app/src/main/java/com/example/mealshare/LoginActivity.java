package com.example.mealshare;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

public class LoginActivity extends AppCompatActivity {
    private AuthViewModel authViewModel;
    private EditText login_email, login_password;
    private Button login_btn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            return insets;
        });

        TextView toRegister = findViewById(R.id.toRegister);
        toRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
                finish();
            }
        });

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Observe messages from ViewModel to show toasts
        authViewModel.getMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
                authViewModel.clearMessage();
            }
        });

        // Observe authentication errors to show field-specific errors
        authViewModel.getAuthError().observe(this, error -> {
            if (error != null) {
                // Clear previous errors
                login_email.setError(null);
                login_password.setError(null);
                
                // Set error on appropriate field based on error type
                switch (error) {
                    case INVALID_EMAIL:
                        login_email.setError(error.getMessage());
                        login_email.requestFocus();
                        break;
                    case WRONG_PASSWORD:
                        login_password.setError(error.getMessage());
                        login_password.requestFocus();
                        break;
                    case USER_NOT_FOUND:
                        login_email.setError(error.getMessage());
                        login_email.requestFocus();
                        break;
                    case INVALID_CREDENTIALS:
                        login_password.setError(error.getMessage());
                        login_password.requestFocus();
                        break;
                    // For other errors, just show toast (already handled by message observer)
                    default:
                        break;
                }
                authViewModel.clearAuthError();
            }
        });

        // Observe authenticated user -> navigate to MainActivity on success
        authViewModel.getAuthUser().observe(this, user -> {
            if (user != null) {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
                authViewModel.clearAuthUser();
            }
        });
        login_email = findViewById(R.id.login_email);
        login_password = findViewById(R.id.login_password);
        login_btn = findViewById(R.id.login_btn);

        login_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = login_email.getText().toString();
                String password = login_password.getText().toString();

                if(!email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                   if(!password.isEmpty()) {
                        // Delegate sign-in to ViewModel
                        authViewModel.login(email, password);
                   }else{
                       login_password.setError("Empty Fields Are not Allowed");
                   }
                }else if(email.isEmpty()){
                    login_email.setError("Email cannot be empty.");
                }else{
                    login_email.setError("Please enter a valid email.");
                }
            }
        });
    }
}
