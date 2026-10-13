package com.company.admin.service;

import com.company.admin.domain.*;
import com.company.admin.repository.AuditRepository;
import com.company.admin.security.AuthorizationService;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly = true)
public class AuditService {
    private final AuditRepository entries;
    private final AuthorizationService authorization;
    public AuditService(AuditRepository entries, AuthorizationService authorization) { this.entries = entries; this.authorization = authorization; }
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    public Page<AuditEntry> list(int page) {
        authorization.require(Permission.AUDIT_READ);
        return entries.findAllByOrderByOccurredAtDescIdDesc(PageRequest.of(Math.max(0, page), 20));
    }
}
