package com.company.admin;

import com.company.admin.domain.AdminUser;
import com.company.admin.domain.AuditEntry;
import com.company.admin.domain.Permission;
import com.company.admin.domain.Role;
import com.company.admin.repository.AuditRepository;
import com.company.admin.repository.RoleRepository;
import com.company.admin.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercises real authentication, rendered MVC pages and database-backed authorization. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired AuditRepository audit;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired EntityManager entityManager;

    @Test
    void anonymousRequestsRequireLoginAndLoginPageRenders() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
        for (String path : new String[]{"/", "/users", "/roles", "/audit"}) {
            mvc.perform(get(path)).andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }
    }

    @Test
    void adminCanRenderEveryManagementPage() throws Exception {
        MockHttpSession session = login("admin", "AdminDemo2026!");
        long userId = user("manager").getId();
        long roleId = role("MANAGER").getId();
        for (String path : new String[]{"/", "/users", "/users/new", "/users/" + userId + "/edit",
                "/roles", "/roles/new", "/roles/" + roleId + "/edit", "/audit"}) {
            mvc.perform(get(path).session(session)).andExpect(status().isOk());
        }
    }

    @Test
    void seededPermissionsControlPageAccess() throws Exception {
        MockHttpSession manager = login("manager", "ManagerDemo2026!");
        mvc.perform(get("/users").session(manager)).andExpect(status().isOk());
        mvc.perform(get("/roles").session(manager)).andExpect(status().isOk());
        mvc.perform(get("/audit").session(manager)).andExpect(status().isForbidden());
        mvc.perform(get("/roles/new").session(manager)).andExpect(status().isForbidden());

        MockHttpSession auditor = login("auditor", "AuditorDemo2026!");
        mvc.perform(get("/users").session(auditor)).andExpect(status().isOk());
        mvc.perform(get("/roles").session(auditor)).andExpect(status().isOk());
        mvc.perform(get("/audit").session(auditor)).andExpect(status().isOk());
        mvc.perform(get("/users/new").session(auditor)).andExpect(status().isForbidden());
        mvc.perform(get("/roles/new").session(auditor)).andExpect(status().isForbidden());
    }

    @Test
    void adminCreatesUserWithEncodedPasswordAssignedRoleAndAuditEntry() throws Exception {
        MockHttpSession admin = login("admin", "AdminDemo2026!");
        long auditBefore = audit.count();
        Role auditorRole = role("AUDITOR");
        mvc.perform(newUser("newuser").param("roleIds", auditorRole.getId().toString())
                        .session(admin).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/users"));

        refreshPersistence();
        AdminUser created = user("newuser");
        assertThat(created.isEnabled()).isTrue();
        assertThat(created.getRoles()).extracting(Role::getName).containsExactly("AUDITOR");
        assertThat(created.getPasswordHash()).isNotEqualTo("NewUser2026!");
        assertThat(passwordEncoder.matches("NewUser2026!", created.getPasswordHash())).isTrue();
        assertThat(audit.count()).isEqualTo(auditBefore + 1);
        assertThat(audit.findAll()).extracting(AuditEntry::getActor).contains("admin");
        assertThat(audit.findAll()).allSatisfy(entry -> {
            String recorded = entry.getActor() + entry.getAction() + entry.getTarget() + entry.getDetails();
            assertThat(recorded).doesNotContain("NewUser2026!", created.getPasswordHash());
        });
        mvc.perform(get("/audit").session(admin)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("newuser")));
    }

    @Test
    void managerCanCreateAccountWithoutGrantingRolesOrPermissions() throws Exception {
        MockHttpSession manager = login("manager", "ManagerDemo2026!");
        mvc.perform(newUser("noroles").session(manager).with(csrf()))
                .andExpect(status().is3xxRedirection());
        refreshPersistence();
        assertThat(user("noroles").getRoles()).isEmpty();
        assertThat(user("noroles").getPermissions()).isEmpty();
        MockHttpSession limited = login("noroles", "NewUser2026!");
        mvc.perform(get("/users").session(limited)).andExpect(status().isForbidden());
    }

    @Test
    void managerUpdatesProfileWhileExistingRolesAndPasswordArePreserved() throws Exception {
        MockHttpSession manager = login("manager", "ManagerDemo2026!");
        AdminUser auditor = user("auditor");
        String hashBefore = auditor.getPasswordHash();
        mvc.perform(editUser(auditor, true, "Updated Auditor", auditor.getVersion())
                        .session(manager).with(csrf()))
                .andExpect(status().is3xxRedirection());
        refreshPersistence();
        AdminUser updated = user("auditor");
        assertThat(updated.getFullName()).isEqualTo("Updated Auditor");
        assertThat(updated.getRoles()).extracting(Role::getName).containsExactly("AUDITOR");
        assertThat(updated.getPasswordHash()).isEqualTo(hashBefore);
    }

    @Test
    void managerCannotForgeRoleAssignmentWhenCreatingUser() throws Exception {
        MockHttpSession manager = login("manager", "ManagerDemo2026!");
        long countBefore = users.count();
        long auditBefore = audit.count();
        mvc.perform(newUser("escalated").param("roleIds", role("ADMIN").getId().toString())
                        .session(manager).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(users.count()).isEqualTo(countBefore);
        assertThat(users.findByUsername("escalated")).isEmpty();
        assertThat(audit.count()).isEqualTo(auditBefore);
    }

    @Test
    void managerCannotAssignAdminToOwnAccount() throws Exception {
        MockHttpSession manager = login("manager", "ManagerDemo2026!");
        AdminUser target = user("manager");
        long auditBefore = audit.count();
        mvc.perform(editUser(target, true).param("roleIds", role("ADMIN").getId().toString())
                        .session(manager).with(csrf()))
                .andExpect(status().isForbidden());
        refreshPersistence();
        assertThat(user("manager").hasRole("ADMIN")).isFalse();
        assertThat(user("manager").hasRole("MANAGER")).isTrue();
        assertThat(audit.count()).isEqualTo(auditBefore);
    }

    @Test
    void managerCannotEditAnAdministratorAccount() throws Exception {
        MockHttpSession manager = login("manager", "ManagerDemo2026!");
        AdminUser administrator = user("admin");
        String originalName = administrator.getFullName();
        long auditBefore = audit.count();
        mvc.perform(editUser(administrator, true, "Unauthorized Change", administrator.getVersion())
                        .session(manager).with(csrf()))
                .andExpect(status().isForbidden());
        refreshPersistence();
        assertThat(user("admin").getFullName()).isEqualTo(originalName);
        assertThat(audit.count()).isEqualTo(auditBefore);
    }

    @ParameterizedTest
    @EnumSource(value = Permission.class, names = {"ROLE_WRITE", "ROLE_ASSIGN"})
    void managerCannotTakeOverAccountWithDelegatedAccessAdministration(Permission privilegedPermission) throws Exception {
        Role delegated = roles.saveAndFlush(new Role("PRIVILEGED_TARGET", "Delegated access management", false,
                Set.of(Permission.USER_READ, privilegedPermission)));
        AdminUser target = users.saveAndFlush(new AdminUser("delegate", "Delegated Administrator", "delegate@example.com",
                passwordEncoder.encode("Delegate2026!"), true, Set.of(delegated)));
        String originalHash = target.getPasswordHash();
        long auditBefore = audit.count();
        MockHttpSession manager = login("manager", "ManagerDemo2026!");
        mvc.perform(editUser(target, true, "Taken Over", target.getVersion(), "AttackerPass2026!")
                        .session(manager).with(csrf()))
                .andExpect(status().isForbidden());
        refreshPersistence();
        assertThat(user("delegate").getFullName()).isEqualTo("Delegated Administrator");
        assertThat(user("delegate").getPasswordHash()).isEqualTo(originalHash);
        assertThat(user("delegate").getPermissions()).contains(privilegedPermission);
        assertThat(audit.count()).isEqualTo(auditBefore);
    }

    @ParameterizedTest
    @EnumSource(value = Permission.class, names = {"ROLE_WRITE", "ROLE_ASSIGN"})
    void delegatedRoleWriterCannotGrantReservedAccessAdministration(Permission privilegedPermission) throws Exception {
        Role operatorRole = roles.saveAndFlush(new Role("ROLE_OPERATOR", "Delegated role writer", false,
                Set.of(Permission.ROLE_READ, Permission.ROLE_WRITE)));
        users.saveAndFlush(new AdminUser("roleoperator", "Role Operator", "roleoperator@example.com",
                passwordEncoder.encode("RoleOperator2026!"), true, Set.of(operatorRole)));
        Role ordinaryRole = roles.saveAndFlush(new Role("ORDINARY_ROLE", "Ordinary role", false,
                Set.of(Permission.USER_READ)));
        MockHttpSession operator = login("roleoperator", "RoleOperator2026!");
        long roleCount = roles.count();
        long auditBefore = audit.count();
        mvc.perform(post("/roles/new").param("name", "RESERVED_GRANT").param("description", "Forbidden grant")
                        .param("permissions", privilegedPermission.name()).session(operator).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(editRole(ordinaryRole).param("permissions", privilegedPermission.name())
                        .session(operator).with(csrf()))
                .andExpect(status().isForbidden());
        refreshPersistence();
        assertThat(roles.count()).isEqualTo(roleCount);
        assertThat(role("ORDINARY_ROLE").getPermissions()).containsExactly(Permission.USER_READ);
        assertThat(audit.count()).isEqualTo(auditBefore);
    }

    @Test
    void readOnlyUserCannotMutateUsersAndManagerCannotMutateRoles() throws Exception {
        MockHttpSession auditor = login("auditor", "AuditorDemo2026!");
        long userCount = users.count();
        mvc.perform(newUser("denieduser").session(auditor).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(users.count()).isEqualTo(userCount);

        MockHttpSession manager = login("manager", "ManagerDemo2026!");
        long roleCount = roles.count();
        mvc.perform(post("/roles/new").param("name", "DENIED_ROLE").param("description", "Denied")
                        .param("permissions", "USER_WRITE").session(manager).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(roles.count()).isEqualTo(roleCount);
    }

    @Test
    void mutationWithoutCsrfIsRejectedAndLogoutRequiresPost() throws Exception {
        MockHttpSession admin = login("admin", "AdminDemo2026!");
        long countBefore = users.count();
        mvc.perform(newUser("csrfdenied").session(admin)).andExpect(status().isForbidden());
        assertThat(users.count()).isEqualTo(countBefore);
        mvc.perform(get("/logout").session(admin));
        mvc.perform(get("/users").session(admin)).andExpect(status().isOk());
        mvc.perform(post("/logout").session(admin).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(unauthenticated());
    }

    @Test
    void administratorCanCreateAndEditPermissionSetOfCustomRole() throws Exception {
        MockHttpSession admin = login("admin", "AdminDemo2026!");
        mvc.perform(post("/roles/new").param("name", "SUPPORT").param("description", "Support staff")
                        .param("permissions", "USER_READ", "ROLE_READ").session(admin).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/roles"));
        refreshPersistence();
        Role created = role("SUPPORT");
        assertThat(created.isSystemRole()).isFalse();
        assertThat(created.getPermissions()).containsExactlyInAnyOrder(Permission.USER_READ, Permission.ROLE_READ);

        mvc.perform(editRole(created).param("permissions", "USER_READ", "AUDIT_READ")
                        .session(admin).with(csrf()))
                .andExpect(status().is3xxRedirection());
        refreshPersistence();
        assertThat(role("SUPPORT").getPermissions())
                .containsExactlyInAnyOrder(Permission.USER_READ, Permission.AUDIT_READ);
    }

    @Test
    void systemAdminRoleCannotBeChanged() throws Exception {
        MockHttpSession admin = login("admin", "AdminDemo2026!");
        Role administrator = role("ADMIN");
        long auditBefore = audit.count();
        mvc.perform(editRole(administrator, "RENAMED_ADMIN")
                        .param("permissions", "USER_READ").session(admin).with(csrf()))
                .andExpect(status().isOk()).andExpect(model().attributeHasErrors("roleForm"));
        refreshPersistence();
        assertThat(roles.findByName("RENAMED_ADMIN")).isEmpty();
        assertThat(role("ADMIN").getPermissions()).containsExactlyInAnyOrder(Permission.values());
        assertThat(audit.count()).isEqualTo(auditBefore);
    }

    @Test
    void lastActiveAdministratorCannotBeDisabled() throws Exception {
        MockHttpSession admin = login("admin", "AdminDemo2026!");
        AdminUser administrator = user("admin");
        long auditBefore = audit.count();
        mvc.perform(editUser(administrator, false).param("roleIds", role("ADMIN").getId().toString())
                        .session(admin).with(csrf()))
                .andExpect(status().isOk()).andExpect(model().attributeHasErrors("userForm"));
        refreshPersistence();
        assertThat(user("admin").isEnabled()).isTrue();
        assertThat(user("admin").hasRole("ADMIN")).isTrue();
        assertThat(audit.count()).isEqualTo(auditBefore);
    }

    @Test
    void lastActiveAdministratorCannotBeDemoted() throws Exception {
        MockHttpSession admin = login("admin", "AdminDemo2026!");
        long auditBefore = audit.count();
        mvc.perform(editUser(user("admin"), true).param("roleIds", role("AUDITOR").getId().toString())
                        .session(admin).with(csrf()))
                .andExpect(status().isOk()).andExpect(model().attributeHasErrors("userForm"));
        refreshPersistence();
        assertThat(user("admin").hasRole("ADMIN")).isTrue();
        assertThat(users.countByEnabledTrueAndRoles_Name("ADMIN")).isEqualTo(1);
        assertThat(audit.count()).isEqualTo(auditBefore);
    }

    @Test
    void changedPermissionsApplyToAlreadyAuthenticatedSession() throws Exception {
        Role dynamicRole = roles.saveAndFlush(new Role("SESSION_EDITOR", "Session editor", false,
                Set.of(Permission.USER_READ, Permission.USER_WRITE)));
        users.saveAndFlush(new AdminUser("sessioneditor", "Session Editor", "sessioneditor@example.com",
                passwordEncoder.encode("SessionEditor2026!"), true, Set.of(dynamicRole)));
        MockHttpSession editor = login("sessioneditor", "SessionEditor2026!");
        mvc.perform(get("/users/new").session(editor)).andExpect(status().isOk());

        MockHttpSession admin = login("admin", "AdminDemo2026!");
        mvc.perform(editRole(dynamicRole).param("permissions", "USER_READ").session(admin).with(csrf()))
                .andExpect(status().is3xxRedirection());
        refreshPersistence();
        mvc.perform(get("/users").session(editor)).andExpect(status().isOk());
        mvc.perform(newUser("revokedwrite").session(editor).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(users.findByUsername("revokedwrite")).isEmpty();

        mvc.perform(editRole(role("SESSION_EDITOR")).param("permissions", "USER_READ", "USER_WRITE")
                        .session(admin).with(csrf()))
                .andExpect(status().is3xxRedirection());
        refreshPersistence();
        mvc.perform(get("/users/new").session(editor)).andExpect(status().isOk());
    }

    @Test
    void disablingUserInvalidatesExistingSessionAndPreventsNewLogin() throws Exception {
        MockHttpSession manager = login("manager", "ManagerDemo2026!");
        MockHttpSession admin = login("admin", "AdminDemo2026!");
        mvc.perform(editUser(user("manager"), false).param("roleIds", role("MANAGER").getId().toString())
                        .session(admin).with(csrf()))
                .andExpect(status().is3xxRedirection());
        refreshPersistence();
        assertThat(user("manager").isEnabled()).isFalse();
        mvc.perform(get("/users").session(manager)).andExpect(status().is3xxRedirection())
                .andExpect(unauthenticated());
        mvc.perform(formLogin("/login").user("manager").password("ManagerDemo2026!"))
                .andExpect(unauthenticated()).andExpect(status().is3xxRedirection());
    }

    @Test
    void changingPasswordInvalidatesExistingSessionAndReplacesLoginCredential() throws Exception {
        MockHttpSession manager = login("manager", "ManagerDemo2026!");
        MockHttpSession admin = login("admin", "AdminDemo2026!");
        AdminUser account = user("manager");
        mvc.perform(editUser(account, true, account.getFullName(), account.getVersion(), "ChangedPass2026!")
                        .param("roleIds", role("MANAGER").getId().toString()).session(admin).with(csrf()))
                .andExpect(status().is3xxRedirection());
        refreshPersistence();
        mvc.perform(get("/users").session(manager)).andExpect(status().is3xxRedirection())
                .andExpect(unauthenticated());
        mvc.perform(formLogin("/login").user("manager").password("ManagerDemo2026!"))
                .andExpect(unauthenticated());
        login("manager", "ChangedPass2026!");
    }

    @Test
    void invalidOrDuplicateInputDoesNotCreateUserOrAuditRecord() throws Exception {
        MockHttpSession admin = login("admin", "AdminDemo2026!");
        long userCount = users.count();
        long auditCount = audit.count();
        mvc.perform(newUser("invalidemail", "not-an-email").session(admin).with(csrf()))
                .andExpect(status().isOk()).andExpect(model().attributeHasErrors("userForm"));
        mvc.perform(newUser("manager").session(admin).with(csrf()))
                .andExpect(status().isOk()).andExpect(model().attributeHasErrors("userForm"));
        mvc.perform(newUser("unknownrole").param("roleIds", "999999999").session(admin).with(csrf()))
                .andExpect(status().isOk()).andExpect(model().attributeHasErrors("userForm"));
        assertThat(users.count()).isEqualTo(userCount);
        assertThat(audit.count()).isEqualTo(auditCount);
    }

    @Test
    void updatesRequireCurrentVersionAndStaleFormCannotOverwriteData() throws Exception {
        MockHttpSession admin = login("admin", "AdminDemo2026!");
        AdminUser manager = user("manager");
        long initialVersion = manager.getVersion();
        String originalName = manager.getFullName();
        mvc.perform(post("/users/" + manager.getId()).param("username", manager.getUsername())
                        .param("fullName", "Missing Version").param("email", manager.getEmail())
                        .param("password", "").param("enabled", "true")
                        .param("roleIds", role("MANAGER").getId().toString()).session(admin).with(csrf()))
                .andExpect(status().isOk()).andExpect(model().attributeHasErrors("userForm"));
        refreshPersistence();
        assertThat(user("manager").getFullName()).isEqualTo(originalName);

        manager = user("manager");
        mvc.perform(editUser(manager, true, "Current Change", manager.getVersion())
                        .param("roleIds", role("MANAGER").getId().toString()).session(admin).with(csrf()))
                .andExpect(status().is3xxRedirection());
        refreshPersistence();
        assertThat(user("manager").getVersion()).isGreaterThan(initialVersion);
        mvc.perform(editUser(user("manager"), true, "Stale Change", initialVersion)
                        .param("roleIds", role("MANAGER").getId().toString())
                        .session(admin).with(csrf()))
                .andExpect(status().isOk()).andExpect(model().attributeHasErrors("userForm"));
        refreshPersistence();
        assertThat(user("manager").getFullName()).isEqualTo("Current Change");
    }

    @Test
    void wrongPasswordDoesNotAuthenticate() throws Exception {
        mvc.perform(formLogin("/login").user("admin").password("wrong-password"))
                .andExpect(unauthenticated()).andExpect(status().is3xxRedirection());
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = mvc.perform(formLogin("/login").user(username).password(password))
                .andExpect(authenticated().withUsername(username)).andExpect(status().is3xxRedirection())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private MockHttpServletRequestBuilder newUser(String username) {
        return newUser(username, username + "@example.com");
    }

    private MockHttpServletRequestBuilder newUser(String username, String email) {
        return post("/users/new").param("username", username).param("fullName", "Test User")
                .param("email", email).param("password", "NewUser2026!")
                .param("enabled", "true");
    }

    private MockHttpServletRequestBuilder editUser(AdminUser user, boolean enabled) {
        return editUser(user, enabled, user.getFullName(), user.getVersion());
    }

    private MockHttpServletRequestBuilder editUser(AdminUser user, boolean enabled, String fullName, long version) {
        return editUser(user, enabled, fullName, version, "");
    }

    private MockHttpServletRequestBuilder editUser(AdminUser user, boolean enabled, String fullName, long version, String password) {
        return post("/users/" + user.getId()).param("version", Long.toString(version))
                .param("username", user.getUsername()).param("fullName", fullName)
                .param("email", user.getEmail()).param("password", password).param("enabled", Boolean.toString(enabled));
    }

    private MockHttpServletRequestBuilder editRole(Role role) {
        return editRole(role, role.getName());
    }

    private MockHttpServletRequestBuilder editRole(Role role, String name) {
        return post("/roles/" + role.getId()).param("version", Long.toString(role.getVersion()))
                .param("name", name).param("description", role.getDescription());
    }

    private AdminUser user(String username) {
        return users.findByUsername(username).orElseThrow();
    }

    private Role role(String name) {
        return roles.findByName(name).orElseThrow();
    }

    private void refreshPersistence() {
        entityManager.flush();
        entityManager.clear();
    }
}
