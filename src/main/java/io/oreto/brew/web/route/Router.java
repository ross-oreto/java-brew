package io.oreto.brew.web.route;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;

public class Router {
    /**
     * Recreate a path pattern using the given variables. Variable replacement is done using the
     * current index.
     *
     * @param pattern Path pattern.
     * @param values Path keys.
     * @return Path.
     */
    public static String reverse(String pattern, Object... values) {
        Map<String, Object> keys = new LinkedHashMap<>();
        IntStream.range(0, values.length).forEach(k -> keys.put(Integer.toString(k), values[k]));
        return reverse(pattern, keys);
    }

    /**
     * Recreate a path pattern using the given variables.
     *
     * @param pattern Path pattern.
     * @param keys Path keys.
     * @return Path.
     */
    public static String reverse(String pattern, Map<String, Object> keys) {
        StringBuilder path = new StringBuilder();
        List<String> filter = new ArrayList<>();
        int start = 0;
        int end = Integer.MAX_VALUE;
        int len = pattern.length();
        int keyIdx = 0;
        for (int i = 0; i < len; i++) {
            char ch = pattern.charAt(i);
            if (ch == '{') {
                path.append(pattern, start, i);
                start = i + 1;
                end = Integer.MAX_VALUE;
            } else if (ch == ':') {
                end = i;
            } else if (ch == '}') {
                String id = pattern.substring(start, Math.min(i, end));
                keyIdx = getKeyIdx(keys, path, filter, keyIdx, id);
                start = i + 1;
                end = Integer.MAX_VALUE;
            } else if (ch == '*') {
                path.append(pattern, start, i);
                String id;
                if (i == len - 1) {
                    id = "*";
                } else {
                    id = pattern.substring(i + 1);
                }
                keyIdx = getKeyIdx(keys, path, filter, keyIdx, id);
                start = len;
                i = len;
            }
        }
        if (path.length() == 0) {
            return pattern;
        }
        if (start > 0) {
            path.append(pattern, start, len);
        }

        if (keyIdx < keys.size()) {
            String query = keys.keySet().stream()
                    .filter(it -> !filter.contains(it))
                    .map(it -> String.format("%s=%s", it, keys.get(it)))
                    .collect(Collectors.joining("&"));
            return String.format("%s?%s", path.toString(), query);
        } else {
            return path.toString();
        }
    }

    private static int getKeyIdx(Map<String, Object> keys, StringBuilder path, List<String> filter, int keyIdx, String id) {
        Object value = keys.get(id);
        if (value == null) {
            String strKey = Integer.toString(keyIdx++);
            value = keys.get(strKey);
            filter.add(strKey);
        } else
            filter.add(id);
        requireNonNull(value, "Missing key: '" + id + "'");
        path.append(value);
        return keyIdx;
    }

    private RouteInfo active;
    private List<NamedRoute> routes;
    private String assetPath;
    private String distPath;

    private Router() {}

    public Router(RouteInfo routeInfo, List<NamedRoute> routes) {
        this.active = routeInfo;
        this.routes = routes;
    }

    public String query(String key) {
        return active.getQuery().get(key);
    }

    public String path(String key) {
        return active.getPathParams().get(key);
    }

    public Router withRouteInfo(RouteInfo routeInfo) {
        this.active = routeInfo;
        return this;
    }

    public Router withRoutes(List<NamedRoute> routes) {
        this.routes = routes;
        return this;
    }

    public Router withAssetPath(String assetPath) {
        this.assetPath = assetPath;
        return this;
    }

    public Router withDistPath(String distPath) {
        this.distPath = distPath;
        return this;
    }

    public NamedRoute at(String name) {
        return routes.stream()
                .filter(route -> route.getName() != null && route.getName().equals(name))
                .findFirst().orElse(null);
    }

    public NamedRoute current() {
        return at(this.active.getName());
    }

    public RouteInfo getActive() {
        return active;
    }

    public List<NamedRoute> getRoutes() {
        return routes;
    }

    public String getAssetPath() {
        return assetPath;
    }

    public String getDistPath() {
        return distPath;
    }

    @Override
    public String toString() {
        return active.toString();
    }
}
