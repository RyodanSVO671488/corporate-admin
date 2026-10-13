package com.company.admin.domain;

public enum Permission {
    USER_READ("Просмотр пользователей"),
    USER_WRITE("Создание и изменение пользователей"),
    ROLE_READ("Просмотр ролей"),
    ROLE_WRITE("Создание и изменение прав ролей"),
    ROLE_ASSIGN("Назначение ролей пользователям"),
    AUDIT_READ("Просмотр журнала действий");

    private final String label;
    Permission(String label) { this.label = label; }
    public String getLabel() { return label; }
}
