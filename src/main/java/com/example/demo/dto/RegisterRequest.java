package com.example.demo.dto;

/**
 * DTO for device registration request coming from the UI.
 * deviceId: the ID printed on the physical product
 * phoneRef: optional token or phone reference (backend can add real phone number server-side)
 */
public class RegisterRequest {
    private String deviceId;
    private String phoneRef;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getPhoneRef() {
        return phoneRef;
    }

    public void setPhoneRef(String phoneRef) {
        this.phoneRef = phoneRef;
    }
}
