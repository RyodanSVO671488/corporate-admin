package com.company.admin.service;

import com.company.admin.repository.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final AuditRepository audit;
    public DashboardService(UserRepository users, RoleRepository roles, AuditRepository audit) { this.users = users; this.roles = roles; this.audit = audit; }
    @Transactional(readOnly = true) @PreAuthorize("isAuthenticated()")
    public Stats stats() { return new Stats(users.count(), users.countByEnabledTrue(), roles.count(), audit.count()); }
    public record Stats(long userCount, long enabledCount, long roleCount, long auditCount) { }
}
