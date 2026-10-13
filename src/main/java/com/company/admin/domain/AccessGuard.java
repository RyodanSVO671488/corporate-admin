package com.company.admin.domain;

import jakarta.persistence.*;

/** Single database row used to serialize access mutations across application instances. */
@Entity @Table(name = "access_guard")
public class AccessGuard {
    @Id private Long id;
    protected AccessGuard() { }
}
