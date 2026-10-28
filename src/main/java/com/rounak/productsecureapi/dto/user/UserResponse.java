package com.rounak.productsecureapi.dto.user;

import com.rounak.productsecureapi.enums.Role;

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        Instant createdAt,
        Boolean emailVerified
) {}