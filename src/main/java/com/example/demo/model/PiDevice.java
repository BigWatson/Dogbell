package com.example.demo.model;

public class PiDevice {
    private String piId;
    private String username;     // null when unregistered
    private String phone;        // e.g. "17607042102"
    private String gatewayEmail; // e.g. "17607042102@tmomail.net"
    private boolean registered;

    public PiDevice() {}

    public PiDevice(String piId) {
        this.piId = piId;
        this.registered = false;
    }

    public String getPiId() { return piId; }
    public void setPiId(String piId) { this.piId = piId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getGatewayEmail() { return gatewayEmail; }
    public void setGatewayEmail(String gatewayEmail) { this.gatewayEmail = gatewayEmail; }

    public boolean isRegistered() { return registered; }
    public void setRegistered(boolean registered) { this.registered = registered; }
}
