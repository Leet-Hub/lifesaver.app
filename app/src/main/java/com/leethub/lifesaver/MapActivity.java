package com.leethub.lifesaver;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.leethub.lifesaver.models.BloodRequest;
import com.leethub.lifesaver.models.User;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private Spinner spinnerFilterBlood;
    private Button btnSearchNearby;
    private FloatingActionButton fabMyLocation;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        spinnerFilterBlood = findViewById(R.id.spinnerFilterBlood);
        btnSearchNearby = findViewById(R.id.btnSearchNearby);
        fabMyLocation = findViewById(R.id.fabMyLocation);

        String[] bloodGroups = {"All", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bloodGroups);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterBlood.setAdapter(adapter);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnSearchNearby.setOnClickListener(v -> refreshMapData());
        fabMyLocation.setOnClickListener(v -> getCurrentLocation());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        getCurrentLocation();
        refreshMapData();
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f));
            }
        });
    }

    private void refreshMapData() {
        mMap.clear();
        fetchDonors();
        fetchBloodRequests();
    }

    private void fetchDonors() {
        String selectedBlood = spinnerFilterBlood.getSelectedItem().toString();
        db.collection("users")
                .whereEqualTo("availability", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        if (user.latitude != 0 && user.longitude != 0) {
                            if (selectedBlood.equals("All") || user.bloodGroup.equals(selectedBlood)) {
                                LatLng donorLoc = new LatLng(user.latitude, user.longitude);
                                mMap.addMarker(new MarkerOptions()
                                        .position(donorLoc)
                                        .title("DONOR: " + user.name + " (" + user.bloodGroup + ")")
                                        .snippet("Phone: " + user.phone)
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                            }
                        }
                    }
                });
    }

    private void fetchBloodRequests() {
        String selectedBlood = spinnerFilterBlood.getSelectedItem().toString();
        db.collection("blood_requests")
                .whereEqualTo("status", "Pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        BloodRequest request = document.toObject(BloodRequest.class);
                        if (request.latitude != 0 && request.longitude != 0) {
                            if (selectedBlood.equals("All") || request.bloodGroup.equals(selectedBlood)) {
                                LatLng requestLoc = new LatLng(request.latitude, request.longitude);
                                mMap.addMarker(new MarkerOptions()
                                        .position(requestLoc)
                                        .title("HELP: " + request.patientName + " (" + request.bloodGroup + ")")
                                        .snippet("Hospital: " + request.hospitalName + " | Contact: " + request.contactNumber)
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                            }
                        }
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            }
        }
    }
}
