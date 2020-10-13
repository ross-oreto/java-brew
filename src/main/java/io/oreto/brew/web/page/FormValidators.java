package io.oreto.brew.web.page;

import io.oreto.brew.num.Range;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class FormValidators<T> {
    static final String INVALID = "invalid";
    static final String REQUIRED = "required";
    static final String LENGTH = "length";
    static final String MAX = "max";
    static final String MAX_CHARS = "max.chars";
    static final String MIN = "min";
    static final String MIN_CHARS = "min.chars";
    static final String BETWEEN = "between";
    static final String EQUAL = "equal";
    static final String NOT_EQUAL = "not.equal";
    static final String IN = "in";

    static final String emailPattern = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";

    static String composeCode(String name, String code) {
        return String.format("%s.%s", name, code);
    }

    static String Invalid(String name) { return composeCode(name, INVALID); }
    static String Required(String name) {  return composeCode(name, REQUIRED); }
    static String Length(String name) {  return composeCode(name, LENGTH); }
    static String Max(String name) {  return composeCode(name, MAX); }
    static String MaxChars(String name) {  return composeCode(name, MAX_CHARS); }
    static String Min(String name) {  return composeCode(name, MIN); }
    static String MinChars(String name) {  return composeCode(name, MIN_CHARS); }
    static String Between(String name) {  return composeCode(name, BETWEEN); }
    static String Equal(String name) {  return composeCode(name, EQUAL); }
    static String NotEqual(String name) {  return composeCode(name, NOT_EQUAL); }
    static String In(String name) {  return composeCode(name, IN); }

    private Form<T> form;

    FormValidators(Form<T> form) {
        this.form = form;
    }

    public FormValidators<T> not() {
        return this;
    }

    public Notification error(String name, Object o, String code, String... args) {
        return new Form.Error(name, o, formPreface(code)).withArgs(args);
    }

    public Notification error(String name, Object o) {
        return error(name, o, Invalid(name));
    }

    public Notification valid(String name) {
        return new Form.Valid(name);
    }

    public Notification required(String name, Object o) {
        if (o == null || o.toString().equals("") || (o instanceof Collection && ((Collection<?>) o).isEmpty())) {
            return new Form.Error(name, o, Required(formPreface(name)));
        }
        return valid(name);
    }

    public Notification equal(String name, Object o1, Object o2) {
        if (o1.equals(o2)) {
            return valid(name);
        }
        return new Form.Error(name, o1, Equal(formPreface(name))).withArgs(o2.toString());
    }

    public Notification in(String name, Object o1, Object[] o2) {
        List<Object> objects = Arrays.stream(o2).collect(Collectors.toList());
        if (objects.contains(o1)) {
            return valid(name);
        }
        return new Form.Error(name, o1, In(formPreface(name)))
                .withArgs(objects.stream().map(Object::toString).toArray(String[]::new));
    }

    public Notification notEqual(String name, Object o1, Object o2) {
        if (!o1.equals(o2)) {
            return valid(name);
        }
        return new Form.Error(name, o1, NotEqual(formPreface(name))).withArgs(o2.toString());
    }

    public Notification length(String name, String s, Number n) {
        if (s.length() == n.intValue()) {
            return valid(name);
        }
        return new Form.Error(name, s, Length(formPreface(name))).withArgs(n.toString());
    }

    public Notification max(String name, String s, Number n) {
        if (s.length() > n.intValue()) {
            return new Form.Error(name, s, MaxChars(formPreface(name))).withArgs(n.toString());
        }
        return valid(name);
    }

    public Notification max(String name, Number n, Number max) {
        return Range.numberIn(n, Integer.MIN_VALUE, max, true)
                ? valid(name)
                : new Form.Error(name, n, Max(formPreface(name))).withArgs(n.toString());
    }

    public Notification min(String name, String s, Number n) {
        if (s.length() < n.intValue()) {
            return new Form.Error(name, s, MinChars(formPreface(name))).withArgs(n.toString());
        }
        return valid(name);
    }

    public Notification min(String name, Number n, Number min) {
        return Range.numberIn(n, min, Integer.MAX_VALUE, true)
                ? valid(name)
                : new Form.Error(name, n, Min(formPreface(name))).withArgs(n.toString());
    }

    public Notification between(String name, Number n, Number x, Number y, boolean inclusive) {
        if (Range.numberIn(n, x, y, inclusive)) {
            return valid(name);
        }
        return new Form.Error(name, n, Between(formPreface(name))).withArgs(x.toString(), y.toString());
    }

    public Notification notBetween(String name, Number n, Number x, Number y, boolean inclusive) {
        if (!Range.numberIn(n, x, y, inclusive)) {
            return valid(name);
        }
        return new Form.Error(name, n, Between(formPreface(name))).withArgs(x.toString(), y.toString());
    }

    public Notification between(String name, Number n, Number x, Number y) {
        return between(name, n, x, y, true);
    }

    public Notification notBetween(String name, Number n, Number x, Number y) {
        return notBetween(name, n, x, y, true);
    }

    public Notification email(String name, String email) {
        if (email.matches(emailPattern)) {
            return valid(name);
        }
        return error(name, email).withArgs(email);
    }

    public Notification notEmail(String name, String email) {
        if (!email.matches(emailPattern)) {
            return valid(name);
        }
        return error(name, email).withArgs(email);
    }

    protected String formPreface(String code) {
        return String.format("validation.%s.%s", form.getName(), code);
    }
}
