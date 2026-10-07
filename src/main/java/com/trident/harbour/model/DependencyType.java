package com.trident.harbour.model;

/**
 * REQUIRED: child cannot operate if this parent is down.
 * BACKUP: child keeps operating (possibly degraded) if this parent is down.
 */
public enum DependencyType {
    REQUIRED,
    BACKUP
}