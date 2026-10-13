package com.company.admin.service;

import com.company.admin.audit.AccessChangedEvent;
import com.company.admin.domain.*;
import com.company.admin.repository.*;
import com.company.admin.security.AuthorizationService;
import com.company.admin.service.policy.*;
import com.company.admin.web.form.UserForm;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final AccessGuardRepository guard;
    private final AuthorizationService authorization;
    private final UserFactory factory;
    private final PasswordEncoder encoder;
    private final List<AccessMutationPolicy> policies;
    private final ApplicationEventPublisher events;
    public UserService(UserRepository users, RoleRepository roles, AccessGuardRepository guard,
                       AuthorizationService authorization, UserFactory factory, PasswordEncoder encoder,
                       List<AccessMutationPolicy> policies, ApplicationEventPublisher events) {
        this.users = users; this.roles = roles; this.guard = guard; this.authorization = authorization;
        this.factory = factory; this.encoder = encoder; this.policies = policies; this.events = events;
    }
    @PreAuthorize("hasAuthority('USER_READ')")
    public List<AdminUser> list(String query) {
        authorization.require(Permission.USER_READ);
        String q = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        return users.findAllByOrderByIdAsc().stream().filter(u -> q.isEmpty()
                || (u.getUsername() + " " + u.getFullName() + " " + u.getEmail()).toLowerCase(Locale.ROOT).contains(q)).toList();
    }
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public UserForm form(long id) {
        authorization.require(Permission.USER_WRITE);
        var user = get(id);
        UserForm form = new UserForm();
        form.setUsername(user.getUsername()); form.setFullName(user.getFullName()); form.setEmail(user.getEmail());
        form.setEnabled(user.isEnabled()); form.setVersion(user.getVersion());
        form.setRoleIds(user.getRoles().stream().map(Role::getId).collect(Collectors.toSet()));
        return form;
    }
    @Transactional
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public void create(UserForm form) {
        guard.lockMutations();
        var actor = authorization.require(Permission.USER_WRITE);
        var selected = resolveRoles(form.getRoleIds());
        if (!selected.isEmpty()) authorization.require(Permission.ROLE_ASSIGN);
        checkUnique(form, null);
        policies.forEach(policy -> policy.validate(new UserMutation(actor, null, form.isEnabled(), selected)));
        var user = users.saveAndFlush(factory.create(form, selected));
        events.publishEvent(new AccessChangedEvent(actor.getUsername(), "USER_CREATED", user.getUsername(), describe(user)));
    }
    @Transactional
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public void update(long id, UserForm form) {
        guard.lockMutations();
        var actor = authorization.require(Permission.USER_WRITE);
        var user = get(id);
        if (form.getVersion() == null || user.getVersion() != form.getVersion()) {
            throw new BusinessException("Пользователь уже изменён. Откройте форму заново");
        }
        if (!user.getUsername().equals(form.getUsername())) throw new BusinessException("Логин нельзя изменять");
        Set<Role> selected;
        if (actor.getPermissions().contains(Permission.ROLE_ASSIGN)) {
            selected = resolveRoles(form.getRoleIds());
        } else {
            if (!form.getRoleIds().isEmpty()) throw new AccessDeniedException("Недостаточно прав для назначения ролей");
            selected = user.getRoles();
        }
        checkUnique(form, id);
        policies.forEach(policy -> policy.validate(new UserMutation(actor, user, form.isEnabled(), selected)));
        if (form.getPassword() != null && !form.getPassword().isEmpty()) {
            UserFactory.validatePassword(form.getPassword());
            user.changePassword(encoder.encode(form.getPassword()));
        }
        user.update(form.getFullName().strip(), form.getEmail().strip().toLowerCase(Locale.ROOT), form.isEnabled(), selected);
        users.flush();
        events.publishEvent(new AccessChangedEvent(actor.getUsername(), "USER_UPDATED", user.getUsername(), describe(user)
                + (form.getPassword() != null && !form.getPassword().isEmpty() ? "; пароль обновлён" : "")));
    }
    private AdminUser get(long id) { return users.findById(id).orElseThrow(NotFoundException::new); }
    private Set<Role> resolveRoles(Set<Long> ids) {
        Set<Role> selected = new LinkedHashSet<>(roles.findAllById(ids));
        if (selected.size() != ids.size()) throw new BusinessException("Одна из выбранных ролей не найдена");
        return selected;
    }
    private void checkUnique(UserForm form, Long currentId) {
        users.findByUsername(form.getUsername().toLowerCase(Locale.ROOT)).filter(u -> !Objects.equals(u.getId(), currentId))
                .ifPresent(u -> { throw new BusinessException("Этот логин уже занят"); });
        users.findByEmail(form.getEmail().strip().toLowerCase(Locale.ROOT)).filter(u -> !Objects.equals(u.getId(), currentId))
                .ifPresent(u -> { throw new BusinessException("Этот email уже используется"); });
    }
    private String describe(AdminUser user) {
        return "Активен: " + user.isEnabled() + "; роли: " + user.getRoles().stream().map(Role::getName).sorted().collect(Collectors.joining(", "));
    }
}
