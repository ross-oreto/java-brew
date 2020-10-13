package io.oreto.brew.web.rest;

import io.oreto.brew.web.http.StatusCode;

public class RestResponse {

    public static RestResponse of(int statusCode, Object data) {
        return new RestResponse(statusCode, data);
    }

    public static RestResponse of(Object data) {
        return new RestResponse(data);
    }

    private int statusCode;
    private Object data;

    protected RestResponse(int statusCode, Object data) {
        this.statusCode = statusCode;
        this.data = data;
    }

    protected RestResponse(Object data) {
        this(StatusCode.OK.value(), data);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Object getData() {
        return data;
    }
}
