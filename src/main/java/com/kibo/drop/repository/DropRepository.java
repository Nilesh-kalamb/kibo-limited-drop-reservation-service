package com.kibo.drop.repository;

import com.kibo.drop.domain.Drop;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DropRepository extends JpaRepository<Drop, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Drop d where d.id = :id")
    Optional<Drop> findByIdForUpdate(@Param("id") Long id);
}
