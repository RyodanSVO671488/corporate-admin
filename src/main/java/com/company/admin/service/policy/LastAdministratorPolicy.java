package com.company.admin.service.policy;

import com.company.admin.repository.UserRepository;
import com.company.admin.service.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class LastAdministratorPolicy implements AccessMutationPolicy {
    private final UserRepository users;
    public LastAdministratorPolicy(UserRepository users) { this.users = users; }
    @Override
    public void validate(UserMutation mutation) {
        var current = mutation.current();
        if (current != null && current.isEnabled() && current.hasRole("ADMIN")
                && (!mutation.enabled() || !mutation.willBeAdministrator())
                && users.countByEnabledTrueAndRoles_Name("ADMIN") <= 1) {
            throw new BusinessException("Нельзя заблокировать или лишить роли последнего активного администратора");
        }
    }
}
