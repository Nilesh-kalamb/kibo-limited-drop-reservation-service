package com.kibo.drop.controller;

import com.kibo.drop.dto.*;
import com.kibo.drop.service.HoldService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/drops/{dropId}/holds")
public class HoldController {
    private final HoldService service;

    public HoldController(HoldService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<HoldResponse> create(@PathVariable Long dropId,
                                               @Valid @RequestBody CreateHoldRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(HoldResponse.from(service.createHold(dropId, request)));
    }

    @PostMapping("/{holdId}/confirm")
    public HoldResponse confirm(@PathVariable Long dropId, @PathVariable UUID holdId) {
        return HoldResponse.from(service.confirm(dropId, holdId));
    }

    @PostMapping("/{holdId}/cancel")
    public HoldResponse cancel(@PathVariable Long dropId, @PathVariable UUID holdId) {
        return HoldResponse.from(service.cancel(dropId, holdId));
    }
}
