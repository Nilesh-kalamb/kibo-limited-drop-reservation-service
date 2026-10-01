package com.kibo.drop.controller;

import com.kibo.drop.domain.Drop;
import com.kibo.drop.dto.*;
import com.kibo.drop.service.DropService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drops")
public class DropController {
    private final DropService service;

    public DropController(DropService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<DropResponse> create(@Valid @RequestBody CreateDropRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DropResponse.from(service.create(request)));
    }

    @GetMapping
    public List<DropResponse> findAll() {
        return service.findAll().stream().map(DropResponse::from).toList();
    }

    @GetMapping("/{id}")
    public DropResponse get(@PathVariable Long id) {
        return DropResponse.from(service.get(id));
    }
}
