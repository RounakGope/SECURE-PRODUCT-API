package com.rounak.productsecureapi.dto.product;

import lombok.Getter;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        String category
) {}