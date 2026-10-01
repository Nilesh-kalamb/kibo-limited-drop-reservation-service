package com.kibo.drop.service;

import com.kibo.drop.domain.*;
import com.kibo.drop.dto.CreateHoldRequest;
import com.kibo.drop.exception.*;
import com.kibo.drop.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class HoldService {
    private final DropRepository dropRepository;
    private final HoldRepository holdRepository;
    private final HoldEventPublisher events;
    private final long holdDurationSeconds;

    public HoldService(DropRepository dropRepository, HoldRepository holdRepository,
                       HoldEventPublisher events,
                       @Value("${app.hold-duration-seconds:300}") long holdDurationSeconds) {
        this.dropRepository = dropRepository;
        this.holdRepository = holdRepository;
        this.events = events;
        this.holdDurationSeconds = holdDurationSeconds;
    }

    @Transactional
    public Hold createHold(Long dropId, CreateHoldRequest request) {
        Drop drop = dropRepository.findByIdForUpdate(dropId)
                .orElseThrow(() -> new NotFoundException("Drop not found: " + dropId));

        Instant now = Instant.now();
        if (now.isBefore(drop.getStartsAt())) {
            throw new BusinessException("Drop has not started");
        }

        if (request.quantity() < 1) {
            throw new BusinessException("Quantity must be at least 1");
        }

        if (request.quantity() > drop.getAvailableUnits()) {
            throw new BusinessException("Insufficient inventory");
        }

        drop.setAvailableUnits(drop.getAvailableUnits() - request.quantity());

        Hold hold = new Hold(drop, request.customerId(), request.quantity(),
                now.plusSeconds(holdDurationSeconds));
        Hold saved = holdRepository.save(hold);

        events.publish("hold.created", saved.getId(), dropId, "ACTIVE");
        return saved;
    }

    @Transactional(noRollbackFor = HoldExpiredException.class)
    public Hold confirm(Long dropId, UUID holdId) {
        Hold hold = getForUpdate(holdId);
        validateDrop(hold, dropId);
        validateActive(hold);

        if (!Instant.now().isBefore(hold.getExpiresAt())) {
            expireInternal(hold);
            events.publish("hold.expired", holdId, dropId, "EXPIRED");
            throw new HoldExpiredException("Hold has expired");
        }

        hold.confirm();
        Hold saved = holdRepository.save(hold);
        events.publish("hold.confirmed", holdId, hold.getDrop().getId(), "CONFIRMED");
        return saved;
    }

    @Transactional(noRollbackFor = HoldExpiredException.class)
    public Hold cancel(Long dropId, UUID holdId) {
        Hold hold = getForUpdate(holdId);
        validateDrop(hold, dropId);
        validateActive(hold);

        if (!Instant.now().isBefore(hold.getExpiresAt())) {
            expireInternal(hold);
            events.publish("hold.expired", holdId, dropId, "EXPIRED");
            throw new HoldExpiredException("Hold has expired");
        }

        hold.getDrop().setAvailableUnits(
                hold.getDrop().getAvailableUnits() + hold.getQuantity());
        hold.cancel();

        Hold saved = holdRepository.save(hold);
        events.publish("hold.cancelled", holdId, hold.getDrop().getId(), "CANCELLED");
        return saved;
    }

    @Transactional
    public void expireHolds() {
        List<Hold> expired = holdRepository.findExpired(Instant.now());
        for (Hold hold : expired) {
            // Re-read with a lock to avoid racing with confirm/cancel.
            Hold locked = getForUpdate(hold.getId());
            if (locked.getStatus() == HoldStatus.ACTIVE &&
                    !Instant.now().isBefore(locked.getExpiresAt())) {
                expireInternal(locked);
                events.publish("hold.expired", locked.getId(),
                        locked.getDrop().getId(), "EXPIRED");
            }
        }
    }

    private Hold getForUpdate(UUID id) {
        return holdRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Hold not found: " + id));
    }

    private void validateActive(Hold hold) {
        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new BusinessException("Hold is already " + hold.getStatus());
        }
    }

    private void validateDrop(Hold hold, Long dropId) {
        if (!hold.getDrop().getId().equals(dropId)) {
            throw new NotFoundException("Hold not found: " + hold.getId());
        }
    }

    private void expireInternal(Hold hold) {
        hold.getDrop().setAvailableUnits(
                hold.getDrop().getAvailableUnits() + hold.getQuantity());
        hold.expire();
        holdRepository.save(hold);
    }
}
