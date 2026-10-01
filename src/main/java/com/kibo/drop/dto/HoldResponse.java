package com.kibo.drop.dto;

import com.kibo.drop.domain.*;
import java.time.Instant;
import java.util.UUID;

public record HoldResponse(UUID id, Long dropId, String customerId, int quantity,
                           HoldStatus status, Instant expiresAt) {
    public static HoldResponse from(Hold h) {
        return new HoldResponse(h.getId(), h.getDrop().getId(), h.getCustomerId(),
                h.getQuantity(), h.getStatus(), h.getExpiresAt());
    }
}
