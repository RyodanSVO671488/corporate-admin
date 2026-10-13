package com.company.admin.web.form;

import com.company.admin.domain.Permission;
import jakarta.validation.constraints.*;
import java.util.LinkedHashSet;
import java.util.Set;

public class RoleForm {
    @NotBlank(message = "Укажите название") @Pattern(regexp = "[A-Z][A-Z0-9_]{2,39}", message = "Название: 3–40 заглавных латинских букв, цифр или подчёркиваний") private String name;
    @NotBlank(message = "Укажите описание") @Size(max = 180, message = "Описание не длиннее 180 символов") private String description;
    private Long version;
    private Set<Permission> permissions = new LinkedHashSet<>();
    public String getName() { return name; } public void setName(String value) { name = value; }
    public String getDescription() { return description; } public void setDescription(String value) { description = value; }
    public Long getVersion() { return version; } public void setVersion(Long value) { version = value; }
    public Set<Permission> getPermissions() { return permissions; } public void setPermissions(Set<Permission> value) { permissions = value == null ? new LinkedHashSet<>() : value; }
}
