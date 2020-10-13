package io.oreto.brew.security;

import io.oreto.brew.str.Str;

import java.util.ArrayList;
import java.util.List;

public final class Anonymous implements UserDetails {
    public Anonymous() { }

    @Override
    public String getUsername() {
        return Str.EMPTY;
    }

    @Override
    public final String getFirstName() {
        return Str.EMPTY;
    }

    @Override
    public final String getMiddleName() {
        return Str.EMPTY;
    }

    @Override
    public final String getLastName() {
        return Str.EMPTY;
    }

    @Override
    public final String getDisplayName() {
        return Str.EMPTY;
    }

    @Override
    public final List<String> getRoles() {
        return new ArrayList<>();
    }

    @Override
    public final boolean verifyPassword(String password) {
        return false;
    }

    @Override
    public final boolean setPassword(String password) {
        return false;
    }

    @Override
    public final boolean isAnonymous() {
        return true;
    }

    @Override
    public final boolean isAuthenticated() {
        return false;
    }
}
