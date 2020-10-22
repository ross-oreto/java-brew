package io.oreto.brew.web.rest;

import io.oreto.brew.web.http.StatusCode;

public class RestResponse<T> {

    public static <T> RestResponse<T> ok(T body) {
        return new RestResponse<T>(body);
    }
    public static <T> RestResponse<T> ok() {
        return new RestResponse<T>(null);
    }
    public static <T> RestResponse<T> created(T body) {
        return new RestResponse<T>(body).withStatus(StatusCode.CREATED_CODE);
    }
    public static <T> RestResponse<T> noContent() {
        return new RestResponse<T>(null).withStatus(StatusCode.NO_CONTENT_CODE);
    }

    public static <T> RestResponse<T> notFound() {
        return new RestResponse<T>(StatusCode.NOT_FOUND_CODE);
    }
    public static <T> RestResponse<T> unprocessable(T body) {
        return new RestResponse<>(body, StatusCode.UNPROCESSABLE_ENTITY_CODE);
    }
    public static <T> RestResponse<T> unprocessable() {
        return new RestResponse<T>(StatusCode.UNPROCESSABLE_ENTITY_CODE);
    }

    private int status;
    private final T body;
    private boolean ok;

    protected RestResponse(T body, int status) {
        withStatus(status);
        this.body = body;
    }

    protected RestResponse(T body) {
        this(body, StatusCode.OK.value());
    }

    protected RestResponse(int status) {
        this(null, status);
    }

    public RestResponse<T> withStatus(int status) {
        this.status = status;
        this.ok = status < 400;
        return this;
    }

    public int getStatus() {
        return status;
    }

    public T getBody() {
        return body;
    }

    public boolean isOk() {
        return ok;
    }
}
