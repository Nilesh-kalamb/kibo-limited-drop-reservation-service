package com.kibo.drop.service;

import com.kibo.drop.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class EventPublisher implements HoldEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public EventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(String routingKey, UUID holdId, Long dropId, String status) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                routingKey,
                Map.of("holdId", holdId.toString(), "dropId", dropId, "status", status)
        );
    }
}
