package io.oreto.brew.web.route;

import io.oreto.brew.web.http.HttpContext;
import io.oreto.brew.web.http.HttpVerb;

import java.util.Map;

public class RouteInfo {
    private String name;
    private HttpVerb verb;
    private String path;
    private Map<String, String> pathParams;
    private Map<String, String> query;

    private RouteInfo(){ }

    public RouteInfo(String name
            , String verb
            , String path
            , Map<String, String> pathParams
            , Map<String, String> query) {
        this.name = name;
        this.verb = HttpVerb.valueOf(verb);
        this.path = path;
        this.pathParams = pathParams;
        this.query = query;
    }

    public RouteInfo(String name
            , String verb
            , String path
            , Map<String, String> pathParams
            , String query) {
        this(name
                , verb
                , path
                , pathParams
                , HttpContext.queryStringToMap(query) );
    }

    public RouteInfo(String verb
            , String path
            , Map<String, String> pathParams
            , String query) {
        this(NamedRoute.of(path, verb).getName(), verb, path, pathParams, query);
    }

    public RouteInfo(String verb
            , String path
            , Map<String, String> pathParams
            , Map<String, String> query) {
        this(NamedRoute.of(path, verb).getName(), verb, path, pathParams, query);
    }

    @Override
    public String toString() {
        return path;
    }

    public String getName() {
        return name;
    }
    public HttpVerb getVerb() {
        return verb;
    }
    public String getPath() {
        return path;
    }
    public Map<String, String> getPathParams() {
        return pathParams;
    }
    public Map<String, String> getQuery() {
        return query;
    }
}
