package com.company.admin.security;

import com.company.admin.domain.AdminUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public record SecurityUser(String username, String password, boolean enabled,
                           List<SimpleGrantedAuthority> authorities) implements UserDetails, Serializable {
    public static SecurityUser from(AdminUser user) {
        var authorities = user.getPermissions().stream().map(p -> new SimpleGrantedAuthority(p.name())).toList();
        return new SecurityUser(user.getUsername(), user.getPasswordHash(), user.isEnabled(), authorities);
    }
    @Override public String getUsername() { return username; }
    @Override public String getPassword() { return password; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public String toString() {
        return "SecurityUser[username=" + username + ", enabled=" + enabled + ", authorities=" + authorities + "]";
    }
}
