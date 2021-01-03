package io.oreto.brew;

public class Bean {

//    public static Validator DEFAULT_VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
//    private static final Map<Locale, Validator> validators = new HashMap<>();
//
//    public static Validator getValidator(Locale locale, String... bundles) {
//        if (locale == null)
//            return DEFAULT_VALIDATOR;
//
//        Validator validator = validators.get(locale);
//        if (validator == null){
//           validator = Validation.byProvider(HibernateValidator.class).configure()
//                   .messageInterpolator(
//                           new ResourceBundleMessageInterpolator(
//                                   new AggregateResourceBundleLocator(Arrays.asList(bundles))
//                           )
//                   )
//                   .defaultLocale(locale)
//                   .buildValidatorFactory()
//                   .getValidator();
//           validators.put(locale, validator);
//        }
//        return validator;
//    }
//
//    public static Validator getValidator(Locale locale) {
//        return getValidator(locale, "messages", "ValidationMessages");
//    }
//
//    public static <T> Set<ConstraintViolation<T>> validate(Locale locale, T t, Class<?>... groups) {
//        return getValidator(locale).validate(t, groups);
//    }
//
//    public static <T> Set<ConstraintViolation<T>> validate(Locale locale, T t, String propertyName, Class<?>... groups) {
//        return getValidator(locale).validateProperty(t, propertyName, groups);
//    }
//
//    public static <T> Set<ConstraintViolation<T>> validate(Locale locale
//            , Class<T> t
//            , String propertyName
//            , Object value
//            , Class<?>... groups) {
//        return getValidator(locale).validateValue(t, propertyName, value, groups);
//    }
}
