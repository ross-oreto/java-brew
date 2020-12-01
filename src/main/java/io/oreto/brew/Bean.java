package io.oreto.brew;

import org.hibernate.validator.HibernateValidator;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Bean {

    public static Validator DEFAULT_VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final Map<Locale, Validator> validators = new HashMap<>();

    public static Validator getValidator(Locale locale) {
        if (locale == null)
            return DEFAULT_VALIDATOR;

        Validator validator = validators.get(locale);
        if (validator == null){
           validator = Validation.byProvider(HibernateValidator.class).configure()
                   .defaultLocale(locale)
                   .buildValidatorFactory()
                   .getValidator();
           validators.put(locale, validator);
        }
        return validator;
    }

    public static <T> Set<ConstraintViolation<T>> validate(Locale locale, T t, Class<?>... groups) {
        return getValidator(locale).validate(t, groups);
    }

    public static <T> Set<ConstraintViolation<T>> validate(Locale locale, T t, String propertyName, Class<?>... groups) {
        return getValidator(locale).validateProperty(t, propertyName, groups);
    }

    public static <T> Set<ConstraintViolation<T>> validate(Locale locale
            , Class<T> t
            , String propertyName
            , Object value
            , Class<?>... groups) {
        return getValidator(locale).validateValue(t, propertyName, value, groups);
    }
}
