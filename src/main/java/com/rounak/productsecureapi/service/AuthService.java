package com.rounak.productsecureapi.service;

import com.rounak.productsecureapi.dto.user.AuthResponse;
import com.rounak.productsecureapi.dto.user.LoginRequest;
import com.rounak.productsecureapi.dto.user.UserRegistrationRequest;
import com.rounak.productsecureapi.dto.user.UserResponse;

public interface AuthService {
    UserResponse register(UserRegistrationRequest request);

    AuthResponse login(LoginRequest request);
    void generateEmailVerificationOtp(String email);

    void verifyEmail(String email, String otp);
}
