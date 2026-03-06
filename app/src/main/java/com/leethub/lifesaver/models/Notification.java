package com.leethub.lifesaver.models;

public class Notification {
    public String notificationId;
    public String fromUserId;
    public String fromUserName;
    public String toUserId;
    public String message;
    public long timestamp;
    public boolean read;
    public String type; // "REQUEST" or "GENERAL"
    public String referenceId; // For BloodRequest ID

    public Notification() {
        // Required for Firestore
    }

    public Notification(String notificationId, String fromUserId, String fromUserName, String toUserId, String message, long timestamp, String type, String referenceId) {
        this.notificationId = notificationId;
        this.fromUserId = fromUserId;
        this.fromUserName = fromUserName;
        this.toUserId = toUserId;
        this.message = message;
        this.timestamp = timestamp;
        this.read = false;
        this.type = type;
        this.referenceId = referenceId;
    }
}
