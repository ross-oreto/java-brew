package io.oreto.brew.data.jpa;

import io.oreto.brew.map.MultiMap;

public class Fetcher {
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
