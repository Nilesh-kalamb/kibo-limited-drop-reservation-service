package com.kibo.drop.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;

public record CreateDropRequest(
        @NotBlank String name,
        @Min(1) int totalUnits,
        @NotNull Instant startsAt
) {}
