package com.rounak.productsecureapi.service;

import com.rounak.productsecureapi.enums.OtpPurpose;
public interface OtpService {

    void generateAndSendOtp(String email, OtpPurpose purpose);

    boolean verifyOtp(String email, String otp, OtpPurpose purpose);
}