package com.example.demo.model;

import java.util.ArrayList;
import java.util.List;

public class PiDevice {
    private String piId;
    private String username;     // null when unregistered
    private List<PhoneContact> contacts = new ArrayList<>();
    private boolean registered;

    // Keep legacy fields for backwards-compatible JSON deserialization
    private String phone;
    private String gatewayEmail;

    public PiDevice() {}

    public PiDevice(String piId) {
        this.piId = piId;
        this.registered = false;
    }

    public String getPiId() { return piId; }
    public void setPiId(String piId) { this.piId = piId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public List<PhoneContact> getContacts() { return contacts; }
    public void setContacts(List<PhoneContact> contacts) { this.contacts = contacts != null ? contacts : new ArrayList<>(); }

    public boolean isRegistered() { return registered; }
    public void setRegistered(boolean registered) { this.registered = registered; }

    // Legacy getters — migrate old single phone/gateway into contacts list
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getGatewayEmail() { return gatewayEmail; }
    public void setGatewayEmail(String gatewayEmail) { this.gatewayEmail = gatewayEmail; }

    /**
     * Call after deserialization to migrate legacy single-phone records
     * into the contacts list (if contacts is empty but phone was set).
     */
    public void migrateLegacy() {
        if ((contacts == null || contacts.isEmpty()) && phone != null && !phone.isBlank()) {
            if (contacts == null) contacts = new ArrayList<>();
            contacts.add(new PhoneContact(phone, gatewayEmail != null ? gatewayEmail : ""));
            phone = null;
            gatewayEmail = null;
        }
    }

    public static class PhoneContact {
        private String phone;
        private String gatewayEmail;

        public PhoneContact() {}

        public PhoneContact(String phone, String gatewayEmail) {
            this.phone = phone;
            this.gatewayEmail = gatewayEmail;
        }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getGatewayEmail() { return gatewayEmail; }
        public void setGatewayEmail(String gatewayEmail) { this.gatewayEmail = gatewayEmail; }
    }
}
