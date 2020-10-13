package io.oreto.brew.security;

import java.util.ArrayList;
import java.util.List;

public interface UserDetails {
    default String getUsername() { return null; }
    default String getFirstName() { return null; }
    default String getMiddleName() { return null; }
    default String getLastName() { return null; }
    default String getDisplayName() { return null; }
    default List<String> getRoles() { return new ArrayList<>(); }

    boolean verifyPassword(String password);
    boolean setPassword(String password);

    default boolean isAnonymous() { return false; }
    default boolean isAuthenticated() { return !isAnonymous(); }

    default boolean hasAllRoles(List<String> roles) {
        return roles.isEmpty() || getRoles().containsAll(roles);
    }
    default boolean hasRole(List<String> roles) {
        return roles.isEmpty() || roles.stream().distinct().anyMatch(getRoles()::contains);
    }
    default boolean hasRole(String role) {
        return getRoles().contains(role);
    }
}
