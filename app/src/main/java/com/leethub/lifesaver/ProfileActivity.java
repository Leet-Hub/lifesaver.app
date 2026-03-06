package com.leethub.lifesaver;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.leethub.lifesaver.models.User;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail, tvPhone, tvCity, tvBloodGroup, tvTrustScore, tvTotalDonations;
    private ShapeableImageView ivProfile;
    private Button btnLogout;
    private androidx.appcompat.widget.SwitchCompat switchAvailability;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ivProfile = findViewById(R.id.ivProfileLarge);
        tvName = findViewById(R.id.tvProfileName);
        tvEmail = findViewById(R.id.tvProfileEmail);
        tvPhone = findViewById(R.id.tvProfilePhone);
        tvCity = findViewById(R.id.tvProfileCity);
        tvBloodGroup = findViewById(R.id.tvProfileBloodGroup);
        tvTrustScore = findViewById(R.id.tvTrustScore);
        tvTotalDonations = findViewById(R.id.tvTotalDonationsProfile);
        switchAvailability = findViewById(R.id.switchAvailability);
        btnLogout = findViewById(R.id.btnLogout);

        loadProfileData();

        // Availability Toggle Listener
        switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateAvailability(isChecked);
        });

        // Back arrow
        findViewById(R.id.btnBackProfile).setOnClickListener(v -> finish());

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadProfileData() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            tvName.setText(user.name);
                            tvEmail.setText("Email: " + user.email);
                            tvPhone.setText("Phone: " + user.phone);
                            tvCity.setText("City: " + user.city);
                            tvBloodGroup.setText(user.bloodGroup);
                            tvTrustScore.setText(String.valueOf(user.trustScore));
                            tvTotalDonations.setText(String.valueOf(user.totalDonations));
                            
                            // Set initial switch state without triggering listener
                            switchAvailability.setOnCheckedChangeListener(null);
                            switchAvailability.setChecked(user.availability);
                            switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> updateAvailability(isChecked));
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateAvailability(boolean isAvailable) {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId)
                .update("availability", isAvailable)
                .addOnSuccessListener(aVoid -> {
                    String status = isAvailable ? "available" : "unavailable";
                    Toast.makeText(ProfileActivity.this, "Status updated to " + status, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Failed to update status", Toast.LENGTH_SHORT).show();
                    // Revert UI on failure
                    switchAvailability.setOnCheckedChangeListener(null);
                    switchAvailability.setChecked(!isAvailable);
                    switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> updateAvailability(isChecked));
                });
    }
}
