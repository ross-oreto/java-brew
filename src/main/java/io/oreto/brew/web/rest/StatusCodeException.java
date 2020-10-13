package io.oreto.brew.web.rest;

import io.oreto.brew.web.http.StatusCode;

/**
 * Runtime exception with status code.
 */
public class StatusCodeException extends RuntimeException {

    private final StatusCode statusCode;

    /**
     * Creates an error with the given status code.
     *
     * @param statusCode Status code.
     */
    public StatusCodeException(StatusCode statusCode) {
        this(statusCode, statusCode.toString());
    }

    /**
     * Creates an error with the given status code.
     *
     * @param statusCode Status code.
     * @param message Error message.
     */
    public StatusCodeException(StatusCode statusCode, String message) {
        this(statusCode, message, null);
    }

    /**
     * Creates an error with the given status code.
     *
     * @param statusCode Status code.
     * @param message Error message.
     * @param cause Cause.
     */
    public StatusCodeException(StatusCode statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /**
     * Status code.
     *
     * @return Status code.
     */
    public StatusCode getStatusCode() {
        return statusCode;
    }
}
