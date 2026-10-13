package com.company.admin.service.policy;

/** Strategies validate the same mutation; implementations must not modify persisted state. */
public interface AccessMutationPolicy {
    void validate(UserMutation mutation);
}
