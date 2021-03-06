package io.oreto.brew.web.rest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.oreto.brew.data.validation.Validator;
import io.oreto.brew.obj.Safe;
import io.oreto.brew.serialize.json.Selectable;
import io.oreto.brew.web.http.StatusCode;
import io.oreto.brew.web.page.Notifiable;
import io.oreto.brew.web.page.Notification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RestResponse<T> implements Notifiable, Selectable {
    public static <T> RestResponse<T> of(int status, String reason) {
        return new RestResponse<T>(status).withReason(reason);
    }
    public static <T> RestResponse<T> of(T body, int status, String reason) {
        return new RestResponse<T>(body, status).withReason(reason);
    }

    public static <T> RestResponse<T> ok(T body) {
        return new RestResponse<T>(body);
    }
    public static <T> RestResponse<T> ok() {
        return new RestResponse<T>(StatusCode.OK);
    }

    public static <T> RestResponse<T> created(T body) {
        return new RestResponse<T>(body).withStatus(StatusCode.CREATED);
    }
    public static <T> RestResponse<T> noContent() {
        return new RestResponse<T>(StatusCode.NO_CONTENT);
    }

    public static <T> RestResponse<T> notFound() {
        return new RestResponse<T>(StatusCode.NOT_FOUND);
    }
    public static <T> RestResponse<T> unprocessable(T body) {
        return new RestResponse<>(body, StatusCode.UNPROCESSABLE_ENTITY);
    }
    public static <T> RestResponse<T> unprocessable() {
        return new RestResponse<T>(StatusCode.UNPROCESSABLE_ENTITY);
    }

    public static <T> RestResponse<T> bad(T body) {
        return new RestResponse<T>(body, StatusCode.BAD_REQUEST);
    }
    public static <T> RestResponse<T> bad() {
        return new RestResponse<T>(StatusCode.BAD_REQUEST);
    }

    public static <T> RestResponse<T> unauthorized() {
        return new RestResponse<T>(StatusCode.UNAUTHORIZED);
    }
    public static <T> RestResponse<T> forbidden() {
        return new RestResponse<T>(StatusCode.FORBIDDEN);
    }

    public static <T> RestResponse<T> error() {
        return new RestResponse<T>(StatusCode.SERVER_ERROR);
    }
    public static <T> RestResponse<T> error(T body) {
        return new RestResponse<T>(body, StatusCode.SERVER_ERROR);
    }

    private int status;
    private String reason;
    private final T body;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<Notification> notifications = new ArrayList<>();
    private boolean ok;

    @JsonIgnore private String _view;
    @JsonIgnore private String _select;
    @JsonIgnore private String _drop;

    @Override
    public String view() {
        return _view;
    }
    public RestResponse<T> view(String _view) {
        this._view = _view;
        return this;
    }

    @Override
    public String select() {
        return _select;
    }
    public RestResponse<T> select(String _select) {
        this._select = _select;
        return this;
    }

    public RestResponse<T> select(Map<String, Object> params) {
        return select(Safe.of(params.get("_select")).q(Object::toString).orElse(""))
                .view(Safe.of(params.get("_view")).q(Object::toString).orElse(""))
                .drop(Safe.of(params.get("_drop")).q(Object::toString).orElse(""));
    }

    @Override
    public String drop() {
        return _drop;
    }
    public RestResponse<T> drop(String _drop) {
        this._drop = _drop;
        return this;
    }

    protected RestResponse(T body, int status) {
        withStatus(status);
        this.body = body;
    }

    protected RestResponse(T body, StatusCode status) {
        withStatus(status);
        this.body = body;
    }

    protected RestResponse(T body) {
        this(body, StatusCode.OK);
    }

    protected RestResponse(int status) {
        this(null, status);
    }

    protected RestResponse(StatusCode status) {
        this(null, status);
    }

    public RestResponse<T> withStatus(int status) {
        this.status = status;
        this.ok = status < 400;
        return this;
    }

    public RestResponse<T> withStatus(StatusCode statusCode) {
        withStatus(statusCode.value());
        withReason(statusCode.reason());
        return this;
    }

    public RestResponse<T> withReason(String reason) {
        this.reason = reason;
        return this;
    }

    public RestResponse<T> withError(Throwable throwable) {
        notify(throwable.getMessage(), Notification.Type.error);
        return this;
    }

    public int getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public T getBody() {
        return body;
    }

    public boolean isOk() {
        return ok;
    }

    public List<Notification> getNotifications() {
        return notifications;
    }

    @Override
    public RestResponse<T> notify(String message, Notification.Type type, String group, String... args) {
        Notifiable.super.notify(message, type, group, args);
        return this;
    }

    @Override
    public RestResponse<T> notify(String message, Notification.Type type) {
        Notifiable.super.notify(message, type);
        return this;
    }

    @Override
    public RestResponse<T> notify(String message, String... args) {
        Notifiable.super.notify(message, args);
        return this;
    }

    @Override
    public RestResponse<T> notify(Notification notification) {
        Notifiable.super.notify(notification);
        return this;
    }

    @Override
    public RestResponse<T> notify(List<Notification> notifications) {
        Notifiable.super.notify(notifications);
        return this;
    }

    @Override
    public RestResponse<T> notify(Iterable<Validator.Invalid> notifications) {
        Notifiable.super.notify(notifications);
        return this;
    }

    public RestResponse<T> localize(Locale locale) {
        getNotifications().forEach(it -> it.localize(locale));
        return this;
    }

    public RestResponse<T> localize() {
        return localize(Locale.getDefault());
    }
}
