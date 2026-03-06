package com.leethub.lifesaver;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.leethub.lifesaver.adapters.DonorAdapter;
import com.leethub.lifesaver.models.BloodRequest;
import com.leethub.lifesaver.models.Notification;
import com.leethub.lifesaver.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements DonorAdapter.OnDonorClickListener {

    private DrawerLayout drawerLayout;
    private LinearLayout navViewCustom;
    private BottomNavigationView bottomNavigationView;
    
    private RecyclerView rvDonors;
    private DonorAdapter donorAdapter;
    private List<User> donorList;
    private List<User> filteredList;

    private EditText etSearch;
    private ImageView btnMenu, btnNotifications;
    private boolean sortAscending = true;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;

    // Header Views
    private TextView tvNavHeaderName, tvNavHeaderEmail;
    private ImageView ivNavHeaderClose;

    private FusedLocationProviderClient fusedLocationClient;

    private static final int PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }
        currentUserId = mAuth.getCurrentUser().getUid();

        // Init views
        drawerLayout = findViewById(R.id.drawer_layout);
        navViewCustom = findViewById(R.id.nav_view_custom);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        rvDonors = findViewById(R.id.rvDonors);
        etSearch = findViewById(R.id.etSearch);
        btnMenu = findViewById(R.id.btnMenu);
        btnNotifications = findViewById(R.id.btnNotifications);

        // Header view setup
        tvNavHeaderName = navViewCustom.findViewById(R.id.tvNavHeaderName);
        tvNavHeaderEmail = navViewCustom.findViewById(R.id.tvNavHeaderEmail);
        ivNavHeaderClose = navViewCustom.findViewById(R.id.ivNavHeaderClose);

        ivNavHeaderClose.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        
        btnNotifications.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, NotificationsActivity.class));
        });

        // Setup RecyclerView
        rvDonors.setLayoutManager(new LinearLayoutManager(this));
        donorList = new ArrayList<>();
        filteredList = new ArrayList<>();
        donorAdapter = new DonorAdapter(this, filteredList, this);
        rvDonors.setAdapter(donorAdapter);

        // Search logic
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterDonors(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        setupCustomDrawerClicks();
        setupBottomNavigation();
        loadCurrentUserHeader();
        fetchDonors();
        setupAppVersionFooter();
        updateFCMToken();
        requestPermissions();
        listenForNotifications();
        checkDonationEligibility();
        updateUserLocation();
    }

    private void updateUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    db.collection("users").document(currentUserId)
                            .update("latitude", location.getLatitude(),
                                    "longitude", location.getLongitude());
                }
            });
        }
    }

    private void requestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    private void checkDonationEligibility() {
        db.collection("users").document(currentUserId).get().addOnSuccessListener(documentSnapshot -> {
            User user = documentSnapshot.toObject(User.class);
            if (user != null && user.lastDonationTimestamp > 0) {
                long currentTime = System.currentTimeMillis();
                long diffInMillis = currentTime - user.lastDonationTimestamp;
                long minutesSinceLastDonation = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);

                if (minutesSinceLastDonation >= 1) {
                    sendEligibilityNotification();
                }
            }
        });
    }

    private void sendEligibilityNotification() {
        String notificationId = "eligibility_" + currentUserId;
        Notification notification = new Notification(
                notificationId,
                "SYSTEM",
                "LifeSaver",
                currentUserId,
                "Great news! You are now eligible to donate blood again and save lives!",
                System.currentTimeMillis(),
                "GENERAL",
                null
        );

        db.collection("notifications").document(notificationId).set(notification);
    }

    private void updateFCMToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            db.collection("users").document(currentUserId).update("fcmToken", token);
        });
    }

    private void listenForNotifications() {
        db.collection("notifications")
                .whereEqualTo("toUserId", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Notification notification = dc.getDocument().toObject(Notification.class);
                                if (notification.timestamp > (System.currentTimeMillis() - 30000)) {
                                    showSystemNotification(notification);
                                }
                            }
                        }
                    }
                });
    }

    private void showSystemNotification(Notification notification) {
        String channelId = "LifeSaver_Notifications";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Blood Requests", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, NotificationsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("LifeSaver Alert")
                .setContentText(notification.message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void setupAppVersionFooter() {
        TextView tvAppVersion = navViewCustom.findViewById(R.id.tvAppVersion);
        if (tvAppVersion != null) {
            String fullText = "Leet-Hub Development Studio";
            SpannableString spannableString = new SpannableString(fullText);

            int leetHubStart = fullText.indexOf("Leet-Hub");
            int leetHubEnd = leetHubStart + "Leet-Hub".length();
            if (leetHubStart != -1) {
                spannableString.setSpan(new ForegroundColorSpan(Color.parseColor("#F44336")), leetHubStart, leetHubEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            
            int devStart = leetHubEnd;
            int devEnd = fullText.length();
            if (devStart != -1) {
                spannableString.setSpan(new ForegroundColorSpan(Color.parseColor("#888888")), devStart, devEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            tvAppVersion.setText(spannableString);
        }
    }

    private void filterDonors(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(donorList);
        } else {
            for (User u : donorList) {
                if (u.city != null && u.city.toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(u);
                }
            }
        }
        donorAdapter.notifyDataSetChanged();
    }

    private void setupCustomDrawerClicks() {
        LinearLayout llNavPrivacy = navViewCustom.findViewById(R.id.llNavPrivacy);
        LinearLayout llNavAboutUs = navViewCustom.findViewById(R.id.llNavAboutUs);
        View btnNavLogout = navViewCustom.findViewById(R.id.llNavLogout);

        llNavPrivacy.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(this, "Privacy Policy clicked", Toast.LENGTH_SHORT).show();
        });

        llNavAboutUs.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(MainActivity.this, AboutActivity.class));
        });

        btnNavLogout.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_home_bottom);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_map_bottom) {
                startActivity(new Intent(MainActivity.this, MapActivity.class));
                return false;
            } else if (id == R.id.nav_request_bottom) {
                startActivity(new Intent(MainActivity.this, RequestActivity.class));
                return false;
            } else if (id == R.id.nav_profile_bottom) {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                return false;
            }
            return true;
        });
    }

    private void loadCurrentUserHeader() {
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            tvNavHeaderName.setText(user.name);
                            tvNavHeaderEmail.setText(user.email);
                        }
                    }
                });
    }

    private void fetchDonors() {
        db.collection("users")
                .whereEqualTo("availability", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    donorList.clear();
                    filteredList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        if (user != null && !user.userId.equals(currentUserId)) {
                            donorList.add(user);
                        }
                    }
                    filteredList.addAll(donorList);
                    donorAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load donors", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onViewDetailsClicked(User user) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_donor_details, null);
        View ivDialogClose = dialogView.findViewById(R.id.ivDialogClose);
        TextView tvDialogName = dialogView.findViewById(R.id.tvDialogName);
        TextView tvDialogAddress = dialogView.findViewById(R.id.tvDialogAddress);
        TextView tvDialogPhone = dialogView.findViewById(R.id.tvDialogPhone);
        TextView tvDialogStatus = dialogView.findViewById(R.id.tvDialogStatus);
        View btnDialogRequest = dialogView.findViewById(R.id.btnDialogRequest);

        String nameText = "Name: " + (user.name != null ? user.name : "N/A");
        SpannableString nameSpan = new SpannableString(nameText);
        nameSpan.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvDialogName.setText(nameSpan);

        String addressText = "Address: " + (user.city != null ? user.city : "N/A");
        SpannableString addressSpan = new SpannableString(addressText);
        addressSpan.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvDialogAddress.setText(addressSpan);

        String phoneText = "Mobile Number: " + (user.phone != null ? user.phone : "N/A");
        SpannableString phoneSpan = new SpannableString(phoneText);
        phoneSpan.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, 14, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvDialogPhone.setText(phoneSpan);

        String statusText = "Current Status: " + (user.availability ? "Ready to Donate" : "Not Available");
        SpannableString statusSpan = new SpannableString(statusText);
        statusSpan.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, 15, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvDialogStatus.setText(statusSpan);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();

        if (dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(width, android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }

        ivDialogClose.setOnClickListener(v -> dialog.dismiss());
        btnDialogRequest.setOnClickListener(v -> {
            dialog.dismiss();
            sendRequestNotification(user);
        });
    }

    @Override
    public void onRequestDonateClicked(User user) {
        sendRequestNotification(user);
    }

    private void sendRequestNotification(User donor) {
        // 1. Create a BloodRequest document so details can be shown in the popup
        String requestId = db.collection("blood_requests").document().getId();
        db.collection("users").document(currentUserId).get().addOnSuccessListener(userDoc -> {
            User currentUser = userDoc.toObject(User.class);
            if (currentUser != null) {
                BloodRequest br = new BloodRequest(requestId, currentUserId, currentUser.name, 
                        donor.bloodGroup, "Direct Request", currentUser.phone, "Urgent", 0.0, 0.0);
                
                db.collection("blood_requests").document(requestId).set(br).addOnSuccessListener(aVoid -> {
                    // 2. Send the notification with the requestId as referenceId
                    String notificationId = db.collection("notifications").document().getId();
                    Notification notification = new Notification(
                            notificationId,
                            currentUserId,
                            currentUser.name,
                            donor.userId,
                            currentUser.name + " has requested you for blood donation.",
                            System.currentTimeMillis(),
                            "REQUEST",
                            requestId
                    );

                    db.collection("notifications").document(notificationId)
                            .set(notification)
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(this, "Request sent to " + donor.name, Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to send notification", Toast.LENGTH_SHORT).show());
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                updateUserLocation();
            }
        }
    }
}
