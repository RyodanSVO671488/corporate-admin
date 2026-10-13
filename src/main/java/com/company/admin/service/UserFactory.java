package com.company.admin.service;

import com.company.admin.domain.*;
import com.company.admin.web.form.UserForm;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/** Encapsulates the invariant that a new user can never contain a plaintext password. */
@Component
public class UserFactory {
    private final PasswordEncoder encoder;
    public UserFactory(PasswordEncoder encoder) { this.encoder = encoder; }
    public AdminUser create(UserForm form, Set<Role> roles) {
        validatePassword(form.getPassword());
        return new AdminUser(form.getUsername().toLowerCase(Locale.ROOT), form.getFullName().strip(),
                form.getEmail().strip().toLowerCase(Locale.ROOT), encoder.encode(form.getPassword()), form.isEnabled(), roles);
    }
    public static void validatePassword(String password) {
        if (password == null || password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException("Пароль должен содержать от 12 символов до 72 байт UTF-8");
        }
    }
}
