package com.kibo.drop.service;

import com.kibo.drop.domain.*;
import com.kibo.drop.dto.CreateHoldRequest;
import com.kibo.drop.exception.BusinessException;
import com.kibo.drop.exception.HoldExpiredException;
import com.kibo.drop.exception.NotFoundException;
import com.kibo.drop.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HoldServiceTest {
    @Mock DropRepository dropRepository;
    @Mock HoldRepository holdRepository;
    @Mock HoldEventPublisher events;

    HoldService service;

    @BeforeEach
    void setup() {
        service = new HoldService(dropRepository, holdRepository, events, 300);
    }

    @Test
    void shouldCreateHoldWhenInventoryIsAvailable() {
        Drop drop = drop(10, 10);
        when(dropRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(drop));
        when(holdRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Hold result = service.createHold(1L, new CreateHoldRequest("customer-1", 3));

        assertEquals(7, drop.getAvailableUnits());
        assertEquals(3, result.getQuantity());
        assertEquals(HoldStatus.ACTIVE, result.getStatus());
        verify(events).publish("hold.created", result.getId(), 1L, "ACTIVE");
    }

    @Test
    void shouldRejectWhenInventoryIsInsufficient() {
        Drop drop = drop(2, 2);
        when(dropRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(drop));

        assertThrows(BusinessException.class,
                () -> service.createHold(1L, new CreateHoldRequest("customer-1", 3)));

        assertEquals(2, drop.getAvailableUnits());
        verify(holdRepository, never()).save(any());
    }

    @Test
    void shouldCancelAndReleaseInventory() {
        Drop drop = drop(10, 5);
        Hold hold = new Hold(drop, "customer", 3, Instant.now().plusSeconds(300));

        when(holdRepository.findByIdForUpdate(hold.getId())).thenReturn(Optional.of(hold));
        when(holdRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Hold result = service.cancel(1L, hold.getId());

        assertEquals(8, drop.getAvailableUnits());
        assertEquals(HoldStatus.CANCELLED, result.getStatus());
        verify(events).publish("hold.cancelled", hold.getId(), 1L, "CANCELLED");
    }

    @Test
    void shouldReleaseInventoryWhenConfirmationFindsExpiredHold() {
        Drop drop = drop(10, 5);
        Hold hold = new Hold(drop, "customer", 3, Instant.now().minusSeconds(1));
        when(holdRepository.findByIdForUpdate(hold.getId())).thenReturn(Optional.of(hold));
        when(holdRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(HoldExpiredException.class, () -> service.confirm(1L, hold.getId()));

        assertEquals(8, drop.getAvailableUnits());
        assertEquals(HoldStatus.EXPIRED, hold.getStatus());
        verify(holdRepository).save(hold);
        verify(events).publish("hold.expired", hold.getId(), 1L, "EXPIRED");
    }

    @Test
    void shouldNotReleaseInventoryWhenConfirmingActiveHold() {
        Drop drop = drop(10, 5);
        Hold hold = new Hold(drop, "customer", 3, Instant.now().plusSeconds(300));
        when(holdRepository.findByIdForUpdate(hold.getId())).thenReturn(Optional.of(hold));
        when(holdRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Hold result = service.confirm(1L, hold.getId());

        assertEquals(5, drop.getAvailableUnits());
        assertEquals(HoldStatus.CONFIRMED, result.getStatus());
        verify(events).publish("hold.confirmed", hold.getId(), 1L, "CONFIRMED");
    }

    @Test
    void shouldNotCancelAHoldUnderAnotherDrop() {
        Drop drop = drop(10, 5);
        Hold hold = new Hold(drop, "customer", 3, Instant.now().plusSeconds(300));
        when(holdRepository.findByIdForUpdate(hold.getId())).thenReturn(Optional.of(hold));

        assertThrows(NotFoundException.class, () -> service.cancel(2L, hold.getId()));

        assertEquals(5, drop.getAvailableUnits());
        verify(holdRepository, never()).save(any());
    }

    @Test
    void shouldRejectZeroQuantityWithoutChangingInventory() {
        Drop drop = drop(10, 10);
        when(dropRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(drop));

        assertThrows(BusinessException.class,
                () -> service.createHold(1L, new CreateHoldRequest("customer-1", 0)));

        assertEquals(10, drop.getAvailableUnits());
        verify(holdRepository, never()).save(any());
    }

    private Drop drop(int totalUnits, int availableUnits) {
        Drop drop = new Drop("Test", totalUnits, availableUnits, Instant.now().minusSeconds(1));
        ReflectionTestUtils.setField(drop, "id", 1L);
        return drop;
    }
}
