package com.leethub.lifesaver.models;

public class User {
    public String userId, name, email, phone, bloodGroup, city, profileImage, lastDonationDate, fcmToken;
    public double latitude, longitude;
    public boolean availability;
    public int totalDonations, trustScore;
    public long lastDonationTimestamp;

    public User() {}

    public User(String userId, String name, String email, String phone, String bloodGroup, String city) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.bloodGroup = bloodGroup;
        this.city = city;
        this.profileImage = "";
        this.lastDonationDate = "Never";
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.availability = true;
        this.totalDonations = 0;
        this.trustScore = 10;
        this.fcmToken = "";
        this.lastDonationTimestamp = 0;
    }
}
