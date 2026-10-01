package com.kibo.drop.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "drops")
public class Drop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int totalUnits;

    @Column(nullable = false)
    private int availableUnits;

    @Column(nullable = false)
    private Instant startsAt;

    protected Drop() {}

    public Drop(String name, int totalUnits, int availableUnits, Instant startsAt) {
        this.name = name;
        this.totalUnits = totalUnits;
        this.availableUnits = availableUnits;
        this.startsAt = startsAt;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public int getTotalUnits() { return totalUnits; }
    public int getAvailableUnits() { return availableUnits; }
    public Instant getStartsAt() { return startsAt; }

    public void setAvailableUnits(int availableUnits) {
        this.availableUnits = availableUnits;
    }
}
