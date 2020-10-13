package io.oreto.brew.web.route;

import io.oreto.brew.web.http.HttpVerb;

import java.util.Arrays;
import java.util.Comparator;

public class NamedRoute implements Comparable<NamedRoute>, Comparator<NamedRoute> {

    public static NamedRoute of(String name, String path, HttpVerb verb) {
        return new NamedRoute(name, path, verb);
    }

    public static NamedRoute of(String name, String path, String verb) {
        return new NamedRoute(name, path, verb);
    }

    public static NamedRoute of(String path, String verb) {
        return new NamedRoute(String.format("%s %s", verb, path)
                , path
                , verb);
    }

    private String name;
    private String path;
    private HttpVerb verb;
    private String nav;

    private NamedRoute(){};

    private NamedRoute(String name, String path, HttpVerb verb) {
        this.name = name;
        this.path = path;
        this.verb = verb;
    }

    private NamedRoute(String name, String path, String verb) {
        this.name = name;
        this.path = path;
        this.verb = HttpVerb.valueOf(verb);
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public HttpVerb getVerb() {
        return verb;
    }

    public NamedRoute withNav(String nav) {
        this.nav = nav;
        return this;
    }

    public String getNav() {
        return nav;
    }

    @Override
    public int compare(NamedRoute o1, NamedRoute o2) {
        return (o1).compareTo(o2);
    }

    @Override
    public int compareTo(NamedRoute o) {
        return this.name.compareTo(o.name);
    }

    @Override
    public String toString() {
        return with();
    }

    public String with(String... args) {
        return Routing.reverse(path, (Object[]) args);
    }

    public String by(String args) {
        return with(Arrays.stream(args == null ? new String[]{} : args.split(","))
                .map(String::trim).toArray(String[]::new));
    }
}
