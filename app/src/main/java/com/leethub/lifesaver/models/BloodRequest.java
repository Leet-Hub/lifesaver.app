package com.leethub.lifesaver.models;

public class BloodRequest {
    public String requestId, requesterId, patientName, bloodGroup, hospitalName, contactNumber, urgency, status;
    public double latitude, longitude;
    public long timestamp;

    public BloodRequest() {}

    public BloodRequest(String requestId, String requesterId, String patientName, String bloodGroup, 
                        String hospitalName, String contactNumber, String urgency, double lat, double lng) {
        this.requestId = requestId;
        this.requesterId = requesterId;
        this.patientName = patientName;
        this.bloodGroup = bloodGroup;
        this.hospitalName = hospitalName;
        this.contactNumber = contactNumber;
        this.urgency = urgency;
        this.status = "Pending";
        this.latitude = lat;
        this.longitude = lng;
        this.timestamp = System.currentTimeMillis();
    }
}
