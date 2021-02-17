package io.oreto.brew.data.jpa;

import io.oreto.brew.data.Sort;
import io.oreto.brew.data.Sortable;
import io.oreto.brew.map.MultiMap;
import io.oreto.brew.str.Str;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Fetch {

    public enum Type {
        join, query
    }

    private static String[] mapPaths(MultiMap<String, Fetch> fetchMultiMap) {
        return fetchMultiMap.keySet().stream().sorted((s1, s2) -> {
            int comp = Long.compare(s1.chars().filter(c -> c == '.').count(), s2.chars().filter(c -> c == '.').count());
            return comp == 0 ? Integer.compare(s1.length(), s2.length()) : comp;
        }).toArray(String[]::new);
    }

    public static Fetcher join(String... name) {
        Fetcher fetcher = new Fetcher();
        fetcher.join(name);
        return fetcher;
    }

    public static Fetcher get(String... name) {
        Fetcher fetcher = new Fetcher();
        fetcher.get(name);
        return fetcher;
    }

    protected final String name;
    protected Type type;
    protected boolean isCollection;
    private int limit = 20;
    private long offset = 0;
    private List<Sortable> sorting;

    protected Fetch(String name) {
        this.isCollection = name.contains("[") && name.contains("]");
        this.name = this.isCollection ? name.substring(0, name.indexOf('[')).trim() : name.trim();
        String index = this.isCollection ? name.substring(name.indexOf('[') + 1, name.indexOf(']')).trim() : "";
        if (Str.isNotEmpty(index)) {
            if (type == Type.join)
                throw new RuntimeException("Cannot limit collection with a join fetch type");
            String[] indices = index.split(":");
            if (indices.length > 1) {
                offset = Str.toInteger(indices[0]).orElseThrow(() ->
                        new RuntimeException("fetch collection index must an integer: " + indices[0]));
                limit = Str.toInteger(indices[1]).orElseThrow(() ->
                        new RuntimeException("fetch collection index must an integer: " + indices[1]));
            } else {
                limit = Str.toInteger(indices[0]).orElseThrow(() ->
                        new RuntimeException("fetch collection index must an integer: " + indices[0]));
            }
        }

        this.sorting = new ArrayList<>();
        if (this.isCollection && name.contains("(") && name.contains(")")) {
            String sorting = name.substring(name.indexOf('(') + 1, name.indexOf(')')).trim();
            this.sort(Arrays.stream(sorting.split(",")).map(s -> {
               String[] sort = s.split(":");
               return Sort.of(sort[0].trim(), sort.length > 1 ? sort[1].trim() : Sort.Direction.asc.name());
            }).toArray(Sortable[]::new));
        }
    }

    public Fetch sort(Sortable... sortable) {
        if (type == Type.join)
            throw new RuntimeException("Cannot order collection with a join fetch type");
        this.sorting.addAll(Arrays.stream(sortable).collect(Collectors.toList()));
        return this;
    }

    public String getName() {
        return name;
    }
    public Type getType() {
        return type;
    }
    public boolean isCollection() {
        return isCollection;
    }
    public int getLimit() {
        return limit;
    }
    public long getOffset() {
        return offset;
    }
    public List<Sortable> getSorting() {
        return sorting;
    }

    public static class Plan {
        public static Plan none() {
            return new Plan();
        }

        private final MultiMap<String, Fetch> joinMap;
        private final MultiMap<String, Fetch> queryMap;

        private final String[] joinPaths;
        private final String[] queryPaths;

        protected Plan(Fetcher fetcher) {
            this.joinMap = fetcher.joinMap;
            this.queryMap = fetcher.queryMap;
            this.joinPaths = mapPaths(joinMap);
            this.queryPaths = mapPaths(queryMap);
        }

        protected Plan() {
            this.joinMap = new MultiMap<>();
            this.queryMap= new MultiMap<>();
            this.joinPaths = mapPaths(joinMap);
            this.queryPaths = mapPaths(queryMap);
        }

        public boolean isEmpty() {
            return joinMap.isEmpty() && queryMap.isEmpty();
        }

        public boolean hasJoins() {
            return !joinMap.isEmpty();
        }

        public boolean hasQueries() {
            return !queryMap.isEmpty();
        }

        public List<Fetch> joins(String path) {
            return joinMap.get(path);
        }

        public List<Fetch> queries(String path) {
            return queryMap.get(path);
        }

        public String[] getJoinPaths() {
            return joinPaths;
        }

        public String[] getQueryPaths() {
            return queryPaths;
        }

        public void clearJoins() {
            joinMap.clear();
        }
    }
}
