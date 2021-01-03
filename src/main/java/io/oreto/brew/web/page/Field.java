package io.oreto.brew.web.page;

import io.oreto.brew.data.validation.NotNull;
import io.oreto.brew.data.validation.Required;
import io.oreto.brew.web.page.constants.C;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class Field {

    private static final Map<Class<?>, String> typeMap = new HashMap<Class<?>, String>() {{
        put(String.class, C.text);
        put(Date.class, C.date);
        put(java.sql.Date.class, C.date);
        put(Timestamp.class, C.time);
        put(LocalDate.class, C.DATETIME_LOCAL_KEBAB);
        put(LocalDateTime.class, C.DATETIME_LOCAL_KEBAB);
        put(byte[].class, C.image);
        put(Number.class, C.number);
        put(Long.class, C.number);
        put(Integer.class, C.number);
        put(Double.class, C.number);
        put(Float.class, C.number);
        put(Short.class, C.number);
        put(Byte.class, C.number);
        put(int.class, C.number);
        put(long.class, C.number);
        put(double.class, C.number);
        put(float.class, C.number);
        put(short.class, C.number);
        put(byte.class, C.number);
    }};

    public static Field of(java.lang.reflect.Field field, String formName) {
        Field newField = new Field()
                .withName(String.format("%s.%s", formName, field.getName()), field.getType());

        if (Arrays.stream(field.getAnnotations())
                .anyMatch(it -> it.annotationType() == NotNull.class
                        || it.annotationType() == Required.class
                        )) {
            newField.required();
        }
        return newField;
    }

    // END STATIC -------------------------------------------------------------------------------------------------

    private String name;
    private String property;
    private String description;
    private Map<String, String> attributes = new HashMap<>();
    private boolean localized;

    @Override
    public String toString() {
        return name;
    }

    public String toString(Locale locale) {
        return Page.I18n(ResourceBundle.getBundle(C.messages, locale), name).orElse(name);
    }

    public Field localize(Locale locale) {
        if (localized) return this;
        withDescription(toString(locale));
        localized = true;
        return this;
    }

    public Field localize() {
        return localize(Locale.US);
    }

    public String getName() {
        return name;
    }

    public String getProperty() { return property; }

    public String getDescription() {
        return description;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public Field withName(String name) {
        this.property = name;
        this.name = name.contains(".") && name.charAt(name.length() - 1) != '.'
                ? name.substring(name.lastIndexOf(".") + 1)
                : name;
        return withAttr(C.type, C.text);
    }

    public Field withName(String name, Class<?> cls) {
        return withName(name).withAttr(C.type, typeMap.getOrDefault(cls, C.text));
    }

    public Field withName(String name, String type) {
        return withName(name).withAttr(C.type, type);
    }

    public Field withDescription(String description) { this.description = description; return this; }

    public String at(String name) {
        return attributes.get(name);
    }

    public Field withAttr(String name, String value) {
        attributes.put(name, value);
        return this;
    }

    public Field required() { return withAttr(C.required, C.required); }
    public Field disabled() { return withAttr(C.disabled, C.disabled); }
}
