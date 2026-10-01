package com.kibo.drop.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "drop.events";
    public static final String QUEUE = "drop.audit.queue";

    @Bean
    TopicExchange dropExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    Queue auditQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    Binding auditBinding(Queue auditQueue, TopicExchange dropExchange) {
        return BindingBuilder.bind(auditQueue).to(dropExchange).with("hold.*");
    }
}
