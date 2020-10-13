package io.oreto.brew;

public interface Environment {

    boolean isActive(String name, String... names);
    default boolean isLocal() { return isActive(Env.local.name()); }
    default boolean isDev() { return isActive(Env.dev.name()); }
    default boolean isUat() { return isActive(Env.uat.name()); }
    default boolean isProd() { return isActive(Env.prod.name()); }
    default boolean isTest() { return isActive(Env.test.name()); }
    default boolean isDebugging() { return isActive(Env.local.name(),Env.dev.name()); }
    default boolean isNotDebugging() { return !isDebugging(); }
}
