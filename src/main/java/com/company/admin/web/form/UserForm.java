package com.company.admin.web.form;

import jakarta.validation.constraints.*;
import java.util.LinkedHashSet;
import java.util.Set;

public class UserForm {
    @NotBlank(message = "Укажите логин") @Pattern(regexp = "[a-zA-Z0-9._-]{3,40}", message = "Логин: 3–40 латинских букв, цифр, точек, дефисов или подчёркиваний")
    private String username;
    @NotBlank(message = "Укажите имя") @Size(max = 100, message = "Имя не длиннее 100 символов") private String fullName;
    @NotBlank(message = "Укажите email") @Email(message = "Некорректный email") @Size(max = 160) private String email;
    @Size(max = 72, message = "Пароль не длиннее 72 символов") private String password = "";
    private boolean enabled = true;
    private Long version;
    private Set<Long> roleIds = new LinkedHashSet<>();
    public String getUsername() { return username; } public void setUsername(String value) { username = value; }
    public String getFullName() { return fullName; } public void setFullName(String value) { fullName = value; }
    public String getEmail() { return email; } public void setEmail(String value) { email = value; }
    public String getPassword() { return password; } public void setPassword(String value) { password = value; }
    public boolean isEnabled() { return enabled; } public void setEnabled(boolean value) { enabled = value; }
    public Long getVersion() { return version; } public void setVersion(Long value) { version = value; }
    public Set<Long> getRoleIds() { return roleIds; } public void setRoleIds(Set<Long> value) { roleIds = value == null ? new LinkedHashSet<>() : value; }
}
