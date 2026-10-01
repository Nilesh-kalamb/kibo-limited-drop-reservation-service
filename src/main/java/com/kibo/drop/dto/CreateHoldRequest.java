package com.kibo.drop.dto;

import jakarta.validation.constraints.*;

public record CreateHoldRequest(
        @NotBlank String customerId,
        @Min(1) int quantity
) {}
