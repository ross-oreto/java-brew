package io.oreto.brew.obj;

import io.oreto.brew.collections.Lists;
import io.oreto.brew.str.Str;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Reflect {
    private static List<Field> getAllFields(Class<?> aClass, List<Field> fields) {
        fields.addAll(
                Arrays.stream(aClass.getDeclaredFields())
                        .filter(it -> !Modifier.isTransient(it.getModifiers())
                                && !Modifier.isStatic(it.getModifiers())
                                && !Modifier.isFinal(it.getModifiers())
                                && !it.getName().startsWith("_"))
                        .collect(Collectors.toList())
        );

        if (aClass.getSuperclass() != null) {
            getAllFields(aClass.getSuperclass(), fields);
        }

        return fields;
    }

    public static List<Field> getAllFields(Class<?> aClass) {
        return getAllFields(aClass, new ArrayList<>());
    }
    public static List<Field> getAllFields(Object o) {
        return getAllFields(o.getClass(), new ArrayList<>());
    }

    public static Optional<Field> getField(Class<?> aClass, String field) {
        return getAllFields(aClass).stream().filter(it -> it.getName().equals(field)).findFirst();
    }
    public static Optional<Field> getField(Object o, String field) {
        return getField(o.getClass(), field);
    }

    public static boolean isFieldPublic(Object o, String name) {
        Optional<Field> field = getField(o, name);
        return field.isPresent() && isFieldPublic(field.get());
    }

    public static boolean isFieldPublic(Field field) {
        return Modifier.isPublic(field.getModifiers());
    }

    public static Object getFieldValue(Object o, Field field)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Object value;
        if (isFieldPublic(field)) {
            value = field.get(o);
        } else {
            Optional<Method> getter = getGetter(field, o);
            value = getter.orElseThrow(() ->
                    new NoSuchMethodException(String.format("No getters found for %s", field.getName())))
                    .invoke(o);
        }
        return value;
    }

    public static boolean methodTypeMatches(Class<?> method, Class<?> cls){
        return method == cls || method.isAssignableFrom(cls)
                || (method.isPrimitive() &&
                cls.getSimpleName().toLowerCase().startsWith(method.getSimpleName()));
    }

    public static Optional<Method> getGetter(Field field, Object o) {
        for (Method method : o.getClass().getMethods()) {
            if (method.getParameterCount() == 0 && methodTypeMatches(method.getReturnType(), field.getType())) {
                String fieldName = Str.capitalize(field.getName());
                if (method.getName().equals(String.format("get%s", fieldName))
                        || method.getName().equals(String.format("is%s", fieldName))
                        || method.getName().equals(field.getName()))
                    return Optional.of(method);
            }
        }
        return Optional.empty();
    }

    public static Optional<Method> getGetter(String name, Object o) {
        return getField(o, name).flatMap(value -> getGetter(value, o));
    }

    public static Object getFieldValue(Object o, String field)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, NoSuchFieldException {
        return getFieldValue(o, getField(o, field).orElseThrow(() -> new NoSuchFieldException("no such field " + field)));
    }

    public static Optional<Method> getSetter(Field field, Object o) {
        for (Method method : o.getClass().getMethods()) {
            Class<?> param = method.getParameterCount() == 1 ? method.getParameterTypes()[0] : null;

            if (Objects.nonNull(param)) {
                String capitalize = Str.capitalize(field.getName());
                String setter = String.format("set%s", capitalize);
                String with = String.format("with%s", capitalize);
                if (method.getName().equals(field.getName())
                        || method.getName().equals(setter)
                        || method.getName().equals(with)) {
                    if (methodTypeMatches(param, field.getType())) {
                        return Optional.of(method);
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<Method> getSetter(String name, Object o) {
        return getField(o, name).flatMap(value -> getSetter(value, o));
    }

    public static void setFieldValue(Object o, Field field, Object value)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        if (isFieldPublic(field))
            field.set(o, value);

        Optional<Method> method = getSetter(field, o);
        if (method.isPresent()) {
            method.get().invoke(o, value);
            return;
        }
        throw new NoSuchMethodException(String.format("No setters found for %s", field.getName()));
    }

    public static void setFieldValue(Object o, String field, Object value)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, NoSuchFieldException {
        setFieldValue(o, getField(o, field).orElseThrow(() -> new NoSuchFieldException("no such field " + field)), value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void copy(Object o1, Object o2, Iterable<String> names, CopyOptions copyOptions)
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Set<String> nameSet = StreamSupport.stream(names.spliterator(), false).collect(Collectors.toSet());

        Iterable<Field> iterable = names.iterator().hasNext() ? getAllFields(o1).stream()
                .filter(it -> isFieldPublic(it) || (getSetter(it, o1).isPresent() && getGetter(it, o1).isPresent()))
                    .filter(it -> copyOptions.exclusion != nameSet.contains(it.getName()))
                    .collect(Collectors.toSet())
                : getAllFields(o1).stream()
                    .filter(it -> isFieldPublic(it) || (getSetter(it, o1).isPresent() && getGetter(it, o1).isPresent()))
                    .collect(Collectors.toSet());

        for (Field field : iterable) {
            if (!copyOptions.nullsOnly || Obj.notInitialized(getFieldValue(o1, field))) {
                if (copyOptions.mergeCollections && List.class.isAssignableFrom(field.getType())) {
                    List l1 = (List) getFieldValue(o1, field);
                    l1.addAll((List) getFieldValue(o2, field));
                } else if (copyOptions.mergeCollections && Set.class.isAssignableFrom(field.getType())) {
                    Set s1 = (Set) getFieldValue(o1, field);
                    s1.addAll((Set) getFieldValue(o2, field));
                } else if (copyOptions.mergeCollections && Map.class.isAssignableFrom(field.getType())) {
                    Map m1 = (Map) getFieldValue(o1, field);
                    m1.putAll((Map) getFieldValue(o2, field));
                } else
                    setFieldValue(o1, field, getFieldValue(o2, field));
            }
        }
    }

    public static void copy(Object o1, Object o2, Iterable<String> names)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        copy(o1, o2, names, CopyOptions.create());
    }

    public static void copy(Object o1, Object o2, CopyOptions copyOptions)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        copy(o1, o2, Lists.EMPTY_STRING_LIST, copyOptions);
    }

    public static void copy(Object o1, Object o2)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        copy(o1, o2, Lists.EMPTY_STRING_LIST, CopyOptions.create());
    }

    public static void copy(Object o1, Object o2, String... names)
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        copy(o1, o2, Arrays.asList(names), CopyOptions.create());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void copy(Object o1, Map<String, Object> values, CopyOptions copyOptions)
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {

        Iterable<Field> iterable = getAllFields(o1).stream()
                .filter(it -> isFieldPublic(it) || (getSetter(it, o1).isPresent() && getGetter(it, o1).isPresent()))
                .filter(it -> copyOptions.exclusion != values.containsKey(it.getName()))
                .collect(Collectors.toSet());

        for (Field field : iterable) {
            if (!copyOptions.nullsOnly || Obj.notInitialized(getFieldValue(o1, field))) {
                if (copyOptions.mergeCollections && List.class.isAssignableFrom(field.getType())) {
                    List l1 = (List) getFieldValue(o1, field);
                    l1.addAll((List) values.get(field.getName()));
                } else if (copyOptions.mergeCollections && Set.class.isAssignableFrom(field.getType())) {
                    Set s1 = (Set) getFieldValue(o1, field);
                    s1.addAll((Set) values.get(field.getName()));
                } else if (copyOptions.mergeCollections && Map.class.isAssignableFrom(field.getType())) {
                    Map m1 = (Map) getFieldValue(o1, field);
                    m1.putAll((Map) values.get(field.getName()));
                } else
                    setFieldValue(o1, field, values.get(field.getName()));
            }
        }
    }

    public static void copy(Object o1, Map<String, Object> values)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        copy(o1, values, CopyOptions.create());
    }

    public static class CopyOptions {
        public static CopyOptions create() {
            return new CopyOptions();
        }

        private boolean nullsOnly;
        private boolean mergeCollections;
        private boolean exclusion;

        protected CopyOptions(){}

        public CopyOptions nullsOnly() {
            nullsOnly = true;
            return this;
        }

        public CopyOptions mergeCollections() {
            mergeCollections = true;
            return this;
        }

        public CopyOptions exclusion() {
            exclusion = true;
            return this;
        }
    }
}
