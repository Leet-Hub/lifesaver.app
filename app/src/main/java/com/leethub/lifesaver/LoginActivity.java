package com.leethub.lifesaver;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        // Animate the overlap card on entry
        android.view.View loginCard = findViewById(R.id.loginCard);
        if (loginCard != null) {
            loginCard.setVisibility(android.view.View.INVISIBLE);
            loginCard.postDelayed(() -> {
                loginCard.setVisibility(android.view.View.VISIBLE);
                loginCard.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.fade_in_up));
            }, 300);
        }

        // Start background gradient animation
        android.view.View heroHeader = findViewById(R.id.heroHeader);
        if (heroHeader != null) {
            android.graphics.drawable.AnimationDrawable animationDrawable = (android.graphics.drawable.AnimationDrawable) heroHeader.getBackground();
            animationDrawable.setEnterFadeDuration(2000);
            animationDrawable.setExitFadeDuration(2000);
            animationDrawable.start();
        }
        
        String registerText = "Don't have an account? Register";
        android.text.SpannableString spannableString = new android.text.SpannableString(registerText);
        int startIndex = registerText.indexOf("Register");
        int endIndex = startIndex + "Register".length();
        spannableString.setSpan(new android.text.style.ForegroundColorSpan(androidx.core.content.ContextCompat.getColor(this, R.color.colorPrimary)), startIndex, endIndex, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvRegister.setText(spannableString);

        btnLogin.setOnClickListener(v -> {
            v.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.button_click));
            loginUser();
        });

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }

        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                        btnLogin.setEnabled(true);
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }
}
