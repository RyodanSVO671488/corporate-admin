package com.company.admin.service;

public class NotFoundException extends RuntimeException {
    public NotFoundException() { super("Запись не найдена"); }
}
