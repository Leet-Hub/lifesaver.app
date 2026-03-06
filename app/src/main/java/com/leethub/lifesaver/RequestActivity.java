package com.leethub.lifesaver;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.leethub.lifesaver.models.BloodRequest;

import java.util.UUID;

public class RequestActivity extends AppCompatActivity {

    private EditText etPatientName, etHospitalName, etContactNumber;
    private Spinner spinnerBloodGroup, spinnerUrgency;
    private Button btnSubmitRequest;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        etPatientName = findViewById(R.id.etPatientName);
        etHospitalName = findViewById(R.id.etHospitalName);
        etContactNumber = findViewById(R.id.etContactNumber);
        spinnerBloodGroup = findViewById(R.id.spinnerRequestBloodGroup);
        spinnerUrgency = findViewById(R.id.spinnerUrgency);
        btnSubmitRequest = findViewById(R.id.btnSubmitRequest);

        // Setup Blood Group Spinner
        String[] bloodGroups = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        ArrayAdapter<String> bloodAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bloodGroups);
        bloodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodGroup.setAdapter(bloodAdapter);

        // Setup Urgency Spinner
        String[] urgencyLevels = {"Normal", "Urgent", "Critical"};
        ArrayAdapter<String> urgencyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, urgencyLevels);
        urgencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUrgency.setAdapter(urgencyAdapter);

        btnSubmitRequest.setOnClickListener(v -> checkLocationAndSubmit());

        // Back arrow
        findViewById(R.id.btnBackRequest).setOnClickListener(v -> finish());
    }

    private void checkLocationAndSubmit() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            submitRequest();
        }
    }

    private void submitRequest() {
        String patientName = etPatientName.getText().toString().trim();
        String hospitalName = etHospitalName.getText().toString().trim();
        String contactNumber = etContactNumber.getText().toString().trim();
        String bloodGroup = spinnerBloodGroup.getSelectedItem().toString();
        String urgency = spinnerUrgency.getSelectedItem().toString();

        if (TextUtils.isEmpty(patientName) || TextUtils.isEmpty(hospitalName) || TextUtils.isEmpty(contactNumber)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitRequest.setEnabled(false);

        // Get location and then save to Firestore
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                double lat = 0.0, lng = 0.0;
                if (location != null) {
                    lat = location.getLatitude();
                    lng = location.getLongitude();
                }
                saveRequestToFirestore(patientName, bloodGroup, hospitalName, contactNumber, urgency, lat, lng);
            });
        } else {
            // Save even without location (default 0,0) or ask again
            saveRequestToFirestore(patientName, bloodGroup, hospitalName, contactNumber, urgency, 0.0, 0.0);
        }
    }

    private void saveRequestToFirestore(String patientName, String bloodGroup, String hospitalName, String contactNumber, String urgency, double lat, double lng) {
        String requestId = UUID.randomUUID().toString();
        String userId = mAuth.getCurrentUser().getUid();

        BloodRequest request = new BloodRequest(requestId, userId, patientName, bloodGroup, hospitalName, contactNumber, urgency, lat, lng);

        db.collection("blood_requests").document(requestId)
                .set(request)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RequestActivity.this, "Request Submitted Successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RequestActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSubmitRequest.setEnabled(true);
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                submitRequest();
            } else {
                Toast.makeText(this, "Location permission is required to post request on map", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
