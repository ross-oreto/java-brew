package io.oreto.brew.data.validation;

import io.oreto.brew.num.Range;
import io.oreto.brew.obj.Reflect;
import io.oreto.brew.str.Str;
import io.oreto.brew.web.page.Notification;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Validator<T> {
    public static Optional<Invalid> Valid() {
        return Optional.empty();
    }
    public static Optional<Invalid> Invalid(String property, Object invalidValue, String message, String... args) {
       return Optional.of(Invalid.of(property, invalidValue, message, args));
    }
    public static Optional<Invalid> Invalid(String property, String message, String... args) {
        return Optional.of(Invalid.of(property, message, args));
    }

    private static Set<Invalid> validate(Object o, Field field) {
        Set<Invalid> errors = new HashSet<>();
        try {
            if (field.isAnnotationPresent(Required.class)) {
                Validator.of(Reflect.getFieldValue(o, field))
                        .message(Message.of(field.getAnnotation(Required.class)))
                        .required()
                        .validate().ifPresent(errors::add);
            }
            if (field.isAnnotationPresent(True.class)) {
                Validator.of(Reflect.getFieldValue(o, field))
                        .message(Message.of(field.getAnnotation(True.class)))
                        .isTrue()
                        .validate().ifPresent(errors::add);
            }
            if (field.isAnnotationPresent(False.class)) {
                Validator.of(Reflect.getFieldValue(o, field))
                        .message(Message.of(field.getAnnotation(False.class)))
                        .isFalse()
                        .validate().ifPresent(errors::add);
            }
            if (field.isAnnotationPresent(NotNull.class)) {
                Validator.of(Reflect.getFieldValue(o, field))
                        .message(Message.of(field.getAnnotation(NotNull.class)))
                        .notNull()
                        .validate().ifPresent(errors::add);
            }
            if (field.isAnnotationPresent(Size.class)) {
                Size size = field.getAnnotation(Size.class);
                Validator.of(Reflect.getFieldValue(o, field))
                        .message(Message.of(field.getAnnotation(Size.class)))
                        .size(size.value())
                        .validate().ifPresent(errors::add);
            }
            if (field.isAnnotationPresent(Max.class)) {
                Max max = field.getAnnotation(Max.class);
                Validator.of(Reflect.getFieldValue(o, field))
                        .message(Message.of(max))
                        .max(max.value())
                        .validate().ifPresent(errors::add);
            }
            if (field.isAnnotationPresent(Min.class)) {
                Min min = field.getAnnotation(Min.class);
                Validator.of(Reflect.getFieldValue(o, field))
                        .message(Message.of(min))
                        .min(min.value())
                        .validate().ifPresent(errors::add);
            }
            if (field.isAnnotationPresent(Between.class)) {
                Between between = field.getAnnotation(Between.class);
                Validator.of(Reflect.getFieldValue(o, field))
                        .message(Message.of(between))
                        .between(between.x(), between.y(), between.inclusive())
                        .validate().ifPresent(errors::add);
            }
            if (field.isAnnotationPresent(NotBetween.class)) {
                NotBetween between = field.getAnnotation(NotBetween.class);
                Validator.of(Reflect.getFieldValue(o, field))
                        .message(Message.of(between))
                        .notBetween(between.x(), between.y(), between.inclusive())
                        .validate().ifPresent(errors::add);
            }
            if (field.isAnnotationPresent(In.class)) {
                In in = field.getAnnotation(In.class);
                Validator.of(Reflect.getFieldValue(o, field)).message(Message.of(in)).in(in.set())
                        .validate().ifPresent(errors::add);
            }
            if (field.isAnnotationPresent(NotIn.class)) {
                NotIn in = field.getAnnotation(NotIn.class);
                Validator.of(Reflect.getFieldValue(o, field)).message(Message.of(in)).notIn(in.set())
                        .validate().ifPresent(errors::add);
            }
            if (field.isAnnotationPresent(Equal.class)) {
                Equal in = field.getAnnotation(Equal.class);
                Validator.of(Reflect.getFieldValue(o, field)).message(Message.of(in)).equal(in.value())
                        .validate().ifPresent(errors::add);
            }
            if (field.isAnnotationPresent(NotEqual.class)) {
                NotEqual in = field.getAnnotation(NotEqual.class);
                Validator.of(Reflect.getFieldValue(o, field)).message(Message.of(in)).notEqual(in.value())
                        .validate().ifPresent(errors::add);
            }
            if (field.isAnnotationPresent(Email.class)) {
                Validator.of(Reflect.getFieldValue(o, field))
                        .message(Message.of(field.getAnnotation(Email.class)))
                        .email()
                        .validate().ifPresent(errors::add);
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return errors;
    }

    public static Set<Invalid> validate(Object o, String field) {
        Optional<Field> optionalField = Reflect.getField(o, field);
        return optionalField.map(value -> validate(o, value)).orElseGet(HashSet::new);
    }

    public static Set<Invalid> validate(Object o) {
        Set<Invalid> errors = new HashSet<>();
        for (Field field : Reflect.getAllFields(o)) {
            errors.addAll(validate(o, field));
        }
        return errors;
    }

    public static <T> Validator<T> of(T data) {
        Validator<T> validator = new Validator<>();
        validator.data = data;
        return validator;
    }

    private T data;
    private Message message = Message.none();
    private Function<T, Boolean> validator;

    private Validator() {}

    public Validator<T> message(String message) {
        this.message.code = message;
        return this;
    }

    public Validator<T> message(Message message) {
        this.message = message;
        return this;
    }

    public Validator<T> named(String name) {
        this.message.name = name;
        return this;
    }

    public Validator<T> property(String property) {
        this.message.property = property;
        return this;
    }

    public Validator<T> group(String group) {
        this.message.group = group;
        return this;
    }

    public Optional<Invalid> validate() {
        return validator.apply(data)
                ? Validator.Valid()
                : Validator.Invalid(message.property, data, message.compose(), message.args);
    }

    public Validator<T> check(Function<T, Boolean> validator) {
        this.validator = validator;
        return this;
    }

    public Validator<T> required() {
        message.name = Message.REQUIRED;
        return check(data -> !(data == null || data.toString().trim().equals("") ||
                (data instanceof Collection && ((Collection<?>) data).isEmpty()) ||
                (data instanceof Iterable && !((Iterable<?>) data).iterator().hasNext())));
    }

    public Validator<T> notNull() {
       message.name = Message.NOT_NULL;
       return check(Objects::nonNull);
    }

    private double dataSize() {
        return data instanceof Number
                ? ((Number) data).doubleValue()
                : data == null ? 0D : (double) data.toString().length();
    }

    public Validator<T> size(Number size) {
        message.withArgs(size.toString()).name = data instanceof Number ? Message.SIZE: Message.SIZE_CHARS;
        return check(data -> dataSize() == size.doubleValue());
    }

    public Validator<T> max(Number max) {
        message.withArgs(max.toString()).name = data instanceof Number ? Message.MAX : Message.MAX_CHARS;
        return check(data -> dataSize() <= max.doubleValue());
    }

    public Validator<T> min(Number min) {
        message.withArgs(min.toString()).name = data instanceof Number ? Message.MIN : Message.MIN_CHARS;
        return check(data -> dataSize() >= min.doubleValue());
    }

    public Validator<T> between(Number x, Number y, boolean inclusive) {
        message.withArgs(x.toString(), y.toString()).name = Message.BETWEEN;
        double size = dataSize();
        return check(data -> Range.numberIn(size, x, y, inclusive));
    }

    public Validator<T> notBetween(Number x, Number y, boolean inclusive) {
        message.withArgs(x.toString(), y.toString()).name = Message.NOT_BETWEEN;
        double size = dataSize();
        return check(data -> !Range.numberIn(size, x, y, inclusive));
    }

    public Validator<T> in(Object[] set) {
        List<Object> objects = Arrays.stream(set).collect(Collectors.toList());
        message.withArgs(objects.stream().map(Object::toString).toArray(String[]::new)).name = Message.IN;
        return check(objects::contains);
    }

    public Validator<T> notIn(Object[] set) {
        List<Object> objects = Arrays.stream(set).collect(Collectors.toList());
        message.withArgs(objects.stream().map(Object::toString).toArray(String[]::new)).name = Message.NOT_IN;
        return check(data -> !objects.contains(data));
    }

    public Validator<T> equal(Object o) {
        message.withArgs(o.toString()).name = Message.EQUAL;
        return check(o::equals);
    }

    public Validator<T> isTrue() {
        message.name = Message.TRUE;
        return check(data -> {
            if (data instanceof Boolean) {
                return (Boolean) data;
            } else if (data instanceof Number) {
                return ((Number) data).intValue() != 0;
            } else if (data instanceof String) {
                return data.toString().trim().equalsIgnoreCase("true");
            } else {
                return Objects.nonNull(data);
            }
        });
    }

    public Validator<T> isFalse() {
        message.name = Message.FALSE;
        return check(data -> {
            if (data instanceof Boolean) {
                return !(Boolean) data;
            } else if (data instanceof Number) {
                return ((Number) data).intValue() == 0;
            } else if (data instanceof String) {
                return data.toString().trim().equalsIgnoreCase("false");
            } else {
                return data == null;
            }
        });
    }

    public Validator<T> notEqual(Object o) {
        message.withArgs(o.toString()).name = Message.NOT_EQUAL;
        return check(data -> !o.equals(data));
    }

    public Validator<T> email() {
        message.withArgs(data.toString()).name = Message.EMAIL;
        return check(data -> Objects.nonNull(data) && data.toString().matches(Message.emailPattern));
    }

    static public class Message {
        public static final String INVALID = "invalid";
        public static final String REQUIRED = "required";
        public static final String TRUE = "true";
        public static final String FALSE = "false";
        public static final String NOT_NULL = "not.null";
        public static final String EMAIL = "email";
        public static final String SIZE = "size";
        public static final String SIZE_CHARS = "size.chars";
        public static final String MAX = "max";
        public static final String MAX_CHARS = "max.chars";
        public static final String MIN = "min";
        public static final String MIN_CHARS = "min.chars";
        public static final String BETWEEN = "between";
        public static final String NOT_BETWEEN = "between";
        public static final String EQUAL = "equal";
        public static final String NOT_EQUAL = "not.equal";
        public static final String IN = "in";
        public static final String NOT_IN = "not.in";
        public static final String emailPattern = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";

        public static Message none() {
            return new Message();
        }

        public static Message of(String code) {
           Message message = new Message();
           message.code = code;
           return message;
        }

        public static Message of(Annotation annotation) throws ReflectiveOperationException {
            Message message = new Message();
            message.code = (String) Reflect.getAttributeValue(annotation, "message");
            message.name = (String) Reflect.getAttributeValue(annotation, "name");
            message.property = (String) Reflect.getAttributeValue(annotation, "property");
            message.group =  (String) Reflect.getAttributeValue(annotation, "group");
            return message;
        }

        public static Message of(String group, String property, String name) {
            Message message = new Message();
            message.group = group;
            message.property = property;
            message.name = name;
            return message;
        }

        public static Message of(String group, String property) {
           return Message.of(group, property, Message.INVALID);
        }

        private String name;
        private String property;
        private String group;
        private String code;
        private String[] args;

        protected Message() {}

        public String getName() {
            return name;
        }

        public String getProperty() {
            return property;
        }

        public String getCode() {
            return code;
        }

        public String getGroup() {
            return group;
        }

        public String[] getArgs() {
            return args;
        }

        public Message withArgs(String... args) {
            this.args = args;
            return this;
        }

        public String compose() {
            return Str.isEmpty(code)
                    ? Arrays.stream(new String[]{"validation", group, property, name == null ? Message.INVALID : name})
                    .filter(Str::isNotEmpty).collect(Collectors.joining("."))
                    : code;
        }
    }

    static public class Invalid extends Notification {
        protected Object invalidValue;

        public static Invalid of(String property, String message, String... args) {
           return new Invalid(property, message).withArgs(args);
        }
        public static Invalid of(String property, Object invalidValue, String message, String... args) {
            return new Invalid(property, invalidValue, message).withArgs(args);
        }

        protected Invalid(String property, String message) {
            super(message, Type.invalid, property);
        }
        protected Invalid(String property, Object invalidValue, String message) {
            super(message, Type.invalid, property);
            this.invalidValue = invalidValue;
        }

        public Object getInvalidValue() {
            return invalidValue;
        }
        public void setInvalidValue(Object invalidValue) {
            this.invalidValue = invalidValue;
        }

        @Override
        public Invalid markLocalized() {
            super.markLocalized();
            return this;
        }

        @Override
        public Invalid withArgs(String... args) {
            super.withArgs(args);
            return this;
        }
    }
}
