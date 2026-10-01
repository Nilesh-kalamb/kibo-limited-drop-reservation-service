package com.kibo.drop.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "holds")
public class Hold {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drop_id", nullable = false)
    private Drop drop;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HoldStatus status;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt;

    protected Hold() {}

    public Hold(Drop drop, String customerId, int quantity, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.drop = drop;
        this.customerId = customerId;
        this.quantity = quantity;
        this.status = HoldStatus.ACTIVE;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Drop getDrop() { return drop; }
    public String getCustomerId() { return customerId; }
    public int getQuantity() { return quantity; }
    public HoldStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }

    public void confirm() { status = HoldStatus.CONFIRMED; }
    public void cancel() { status = HoldStatus.CANCELLED; }
    public void expire() { status = HoldStatus.EXPIRED; }
}
