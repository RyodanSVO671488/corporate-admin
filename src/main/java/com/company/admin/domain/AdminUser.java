package com.company.admin.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "admin_users")
public class AdminUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version private long version;
    @Column(nullable = false, unique = true, length = 40)
    private String username;
    @Column(nullable = false, length = 100)
    private String fullName;
    @Column(nullable = false, unique = true, length = 160)
    private String email;
    @Column(nullable = false, length = 100)
    private String passwordHash;
    @Column(nullable = false)
    private boolean enabled;
    @Column(nullable = false)
    private Instant createdAt;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new LinkedHashSet<>();

    protected AdminUser() { }
    public AdminUser(String username, String fullName, String email, String passwordHash, boolean enabled, Set<Role> roles) {
        this.username = username; this.fullName = fullName; this.email = email;
        this.passwordHash = passwordHash; this.enabled = enabled;
        this.roles = new LinkedHashSet<>(roles); this.createdAt = Instant.now();
    }
    public Long getId() { return id; }
    public long getVersion() { return version; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Set<Role> getRoles() { return Set.copyOf(roles); }
    public boolean hasRole(String name) { return roles.stream().anyMatch(r -> r.getName().equals(name)); }
    public Set<Permission> getPermissions() {
        Set<Permission> result = new LinkedHashSet<>();
        roles.forEach(role -> result.addAll(role.getPermissions()));
        return Set.copyOf(result);
    }
    public void update(String fullName, String email, boolean enabled, Set<Role> roles) {
        this.fullName = fullName; this.email = email; this.enabled = enabled;
        this.roles = new LinkedHashSet<>(roles);
    }
    public void changePassword(String hash) { this.passwordHash = hash; }
}
