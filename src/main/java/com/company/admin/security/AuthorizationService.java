package com.company.admin.security;

import com.company.admin.domain.*;
import com.company.admin.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationService {
    private final UserRepository users;
    public AuthorizationService(UserRepository users) { this.users = users; }
    public AdminUser require(Permission permission) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) throw new AccessDeniedException("Требуется вход");
        var actor = users.findByUsername(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Нет доступа"));
        if (!actor.isEnabled() || !actor.getPermissions().contains(permission)) throw new AccessDeniedException("Недостаточно прав");
        return actor;
    }
}
