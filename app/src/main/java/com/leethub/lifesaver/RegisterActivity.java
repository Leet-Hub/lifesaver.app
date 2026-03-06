package com.leethub.lifesaver;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.leethub.lifesaver.models.User;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone, etCity, etPassword;
    private Spinner spinnerBloodGroup;
    private Button btnRegister;
    private TextView tvLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etCity = findViewById(R.id.etCity);
        etPassword = findViewById(R.id.etPassword);
        spinnerBloodGroup = findViewById(R.id.spinnerBloodGroup);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        // Animate the overlap card on entry
        android.view.View registerCard = findViewById(R.id.registerCard);
        if (registerCard != null) {
            registerCard.setVisibility(android.view.View.INVISIBLE);
            registerCard.postDelayed(() -> {
                registerCard.setVisibility(android.view.View.VISIBLE);
                registerCard.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.fade_in_up));
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

        String loginText = "Already have an account? Login";
        android.text.SpannableString spannableString = new android.text.SpannableString(loginText);
        int startIndex = loginText.indexOf("Login");
        int endIndex = startIndex + "Login".length();
        spannableString.setSpan(new android.text.style.ForegroundColorSpan(androidx.core.content.ContextCompat.getColor(this, R.color.colorPrimary)), startIndex, endIndex, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvLogin.setText(spannableString);

        // Setup Blood Group Spinner
        String[] bloodGroups = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bloodGroups);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodGroup.setAdapter(adapter);

        btnRegister.setOnClickListener(v -> {
            v.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.button_click));
            registerUser();
        });

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        });
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String bloodGroup = spinnerBloodGroup.getSelectedItem().toString();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Phone is required");
            return;
        }
        if (TextUtils.isEmpty(city)) {
            etCity.setError("City is required");
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        btnRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            saveUserToFirestore(firebaseUser.getUid(), name, email, phone, bloodGroup, city);
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                        btnRegister.setEnabled(true);
                    }
                });
    }

    private void saveUserToFirestore(String userId, String name, String email, String phone, String bloodGroup, String city) {
        User user = new User(userId, name, email, phone, bloodGroup, city);

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Error saving data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnRegister.setEnabled(true);
                });
    }
}
