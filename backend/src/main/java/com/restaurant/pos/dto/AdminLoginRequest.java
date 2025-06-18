package com.restaurant.pos.dto;

import jakarta.validation.constraints.NotBlank;

public class AdminLoginRequest {
    @NotBlank(message = "Админ кодот е задолжителен")
    private String adminCode;

    // Constructors
    public AdminLoginRequest() { }

    public AdminLoginRequest(String adminCode) {
        this.adminCode = adminCode;
    }

    // Getters and Setters
    public String getAdminCode() { return adminCode; }
    public void setAdminCode(String adminCode) { this.adminCode = adminCode; }
}