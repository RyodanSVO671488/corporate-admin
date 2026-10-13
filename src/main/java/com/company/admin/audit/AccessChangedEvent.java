package com.company.admin.audit;

/** Explicitly excludes passwords, hashes and untrusted full form contents. */
public record AccessChangedEvent(String actor, String action, String target, String details) { }
