package com.company.admin.audit;

import com.company.admin.domain.AuditEntry;
import com.company.admin.repository.AuditRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AuditEventListener {
    private final AuditRepository repository;
    public AuditEventListener(AuditRepository repository) { this.repository = repository; }
    /** Synchronous observer participates in the use-case transaction: both writes commit or both roll back. */
    @EventListener
    public void onAccessChanged(AccessChangedEvent event) {
        repository.save(new AuditEntry(event.actor(), event.action(), event.target(), event.details()));
    }
}
