package com.company.admin.service.policy;

import com.company.admin.domain.*;
import java.util.Set;

public record UserMutation(AdminUser actor, AdminUser current, boolean enabled, Set<Role> roles) {
    public boolean willBeAdministrator() { return roles.stream().anyMatch(r -> r.getName().equals("ADMIN")); }
}
