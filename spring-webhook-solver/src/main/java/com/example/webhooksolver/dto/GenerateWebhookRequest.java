
package com.example.webhooksolver.dto;

public class GenerateWebhookRequest {
    private String name;
    private String regNo;
    private String email;

    public GenerateWebhookRequest() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegNo() {
        return regNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
