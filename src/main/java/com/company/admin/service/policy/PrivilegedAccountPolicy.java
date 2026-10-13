package com.company.admin.service.policy;

import com.company.admin.domain.Permission;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class PrivilegedAccountPolicy implements AccessMutationPolicy {
    @Override
    public void validate(UserMutation mutation) {
        boolean privilegedTarget = mutation.willBeAdministrator()
                || mutation.roles().stream().anyMatch(r -> privileged(r.getPermissions()))
                || (mutation.current() != null && (mutation.current().hasRole("ADMIN")
                    || privileged(mutation.current().getPermissions())));
        if (privilegedTarget && !mutation.actor().hasRole("ADMIN")) {
            throw new AccessDeniedException("Только администратор может изменять учётные записи, управляющие доступом");
        }
    }
    private boolean privileged(Set<Permission> permissions) {
        return permissions.contains(Permission.ROLE_WRITE) || permissions.contains(Permission.ROLE_ASSIGN);
    }
}
