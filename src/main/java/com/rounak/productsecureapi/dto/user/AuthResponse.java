package com.rounak.productsecureapi.dto.user;

public record AuthResponse(
        String accessToken,
        String tokenType
) {}