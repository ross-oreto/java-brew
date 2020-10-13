package io.oreto.brew.web.http;

import io.oreto.brew.str.Str;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public interface HttpContext {

    enum SCHEME {
        http, https
    }

    static Map<String, String> queryStringToMap(String query) {
        return Arrays.stream(query.split("&"))
                .map(it -> it.split("="))
                .collect(Collectors.toMap(it -> it[0], it -> it.length > 1 ? it[1] : ""));
    }

    static Optional<String> getAuthorizationToken(Map<String, String> headers) {
        String authorization = headers == null ? null : headers.get("Authorization");
        if(Objects.nonNull(authorization)) {
            Str token = Str.of(authorization);
            return Optional.of(token.slice(token.indexOf(' ').orElse(0)).trim().toString());
        }
        return Optional.empty();
    }

    static boolean isAjax(Map<String, String> headers) {
        return headers.containsKey("X-Requested-With")
                && headers.get("X-Requested-With").equals("XMLHttpRequest");
    }

    String verb();  // GET, POST, etc..

    // scheme://host:port/path?query
    // example: https://oreto.com:443/test/ing?foo=bar&this=that
    String scheme();       // http,https
    String host();         // oreto.com
    int port();            // 443
    default String hostWithPort() { return String.format("%s:%d", host(), port()); } // oreto.com:443
    String requestUrl();   // full URL https://oreto.com:443/test/ing?foo=bar&this=that
    String requestPath();  // /test/ing
    String uri();          // no scheme: oreto.com/test/ing?foo=bar&this=that
    String queryString();  // ?foo=bar&this=that

    /**
     * Returns the value of the specified parameter from the HTTP request path + URL params, or empty if not found.
     */
    default Optional<String> getParam(String name) {
        Optional<String> path = getPath(name);
        return path.isPresent() ? path : getQuery(name);
    }

    /**
     * Returns the value of the specified URL path parameter from the HTTP request, or empty if not found.
     */
    Optional<String> getPath(String name);

    /**
     * Returns the value of the specified URL query parameter from the HTTP request, or empty if not found.
     */
    Optional<String> getQuery(String name);

    /**
     * Returns the value of the specified <b>cookie</b> from the HTTP request, , or empty if not found.
     */
    Optional<String> getCookie(String name);

    /**
     * Returns the value of the specified <b>header</b> from the HTTP request, or empty if not found.
     */
    Optional<String> getHeader(String name);

    /**
     * Get an attribute by a key.
     *
     * @param key Attribute key.
     * @param <T> Attribute type.
     * @return Attribute value if key exists, empty otherwise.
     */
    <T> Optional<T> getAttribute(String key);

    Map<String, String> params();
    Map<String, String> pathParams();
    Map<String, String> queryParams();
    Map<String, Object> attributes();

    <T> T getUser();

    HttpContext withResponseType(MediaType mediaType);
    HttpContext withResponseCode(int code);
}
