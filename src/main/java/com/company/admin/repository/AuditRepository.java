package com.company.admin.repository;

import com.company.admin.domain.AuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditRepository extends JpaRepository<AuditEntry, Long> {
    Page<AuditEntry> findAllByOrderByOccurredAtDescIdDesc(Pageable pageable);
}
