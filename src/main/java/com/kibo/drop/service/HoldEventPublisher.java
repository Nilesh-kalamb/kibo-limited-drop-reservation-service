package com.kibo.drop.service;

import java.util.UUID;

public interface HoldEventPublisher {
    void publish(String routingKey, UUID holdId, Long dropId, String status);
}
