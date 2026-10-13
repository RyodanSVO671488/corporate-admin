package com.company.admin.config;

import com.company.admin.domain.*;
import com.company.admin.repository.*;
import com.company.admin.service.UserFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Component
public class BootstrapData implements ApplicationRunner {
    private final UserRepository users;
    private final RoleRepository roles;
    private final AccessGuardRepository guard;
    private final PasswordEncoder encoder;
    private final boolean demo;
    private final String username;
    private final String password;
    private final String email;
    public BootstrapData(UserRepository users, RoleRepository roles, AccessGuardRepository guard, PasswordEncoder encoder,
            @Value("${app.bootstrap.demo:false}") boolean demo,
            @Value("${app.bootstrap.username:admin}") String username,
            @Value("${app.bootstrap.password:}") String password,
            @Value("${app.bootstrap.email:admin@example.com}") String email) {
        this.users = users; this.roles = roles; this.guard = guard; this.encoder = encoder;
        this.demo = demo; this.username = username; this.password = password; this.email = email;
    }
    @Override @Transactional
    public void run(ApplicationArguments args) {
        guard.lockMutations();
        if (users.count() != 0) return;
        UserFactory.validatePassword(password);
        if (!username.matches("[a-zA-Z0-9._-]{3,40}") || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") || email.length() > 160)
            throw new IllegalStateException("Некорректные параметры ADMIN_USERNAME или ADMIN_EMAIL");
        Role admin = roles.findByName("ADMIN").orElseGet(() -> roles.save(new Role("ADMIN", "Полный доступ к администрированию", true, EnumSet.allOf(Permission.class))));
        users.save(new AdminUser(username.toLowerCase(Locale.ROOT), "Администратор", email.toLowerCase(Locale.ROOT), encoder.encode(password), true, Set.of(admin)));
        if (demo) {
            Role manager = roles.save(new Role("MANAGER", "Работа с учётными записями без назначения ролей", false, EnumSet.of(Permission.USER_READ, Permission.USER_WRITE, Permission.ROLE_READ)));
            Role auditor = roles.save(new Role("AUDITOR", "Просмотр пользователей, ролей и журнала действий", false, EnumSet.of(Permission.USER_READ, Permission.ROLE_READ, Permission.AUDIT_READ)));
            users.save(new AdminUser("manager", "Анна Смирнова", "manager@example.com", encoder.encode("ManagerDemo2026!"), true, Set.of(manager)));
            users.save(new AdminUser("auditor", "Илья Волков", "auditor@example.com", encoder.encode("AuditorDemo2026!"), true, Set.of(auditor)));
        }
    }
}
