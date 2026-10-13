package com.company.admin.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "audit_entries")
public class AuditEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Instant occurredAt;
    @Column(nullable = false, length = 40) private String actor;
    @Column(nullable = false, length = 60) private String action;
    @Column(nullable = false, length = 120) private String target;
    @Column(nullable = false, length = 500) private String details;
    protected AuditEntry() { }
    public AuditEntry(String actor, String action, String target, String details) {
        occurredAt = Instant.now(); this.actor = actor; this.action = action; this.target = target; this.details = details;
    }
    public Long getId() { return id; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getTarget() { return target; }
    public String getDetails() { return details; }
}
