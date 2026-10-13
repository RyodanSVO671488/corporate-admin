package com.company.admin.security;

import com.company.admin.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository users;
    public DatabaseUserDetailsService(UserRepository users) { this.users = users; }
    @Override @Transactional(readOnly = true)
    public SecurityUser loadUserByUsername(String username) {
        return users.findByUsername(username.toLowerCase(Locale.ROOT)).map(SecurityUser::from)
                .orElseThrow(() -> new UsernameNotFoundException("Неверный логин или пароль"));
    }
}
