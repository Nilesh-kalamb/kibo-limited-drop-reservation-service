package com.kibo.drop.repository;

import com.kibo.drop.domain.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.*;

public interface HoldRepository extends JpaRepository<Hold, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Hold h join fetch h.drop where h.id = :id")
    Optional<Hold> findByIdForUpdate(@Param("id") UUID id);

    @Query("select h from Hold h where h.status = 'ACTIVE' and h.expiresAt <= :now")
    List<Hold> findExpired(@Param("now") Instant now);
}
