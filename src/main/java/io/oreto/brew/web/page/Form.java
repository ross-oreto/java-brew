package io.oreto.brew.web.page;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.oreto.brew.collections.Lists;
import io.oreto.brew.data.validation.Validator;
import io.oreto.brew.obj.Reflect;
import io.oreto.brew.str.Str;
import io.oreto.brew.web.page.constants.C;

import javax.persistence.GeneratedValue;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Form<T> implements Notifiable, Validatable {

    public static <T> Form<T> of(Class<T> cls) {
        return new Form<T>(cls.getSimpleName()).withFields(cls);
    }

    public static <T> Form<T> of(String name) {
        return new Form<>(name);
    }

    protected String name;
    protected List<Field> fields = new ArrayList<>();
    protected T model;

    @JsonIgnore
    protected List<Validator<?>> validators = new ArrayList<>();

    protected boolean valid = true;
    protected List<Notification> notifications = new ArrayList<>();
    protected Locale locale;

    protected Form(String name) {
        this.name = Str.toCamel(name);
    }

    public String getName() { return name; }

    public List<Field> fields() { return fields; }
    public List<Validator<?>> validators() { return validators; }

    public T getModel() {
        return model;
    }

    public Form<T> withModel(T data) { this.model = data; return this; }
    public Form<T> withLocale(Locale locale) { this.locale = locale; return this; }

    public <V> Form<T> withValidator(Validator<V> validator) {
        this.validators.add(validator.group(name));
        return this;
    }

    public <V> Form<T> withValidator(Function<T, Validator<V>> validator) {
        this.validators.add(validator.apply(model).group(name));
        return this;
    }

    public boolean validate(Locale locale) {
        // check data object for validatable implementation
        if (model instanceof Validatable) {
            validators.addAll(((Validatable) model).validators());
        }

        // validate data object using annotations
        for(Validator.Invalid invalid : Validator.validate(model)) {
           withValidationError(invalid);
        }

        // finally run validators on the form
        for(Validator<?> validator : validators) {
            validator.validate().ifPresent(this::withValidationError);
        }
        if (Objects.nonNull(locale))
            localize(locale);

        return isValid();
    }

    public boolean validate() {
       return validate(this.locale);
    }

    public boolean isValid() {
        return valid = validationErrors().size() == 0;
    }

    public Form<T> withValidationError(Validator.Invalid error) {
        notifications.add(error);
        return this;
    }

    public Form<T> withValidationErrors(List<Validator.Invalid> errors) {
        notifications.addAll(errors);
        return this;
    }

    public List<Validator.Invalid> validationErrors() {
        return notifications.stream()
                .filter(it -> it instanceof Validator.Invalid)
                .map(it -> (Validator.Invalid) it)
                .collect(Collectors.toList());
    }

    public List<Validator.Invalid> validationErrors(String name) {
        return validationErrors().stream()
                .filter(it -> it.group.equals(name))
                .collect(Collectors.toList());
    }

    public boolean isInvalid() {
        return !isValid();
    }

    public static class Admit {
        public static Admit exclude(String... names) {
            return new Admit(false, names);
        }

        public static Admit include(String... names) {
            return new Admit(true, names);
        }

        protected Admit(boolean include, String... names) {
            this.include = include;
            this.names = names;
        }

        boolean include;
        String[] names;
    }

    public Form<T> withFields(Class<T> cls, Admit admit) {
        List<String> exclusions = Lists.of(admit.names);
        Iterable<java.lang.reflect.Field> allFields = admit.names.length == 0
                ? Reflect.getAllFields(cls)
                : Reflect.getAllFields(cls).stream()
                .filter(it -> admit.include == exclusions.contains(it.getName()))
                .collect(Collectors.toSet());

        for (java.lang.reflect.Field field : allFields) {
            if (Reflect.getSetter(field, cls).isPresent() && !field.isAnnotationPresent(GeneratedValue.class)) {
                Field formField = Field.of(field, this.name);
                if (field.getGenericType().getTypeName().contains("<")) {
                    String[] types = field.getGenericType().getTypeName().split("<");
                    if (types[1].contains(",")) {
                        String[] map = types[1].split(",");
                        String k = map[0].substring(map[0].lastIndexOf('.') + 1);
                        String v = map[1].substring(map[1].lastIndexOf('.') + 1);
                        formField.withDescription(String.format("%s<%s,%s"
                                , types[0].substring(types[0].lastIndexOf('.') + 1), k, v));
                    } else {
                        formField.withDescription(String.format("%s<%s"
                                , types[0].substring(types[0].lastIndexOf('.') + 1)
                                , types[1].substring(types[1].lastIndexOf('.') + 1)));
                    }
                } else {
                    String types = field.getGenericType().getTypeName();
                    formField.withDescription(types.substring(types.lastIndexOf('.') + 1));
                }
                fields.add(formField);
            }
        }
        return this;
    }

    public Form<T> withFields(Class<T> cls) {
        return withFields(cls, Admit.include());
    }

    public Form<T> withField(String name, String type) {
        fields.add(new Field().withName(formPreface(name), type));
        return this;
    }

    public Form<T> withField(String name) {
        fields.add(new Field().withName(formPreface(name)));
        return this;
    }

    public Form<T> withButtons(String...buttons) {
        for(String button : buttons)
            fields.add(new Field().withName(formPreface(button), C.button));
        return this;
    }

    public Field at(String name) {
        return fields.stream().filter(it -> it.getName().equals(name)).findFirst().orElse(null);
    }

    public Form<T> at(String name, Consumer<Field> fieldConsumer) {
        fieldConsumer.accept(at(name));
        return this;
    }

    public Form<T> withField(String name, Consumer<Field> fieldConsumer) {
        Field field = new Field().withName(formPreface(name));
        fields.add(field);
        fieldConsumer.accept(field);
        return this;
    }

    protected String formPreface(String code) {
        return String.format("%s.%s", this.name, code);
    }

    @Override
    public List<Notification> getNotifications() {
        return notifications;
    }

    @Override
    public Form<T> notify(String message, Notification.Type type, String group, String... args) {
        getNotifications().add(
                Notification.of(formPreface(message), type, group).withArgs(args)
        );
        return this;
    }

    public Form<T> notify(String message, Notification.Type type, String... args) {
        return notify(message, type, name, args);
    }

    public Form<T> notify(String message, String... args) {
        return notify(message, Notification.Type.info, name, args);
    }

    @Override
    public String toString() {
        return name;
    }

    public String toString(Locale locale) {
        return Page.I18n(ResourceBundle.getBundle(C.messages, locale), name).orElse(name);
    }

    public Form<T> localize(Locale locale) {
        fields.forEach(it -> it.localize(locale));
        notifications.forEach(it -> it.localize(locale));
        return this;
    }

    public Form<T> localize() {
        return localize(locale == null ? Locale.US : locale);
    }
}
