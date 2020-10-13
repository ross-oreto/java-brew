package io.oreto.brew;

import java.util.Arrays;

public enum Env {
    local
    , dev
    , uat
    , prod
    , test
    , other;

    @Override
    public String toString() {
        return name();
    }

    public Env from(String value) {
        return Arrays.stream(values()).anyMatch(it -> it.name().equals(value)) ? Env.valueOf(value) : Env.other;
    }
}
