package com.kibo.drop.dto;

import com.kibo.drop.domain.Drop;
import java.time.Instant;

public record DropResponse(Long id, String name, int totalUnits, int availableUnits, Instant startsAt) {
    public static DropResponse from(Drop d) {
        return new DropResponse(d.getId(), d.getName(), d.getTotalUnits(),
                d.getAvailableUnits(), d.getStartsAt());
    }
}
