package com.rounak.productsecureapi.dto.product;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;


public record ProductUpdate(

        @NotBlank(message = "Product name is required")
        @Size(max = 150, message = "Product name cannot exceed 150 characters")
        String name,

        @Size(max = 2000, message = "Description cannot exceed 2000 characters")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", message = "Price cannot be negative")
        BigDecimal price,

        @NotNull(message = "Stock is required")
        @PositiveOrZero(message = "Stock cannot be negative")
        Integer stock,

        @NotBlank(message = "Category is required")
        @Size(max = 100, message = "Category cannot exceed 100 characters")
        String category
) {}
