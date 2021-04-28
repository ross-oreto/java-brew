package io.oreto.brew.data.jpa;

import io.oreto.brew.map.MultiMap;
import io.oreto.brew.str.Str;

import java.util.Stack;

public class Fetcher {
    private static Fetch.Plan parse(String s, Stack<String> path) {
        if (Str.isBlank(s))
            return Fetch.Plan.none();
        Fetcher fetch = Fetch.join();
        Str str = Str.of();
        boolean spaced = false;
        boolean collection = false;
        boolean range = false;
        String field = null;

        int len = s.length();
        int end = len - 1;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (collection) {
                if (c == ' ') continue;
                str.add(c);
                if (c == ']') {
                    if (range) fetch.at(String.join(".", path)).get(str.toString());
                    else fetch.at(String.join(".", path)).join(str.trim().toString());
                    str.delete();
                    collection = false;
                    range = false;
                    spaced = false;
                } else {
                    range = true;
                }
                continue;
            }

            switch (c) {
                case '{':
                    if (field == null) field = str.trim().toString();
                    if (!field.isEmpty())
                        path.push(field);
                    field = null;
                    str.delete();
                    spaced = false;
                    break;
                case '}':
                    if (str.isNotEmpty())
                        fetch.at(String.join(".", path)).join(str.trim().toString());
                    field = null;
                    str.delete();
                    spaced = false;
                    path.pop();
                    break;
                case '[':
                    if (str.isBlank()) throw new RuntimeException("field name expected before []");
                    if (field == null) field = str.trim().toString();
                    collection = true;
                    str.add(c);
                    break;
                case ' ':
                    if (str.isNotEmpty()) {
                        spaced = true;
                        if (field == null) field = str.trim().toString();
                    }
                    break;
                default:
                    if (i == end || spaced) {
                        fetch.at(String.join(".", path)).join(str.trim().toString());
                        str.delete();
                        spaced = false;
                        field = null;
                    }
                    str.add(c);
                    break;
            }
        }
        Fetch.Plan plan = fetch.buildPlan();
        return plan.isEmpty() ? Fetch.Plan.none() : plan;
    }

    public static Fetch.Plan parse(String s) {
       return parse(s, new Stack<>());
    }

    private String path = "";
    protected final MultiMap<String, Fetch> joinMap = new MultiMap<>();
    protected final MultiMap<String, Fetch> queryMap = new MultiMap<>();

    public Fetcher join(String... fields) {
        for (String name : fields) {
            Fetch fetch = new Fetch(name);
            fetch.type = Fetch.Type.join;
            joinMap.put(path, fetch);
        }
        return this;
    }

    public Fetcher get(String... fields) {
        for (String name : fields) {
            Fetch fetch = new Fetch(name);
            fetch.type = Fetch.Type.query;
            queryMap.put(path, fetch);
        }
        return this;
    }

    public Fetcher at(String path) {
        this.path = path;
        return this;
    }

    public Fetch.Plan buildPlan() {
        return new Fetch.Plan(this);
    }
}
