package io.oreto.brew.web.page;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.oreto.brew.web.page.constants.C;

import java.util.*;
import java.util.stream.Collectors;

public class Notification {
    protected String name;
    protected String message;
    protected Type type;
    private boolean localized;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected String[] args;

    public Notification(String name, String message, Type type) {
        this.name = name;
        this.message = message;
        this.type = type;
    }

    public Notification(String name, String message) {
        this.name = name;
        this.message = message;
        this.type = Type.info;
    }

    public Notification(){ }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    public List<String> argList() {
        return args == null ? new ArrayList<>() : Arrays.stream(args).collect(Collectors.toList());
    }

    public Notification withName(String name) {
        this.name = name;
        return this;
    }

    public Notification withMessage(String message) {
        this.message = message;
        return this;
    }

    public Notification withType(Type type) {
        this.type = type;
        return this;
    }

    public Notification withArgs(String... args) {
        this.args = args;
        return this;
    }

    public Notification markLocalized() {
        this.localized = true;
        return this;
    }

    public String toString(Locale locale) {
        return Page.I18n(ResourceBundle.getBundle(C.messages, locale), message, (Object[]) args).orElse(message);
    }

    public Notification localize(Locale locale) {
        if (localized) return this;
        return withMessage(toString(locale)).markLocalized();
    }

    public Notification localize() {
      return localize(Locale.US);
    }

    @Override
    public String toString() {
        return message;
    }

    public enum Type {
        info, success, warning, error, tip, description, valid
    }
}
