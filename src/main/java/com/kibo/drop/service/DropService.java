package com.kibo.drop.service;

import com.kibo.drop.domain.Drop;
import com.kibo.drop.dto.CreateDropRequest;
import com.kibo.drop.exception.NotFoundException;
import com.kibo.drop.repository.DropRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DropService {
    private final DropRepository repository;

    public DropService(DropRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Drop create(CreateDropRequest request) {
        Drop drop = new Drop(request.name(), request.totalUnits(),
                request.totalUnits(), request.startsAt());
        return repository.save(drop);
    }

    @Transactional(readOnly = true)
    public List<Drop> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Drop get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Drop not found: " + id));
    }
}
