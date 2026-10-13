package com.company.admin.repository;

import com.company.admin.domain.AccessGuard;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;

public interface AccessGuardRepository extends JpaRepository<AccessGuard, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from AccessGuard g where g.id = 1")
    AccessGuard lockMutations();
}
