package com.company.admin.repository;

import com.company.admin.domain.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByUsername(String username);
    Optional<AdminUser> findByEmail(String email);
    List<AdminUser> findAllByOrderByIdAsc();
    long countByEnabledTrue();
    long countByEnabledTrueAndRoles_Name(String name);
}
