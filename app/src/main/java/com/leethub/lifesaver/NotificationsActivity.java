package com.leethub.lifesaver;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.leethub.lifesaver.adapters.NotificationAdapter;
import com.leethub.lifesaver.models.BloodRequest;
import com.leethub.lifesaver.models.Notification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationClickListener {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        rvNotifications = findViewById(R.id.rvNotifications);
        btnBack = findViewById(R.id.btnBackNotifications);

        btnBack.setOnClickListener(v -> finish());

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        notificationList = new ArrayList<>();
        // Updated adapter with click listener
        adapter = new NotificationAdapter(this, notificationList, this);
        rvNotifications.setAdapter(adapter);

        fetchNotifications();
    }

    private void fetchNotifications() {
        String currentUserId = mAuth.getCurrentUser().getUid();

        db.collection("notifications")
                .whereEqualTo("toUserId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notificationList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Notification notification = doc.toObject(Notification.class);
                        if (notification != null) {
                            notificationList.add(notification);
                        }
                    }
                    
                    Collections.sort(notificationList, (n1, n2) -> Long.compare(n2.timestamp, n1.timestamp));
                    
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onNotificationClick(Notification notification) {
        if ("REQUEST".equals(notification.type) && notification.referenceId != null) {
            fetchRequestDetails(notification.referenceId);
        }
    }

    private void fetchRequestDetails(String requestId) {
        db.collection("blood_requests").document(requestId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    BloodRequest request = documentSnapshot.toObject(BloodRequest.class);
                    if (request != null) {
                        showRequestDetailsDialog(request);
                    } else {
                        Toast.makeText(this, "Request details not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load details", Toast.LENGTH_SHORT).show());
    }

    private void showRequestDetailsDialog(BloodRequest request) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_request_info, null);

        TextView tvReqName = dialogView.findViewById(R.id.tvReqName);
        TextView tvReqBloodGroup = dialogView.findViewById(R.id.tvReqBloodGroup);
        TextView tvReqHospital = dialogView.findViewById(R.id.tvReqHospital);
        TextView tvReqPhone = dialogView.findViewById(R.id.tvReqPhone);
        TextView tvReqMessage = dialogView.findViewById(R.id.tvReqMessage);
        View btnAccept = dialogView.findViewById(R.id.btnAcceptRequest);
        View btnReject = dialogView.findViewById(R.id.btnRejectRequest);
        View ivClose = dialogView.findViewById(R.id.ivCloseRequestDialog);

        tvReqName.setText("Name: " + request.patientName);
        tvReqBloodGroup.setText("Blood Group: " + request.bloodGroup);
        tvReqHospital.setText("Medical Name: " + request.hospitalName);
        tvReqPhone.setText("Mobile Number: " + request.contactNumber);
        tvReqMessage.setText("Message: " + request.urgency);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();

        if (dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        ivClose.setOnClickListener(v -> dialog.dismiss());

        btnAccept.setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(this, "Request Accepted!", Toast.LENGTH_SHORT).show();
            // Implement calling functionality
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + request.contactNumber));
            startActivity(intent);
        });

        btnReject.setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(this, "Request Rejected", Toast.LENGTH_SHORT).show();
        });
    }
}
