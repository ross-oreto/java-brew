package io.oreto.brew.web.page;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.oreto.brew.obj.Reflect;
import io.oreto.brew.str.Str;
import io.oreto.brew.web.page.constants.C;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Form<T> implements Notifiable, Validatable<T> {

    public static <T> Form<T> of(Class<T> cls) {
        return new Form<T>(cls.getSimpleName()).withField(cls);
    }

    protected String name;
    protected List<Field> fields = new ArrayList<>();
    protected T data;
    protected @JsonIgnore List<Function<Form<T>, Notification>> validators = new ArrayList<>();
    protected boolean valid = true;
    public @JsonIgnore FormValidators<T> validation;
    protected List<Notification> notifications = new ArrayList<>();

    protected Form(String name) {
        this.name = Str.toCamel(name);
        validation = new FormValidators<T>(this);
    }

    public String getName() { return name; }

    public List<Field> fields() { return fields; }
    public List<Function<Form<T>, Notification>> validators() { return validators; }

    public T getData() {
        return data;
    }

    public Form<T> withData(T data) { this.data = data; return this; }

    public Form<T> withValidator(Function<Form<T>, Notification> validator) {
        this.validators.add(validator);
        return this;
    }

    public Form<T> withValidator(Supplier<Notification> validator) {
        this.validators.add((Form<T> f) -> validator.get());
        return this;
    }

    public Form<T> withValidator(String name, Function<T, Boolean> validator, String...args) {
        this.validators.add((Form<T> f) -> {
            if (validator.apply(f.data))
                return new Valid(name);
            else
                return f.validation.error(name, f.data, FormValidators.Invalid(name), args);
        });
        return this;
    }

    public Form<T> withValidator(Function<T, String> validator, String name, String...args) {
        this.validators.add((Form<T> f) -> {
            String code = validator.apply(f.data);
            if (Str.isEmpty(code))
                return new Valid(name);
            else
                return f.validation.error(name, f.data, code, args);
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    public Form<T> validate() {
        if (data instanceof Validatable) {
            validators.addAll(((Validatable<T>) data).validators());
        }
        for(Function<Form<T>, Notification> func : validators) {
            Notification result = func.apply(this);
            if (result instanceof Error) {
                withError((Error) result);
            } else {
                result.setType(Notification.Type.valid);
                notifications.add(result);
            }
        }
        return this;
    }

    public boolean submit() {
        return validate().isValid();
    }

    public boolean submit(Consumer<Form<T>> success) {
        if (validate().isValid()){
            success.accept(this);
            return true;
        }
        return false;
    }

    public boolean submit(Consumer<Form<T>> success, Consumer<Form<T>> failure) {
        if (validate().isValid()) {
            success.accept(this);
            return true;
        } else {
            failure.accept(this);
            return false;
        }
    }

    public boolean isValid() {
        return valid = errors().size() == 0;
    }

    public Form<T> withError(Error error) {
        notifications.add(error);
        return this;
    }

    public Form<T> withError(List<Error> errors) {
        notifications.addAll(errors);
        return this;
    }

    public List<Error> errors() {
        return notifications.stream()
                .filter(it -> it instanceof Error)
                .map(it -> (Error) it)
                .collect(Collectors.toList());
    }

    public List<Error> errors(String name) {
        return errors().stream()
                .filter(it -> it.name.equals(name))
                .collect(Collectors.toList());
    }

    public boolean isInvalid() {
        return !isValid();
    }

    public Form<T> withField(Class<T> cls) {
        for (java.lang.reflect.Field field : Reflect.getAllFields(cls)) {
            if (!field.getName().startsWith("_")) {
                Field formField = Field.of(field, this.name);
                fields.add(formField);
            }
        }
        return this;
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

    static public class Valid extends Notification {
        public Valid(String name) {
            super();
            this.name = name;
        }

        public Valid(String name, String message) {
            super(name, message, Type.success);
        }
    }

    static public class Error extends Notification {
        protected Object invalidValue;

        public Error(String name, String message) {
            super(name, message, Type.error);
        }

        public Error(String name, Object invalidValue, String message) {
            super(name, message, Type.error);
            this.invalidValue = invalidValue;
        }

        public Object getInvalidValue() {
            return invalidValue;
        }
        public void setInvalidValue(Object invalidValue) {
            this.invalidValue = invalidValue;
        }
    }
}
