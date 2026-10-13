package com.company.admin.domain;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
public class Role {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version private long version;
    @Column(nullable = false, unique = true, length = 40)
    private String name;
    @Column(nullable = false, length = 180)
    private String description;
    @Column(nullable = false)
    private boolean systemRole;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Enumerated(EnumType.STRING) @Column(name = "permission", nullable = false, length = 40)
    private Set<Permission> permissions = new LinkedHashSet<>();

    protected Role() { }
    public Role(String name, String description, boolean systemRole, Set<Permission> permissions) {
        this.name = name; this.description = description; this.systemRole = systemRole;
        this.permissions = new LinkedHashSet<>(permissions);
    }
    public Long getId() { return id; }
    public long getVersion() { return version; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isSystemRole() { return systemRole; }
    public Set<Permission> getPermissions() { return Set.copyOf(permissions); }
    public void update(String name, String description, Set<Permission> permissions) {
        this.name = name; this.description = description; this.permissions = new LinkedHashSet<>(permissions);
    }
}
