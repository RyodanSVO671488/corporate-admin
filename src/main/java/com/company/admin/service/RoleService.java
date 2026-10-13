package com.company.admin.service;

import com.company.admin.audit.AccessChangedEvent;
import com.company.admin.domain.*;
import com.company.admin.repository.*;
import com.company.admin.security.AuthorizationService;
import com.company.admin.web.form.RoleForm;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service @Transactional(readOnly = true)
public class RoleService {
    private final RoleRepository roles;
    private final AccessGuardRepository guard;
    private final AuthorizationService authorization;
    private final ApplicationEventPublisher events;
    public RoleService(RoleRepository roles, AccessGuardRepository guard, AuthorizationService authorization, ApplicationEventPublisher events) {
        this.roles = roles; this.guard = guard; this.authorization = authorization; this.events = events;
    }
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public List<Role> list() { authorization.require(Permission.ROLE_READ); return roles.findAllByOrderByNameAsc(); }
    /** Only returns selectable roles if the actor may actually assign them. */
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public List<Role> assignableRoles() {
        var actor = authorization.require(Permission.USER_WRITE);
        if (!actor.getPermissions().contains(Permission.ROLE_ASSIGN)) return List.of();
        return roles.findAllByOrderByNameAsc().stream().filter(r -> actor.hasRole("ADMIN")
                || (!r.isSystemRole() && !r.getPermissions().contains(Permission.ROLE_WRITE)
                    && !r.getPermissions().contains(Permission.ROLE_ASSIGN))).toList();
    }
    @PreAuthorize("hasAuthority('ROLE_WRITE')")
    public RoleForm form(long id) {
        authorization.require(Permission.ROLE_WRITE);
        var role = get(id); checkEditable(role);
        RoleForm form = new RoleForm();
        form.setName(role.getName()); form.setDescription(role.getDescription());
        form.setPermissions(role.getPermissions()); form.setVersion(role.getVersion());
        return form;
    }
    @Transactional @PreAuthorize("hasAuthority('ROLE_WRITE')")
    public void create(RoleForm form) {
        guard.lockMutations();
        var actor = authorization.require(Permission.ROLE_WRITE);
        checkUnique(form.getName(), null); checkReservedPermissions(actor, form);
        var role = roles.saveAndFlush(new Role(form.getName(), form.getDescription().strip(), false, form.getPermissions()));
        publish(actor, "ROLE_CREATED", role);
    }
    @Transactional @PreAuthorize("hasAuthority('ROLE_WRITE')")
    public void update(long id, RoleForm form) {
        guard.lockMutations();
        var actor = authorization.require(Permission.ROLE_WRITE);
        var role = get(id); checkEditable(role);
        if (form.getVersion() == null || role.getVersion() != form.getVersion()) throw new BusinessException("Роль уже изменена. Откройте форму заново");
        checkUnique(form.getName(), id); checkReservedPermissions(actor, form);
        if (!actor.hasRole("ADMIN") && role.getPermissions().contains(Permission.ROLE_WRITE))
            throw new AccessDeniedException("Только администратор изменяет роли, управляющие доступом");
        role.update(form.getName(), form.getDescription().strip(), form.getPermissions());
        roles.flush(); publish(actor, "ROLE_UPDATED", role);
    }
    private void checkReservedPermissions(AdminUser actor, RoleForm form) {
        if (!actor.hasRole("ADMIN") && (form.getPermissions().contains(Permission.ROLE_WRITE) || form.getPermissions().contains(Permission.ROLE_ASSIGN)))
            throw new AccessDeniedException("Только администратор делегирует управление доступом");
    }
    private Role get(long id) { return roles.findById(id).orElseThrow(NotFoundException::new); }
    private void checkEditable(Role role) { if (role.isSystemRole()) throw new BusinessException("Системная роль ADMIN защищена от изменений"); }
    private void checkUnique(String name, Long id) {
        roles.findByName(name).filter(r -> !Objects.equals(r.getId(), id)).ifPresent(r -> { throw new BusinessException("Роль с таким названием уже существует"); });
    }
    private void publish(AdminUser actor, String action, Role role) {
        events.publishEvent(new AccessChangedEvent(actor.getUsername(), action, role.getName(), "Права: "
                + role.getPermissions().stream().map(Enum::name).sorted().collect(Collectors.joining(", "))));
    }
}
