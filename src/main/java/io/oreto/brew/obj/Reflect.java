package io.oreto.brew.obj;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.oreto.brew.collections.Lists;
import io.oreto.brew.str.Noun;
import io.oreto.brew.str.Str;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Reflect {

    public static class Allow {
        public static Allow none() {
            return new Allow();
        }

        public static Allow all() {
            return none()
                    .allowTransient()
                    .allowStatic()
                    .allowFinal()
                    .allowJsonIgnore()
                    .allowUnderscore();
        }

        boolean trans;
        boolean _static;
        boolean _final = true;
        boolean underscore;
        boolean jsonIgnore = true;

        public Allow allowTransient() {
            trans = true;
            return this;
        }

        public Allow allowStatic() {
            _static = true;
            return this;
        }

        public Allow allowFinal() {
            _final = true;
            return this;
        }

        public Allow allowJsonIgnore() {
            jsonIgnore = true;
            return this;
        }

        public Allow allowUnderscore() {
            underscore = true;
            return this;
        }

        public Allow ignoreTransient() {
            trans = false;
            return this;
        }

        public Allow ignoreStatic() {
            _static = false;
            return this;
        }

        public Allow ignoreFinal() {
            _final = false;
            return this;
        }

        public Allow ignoreJsonIgnore() {
            jsonIgnore = false;
            return this;
        }

        public Allow ignoreUnderscore() {
            underscore = false;
            return this;
        }
    }

    @SuppressWarnings("unchecked")
    private static void parameterNames(Map<String, Object> paramMap, String path, List<String> names) {
        for (String key : paramMap.keySet()) {
            Object val = paramMap.get(key);
            if (val instanceof List) {
                List<?> items = (List<?>) val;
                if (items.size() > 0 && items.get(0) instanceof Map) {
                    parameterNames((Map<String, Object>) items.get(0)
                            , Str.isEmpty(path) ? key : String.format("%s.%s", path, key)
                            , names);
                } else {
                    names.add(Str.isEmpty(path) ? key : String.format("%s.%s", path, key));
                }
            } else if (val instanceof Map) {
                parameterNames((Map<String, Object>) val
                        , Str.isEmpty(path) ? key : String.format("%s.%s", path, key)
                        , names);
            } else {
                names.add(Str.isEmpty(path) ? key : String.format("%s.%s", path, key));
            }
        }
    }

    public static List<String> parameterNames(Map<String, Object> paramMap) {
        List<String> names = new ArrayList<>();
        parameterNames(paramMap, "", names);
        return names.stream().distinct().collect(Collectors.toList());
    }

    private static List<Field> getAllFields(Class<?> aClass, List<Field> fields, Allow allow) {
        fields.addAll(
                Arrays.stream(aClass.getDeclaredFields())
                        .filter(it -> allow.trans || !Modifier.isTransient(it.getModifiers()))
                        .filter(it -> allow._static || !Modifier.isStatic(it.getModifiers()))
                        .filter(it -> allow._final || !Modifier.isFinal(it.getModifiers()))
                        .filter(it -> allow.jsonIgnore || !it.isAnnotationPresent(JsonIgnore.class))
                        .filter(it -> allow.underscore || !it.getName().startsWith("_"))
                        .collect(Collectors.toList())
        );

        if (aClass.getSuperclass() != null) {
            getAllFields(aClass.getSuperclass(), fields, allow);
        }

        return fields;
    }

    private static List<Field> getAllFields(Class<?> aClass, List<Field> fields) {
        return getAllFields(aClass, fields, Allow.none());
    }

    public static List<Field> getAllFields(Class<?> aClass, Allow ignore) {
        return getAllFields(aClass, new ArrayList<>(), ignore);
    }
    public static List<Field> getAllFields(Class<?> aClass) {
        return getAllFields(aClass, new ArrayList<>());
    }

    public static List<Field> getAllFields(Object o, Allow ignore) {
        return getAllFields(o.getClass(), new ArrayList<>(), ignore);
    }
    public static List<Field> getAllFields(Object o) {
        return getAllFields(o.getClass(), new ArrayList<>());
    }

    public static Optional<Field> getField(Class<?> aClass, String field) {
        return getAllFields(aClass).stream().filter(it -> it.getName().equals(field)).findFirst();
    }
    public static Optional<Field> getField(Object o, String field) {
        return o == null ? Optional.empty() : getField(o.getClass(), field);
    }

    public static boolean isFieldPublic(Object o, String name) {
        Optional<Field> field = getField(o, name);
        return field.isPresent() && isFieldPublic(field.get());
    }

    public static boolean isFieldPublic(Field field) {
        return Modifier.isPublic(field.getModifiers());
    }

    public static Object getFieldValue(Object o, Field field)
            throws ReflectiveOperationException {
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

    public static Optional<Method> getGetter(Field field, Class<?> cls) {
        for (Method method : cls.getMethods()) {
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

    public static Optional<Method> getGetter(Field field, Object o) {
        return getGetter(field, o.getClass());
    }

    public static Optional<Method> getGetter(String name, Class<?> cls) {
        return getField(cls, name).flatMap(value -> getGetter(value, cls));
    }
    public static Optional<Method> getGetter(String name, Object o) {
        return getField(o, name).flatMap(value -> getGetter(value, o));
    }

    public static Object getFieldValue(Object o, String field)
            throws ReflectiveOperationException {
        return getFieldValue(o, getField(o, field).orElseThrow(() -> new NoSuchFieldException("no such field " + field)));
    }

    public static Object getAttributeValue(Annotation annotation, String attribute)
            throws ReflectiveOperationException {
        Method method = Arrays.stream(annotation.annotationType().getMethods())
                .filter(it -> it.getName().equals(attribute)).findFirst().orElseThrow(() ->
                        new NoSuchElementException("no such attribute " + attribute));
        return  method.invoke(annotation);
    }

    public static Optional<Method> getSetter(Field field, Class<?> cls) {
        for (Method method : cls.getMethods()) {
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

    public static Optional<Method> getSetter(Field field, Object o) {
        return getSetter(field, o.getClass());
    }

    public static Optional<Method> getSetter(String name, Object o) {
        return getField(o, name).flatMap(value -> getSetter(value, o));
    }

    public static Optional<Method> getSetter(String name, Class<?> cls) {
        return getField(cls, name).flatMap(value -> getSetter(value, cls));
    }

    private static Optional<Method> getAddRemoveMethod(String name, Field field, Class<?> cls) {
        ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
        Type[] genericTypes = parameterizedType.getActualTypeArguments();

        for (Method method : cls.getMethods()) {
            Class<?> param = method.getParameterCount() > 0 ? method.getParameterTypes()[0] : null;
            if (Objects.nonNull(param)) {
                String methodName = Str.of(Noun.singular(field.getName())).capitalize().preface(name).toString();
                if (method.getName().equals(methodName)) {
                    if (methodTypeMatches(param.isArray() && method.isVarArgs()
                            ? param.getComponentType() : param, (Class<?>) genericTypes[0])) {
                        return Optional.of(method);
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<Method> getAdder(Field field, Class<?> cls) {
        return getAddRemoveMethod("add", field, cls);
    }

    public static Optional<Method> getAdder(Field field, Object o) {
        return getAdder(field, o.getClass());
    }

    public static Optional<Method> getAdder(String name, Object o) {
        return getField(o, name).flatMap(value -> getAdder(value, o));
    }

    public static Optional<Method> getAdder(String name, Class<?> cls) {
        return getField(cls, name).flatMap(value -> getAdder(value, cls));
    }

    private static Object newArray(Object value, Class<?> type) {
        Object varargs = Array.newInstance(type, 1);
        Array.set(varargs, 0, value);
        return varargs;
    }

    public static void addFieldValue(Object o, Field field, Object value) throws ReflectiveOperationException {
        Optional<Method> method = getAdder(field, o);
        if (method.isPresent()) {
            if (method.get().isVarArgs()) {
                method.get().invoke(o, newArray(value, method.get().getParameterTypes()[0].getComponentType()));
            } else {
                method.get().invoke(o, value);
            }
            return;
        }
        throw new NoSuchMethodException(String.format("No add method found for %s", field.getName()));
    }

    public static void addFieldValue(Object o, String field, Object value)
            throws ReflectiveOperationException {
        addFieldValue(o, getField(o, field).orElseThrow(() -> new NoSuchFieldException("no such field " + field)), value);
    }

    public static Optional<Method> getRemover(Field field, Class<?> cls) {
        return getAddRemoveMethod("remove", field, cls);
    }

    public static Optional<Method> getRemover(Field field, Object o) {
        return getRemover(field, o.getClass());
    }

    public static Optional<Method> getRemover(String name, Object o) {
        return getField(o, name).flatMap(value -> getRemover(value, o));
    }

    public static Optional<Method> getRemover(String name, Class<?> cls) {
        return getField(cls, name).flatMap(value -> getRemover(value, cls));
    }

    public static void removeFieldValue(Object o, Field field, Object value) throws ReflectiveOperationException {
        Optional<Method> method = getRemover(field, o);
        if (method.isPresent()) {
            if (method.get().isVarArgs()) {
                method.get().invoke(o, newArray(value, method.get().getParameterTypes()[0].getComponentType()));
            } else {
                method.get().invoke(o, value);
            }
            return;
        }
        throw new NoSuchMethodException(String.format("No remove method found for %s", field.getName()));
    }

    public static void removeFieldValue(Object o, String field, Object value)
            throws ReflectiveOperationException {
        removeFieldValue(o, getField(o, field).orElseThrow(() -> new NoSuchFieldException("no such field " + field)), value);
    }

    public static void setFieldValue(Object o, Field field, Object value)
            throws ReflectiveOperationException {

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
            throws ReflectiveOperationException {
        setFieldValue(o, getField(o, field).orElseThrow(() -> new NoSuchFieldException("no such field " + field)), value);
    }

    public static void copy(Object o1, Object o2, Iterable<String> names, CopyOptions copyOptions)
            throws ReflectiveOperationException {
        copy(o1, o2, names, copyOptions, new HashMap<>());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void copy(Object o1
            , Object o2
            , Iterable<String> names
            , CopyOptions copyOptions
            , Map<Object, Object> visited)
            throws ReflectiveOperationException {
        if (o1 == null || o2 == null || visited.containsKey(o2))
            return;
        else
            visited.put(o2, o2);

        Set<String> nameSet = names == null
                ? new HashSet<>()
                : StreamSupport.stream(names.spliterator(), false)
                .map(it -> Lists.subLists(Arrays.asList(it.split("\\."))))
                .flatMap(it -> it.stream().map(sublist -> String.join(".", sublist)))
                .collect(Collectors.toSet());

        Iterable<Field> iterable = Objects.nonNull(names) && names.iterator().hasNext() ? getAllFields(o1, copyOptions.ignore).stream()
                .filter(it -> isFieldPublic(it) || (getSetter(it, o1).isPresent() && getGetter(it, o1).isPresent()))
                    .filter(it -> copyOptions.exclusion != nameSet.contains(it.getName()))
                    .collect(Collectors.toSet())
                : getAllFields(o1).stream()
                    .filter(it -> isFieldPublic(it) || (getSetter(it, o1).isPresent() && getGetter(it, o1).isPresent()))
                    .collect(Collectors.toSet());

        for (Field field : iterable) {
            Object v1 = getFieldValue(o1, field);
            Object v2 = getFieldValue(o2, field);
            if (!copyOptions.nullsOnly || Obj.notInitialized(v1)) {
                if (copyOptions.mergeCollections && Collection.class.isAssignableFrom(field.getType())) {
                    Collection l1 = (Collection) v1;
                    Collection l2 = (Collection) v2;
                    if (Objects.nonNull(l1) && Objects.nonNull(l2)) {
                        boolean hasAdder = Reflect.getAdder(field, o1).isPresent();
                        boolean hasRemover = Reflect.getRemover(field, o1).isPresent();
                        for(Object it : l2) {
                            if(l1.contains(it) && !isPrimitive(field.getType())) {
                                String childPath = String.format("%s.", field.getName());
                                Iterable<String> children = nameSet.stream()
                                        .filter(name -> name.startsWith(childPath))
                                        .map(name -> name.replaceFirst(childPath, ""))
                                        .collect(Collectors.toSet());
                                Reflect.copy(l1.stream().filter(o -> o.equals(it)).findFirst().orElse(null)
                                        , it
                                        , children.iterator().hasNext() ? children : null
                                        , copyOptions
                                        , visited);
                            } else {
                                if(hasAdder)
                                    Reflect.addFieldValue(o1, field, it);
                                else
                                    l1.add(it);
                            }
                        }
                        if (copyOptions.updateCollections) {
                            Object[] removals = l1.stream().filter(it -> !l2.contains(it)).toArray();
                            for (Object removal : removals) {
                                if(hasRemover) Reflect.removeFieldValue(o1, field, removal);
                                else l1.remove(removal);
                            }
                        }
                    }
                } else if (copyOptions.mergeCollections && Map.class.isAssignableFrom(field.getType())) {
                    Map m1 = (Map) v1;
                    Map m2 = (Map) v2;
                    if (Objects.nonNull(m1) && Objects.nonNull(m2))
                        m1.putAll(m2);
                    if (copyOptions.updateCollections) {
                        Object[] keys = m1.keySet().stream().filter(it -> !m2.containsKey(it)).toArray();
                        for (Object key : keys)
                            m1.remove(key);
                    }
                } else {
                    if (isPrimitive(field.getType())) {
                        if (!Objects.equals(v1, v2)) {
                            setFieldValue(o1, field, v2);
                        }
                    } else {
                        String childPath = String.format("%s.", field.getName());
                        Iterable<String> children = nameSet.stream()
                                .filter(name -> name.startsWith(childPath))
                                .map(name -> name.replaceFirst(childPath, ""))
                                .collect(Collectors.toSet());
                        Reflect.copy(v1
                                , v2
                                , children.iterator().hasNext() ? children : null
                                , copyOptions
                                , visited);
                    }
                }
            }
        }
    }

    public static boolean isPrimitive(Class<?> type) {
        return type.isPrimitive()
                || CharSequence.class.isAssignableFrom(type)
                || Date.class.isAssignableFrom(type)
                || Temporal.class.isAssignableFrom(type);
    }

    public static void copy(Object o1, Object o2, Iterable<String> names)
            throws ReflectiveOperationException {
        copy(o1, o2, names, CopyOptions.create());
    }

    public static void copy(Object o1, Object o2, CopyOptions copyOptions)
            throws ReflectiveOperationException {
        copy(o1, o2, Lists.EMPTY_STRING_LIST, copyOptions);
    }

    public static void copy(Object o1, Object o2)
            throws ReflectiveOperationException {
        copy(o1, o2, Lists.EMPTY_STRING_LIST, CopyOptions.create());
    }

    public static void copy(Object o1, Object o2, String... names)
            throws ReflectiveOperationException {
        copy(o1, o2, Arrays.asList(names), CopyOptions.create());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void copy(Object o1, Map<String, Object> values, CopyOptions copyOptions)
            throws ReflectiveOperationException {

        Iterable<Field> iterable = getAllFields(o1, copyOptions.ignore).stream()
                .filter(it -> isFieldPublic(it) || (getSetter(it, o1).isPresent() && getGetter(it, o1).isPresent()))
                .filter(it -> copyOptions.exclusion != values.containsKey(it.getName()))
                .collect(Collectors.toSet());

        for (Field field : iterable) {
            Object v1 = getFieldValue(o1, field);
            Object v2 = values.get(field.getName());
            if (!copyOptions.nullsOnly || Obj.notInitialized(v1)) {
                if (copyOptions.mergeCollections && Collection.class.isAssignableFrom(field.getType())) {
                    boolean hasAdder = Reflect.getAdder(field, o1).isPresent();
                    boolean hasRemover = Reflect.getRemover(field, o1).isPresent();
                    Collection l1 = (Collection) v1;
                    Collection l2 = (Collection) v2;
                    for(Object it : l2) {
                        if(!l1.contains(it)) {
                            if(hasAdder)
                                Reflect.addFieldValue(o1, field, it);
                            else
                                l1.add(it);
                        }
                    }
                    if (copyOptions.updateCollections) {
                        Object[] removals = l1.stream().filter(it -> !l2.contains(it)).toArray();
                        for (Object removal : removals) {
                            if(hasRemover) Reflect.removeFieldValue(o1, field, removal);
                            else l1.remove(removal);
                        }
                    }
                } else if (copyOptions.mergeCollections && Map.class.isAssignableFrom(field.getType())) {
                    Map m1 = (Map) v1;
                    Map m2 = (Map) v2;
                    m1.putAll(m2);
                    if (copyOptions.updateCollections) {
                        Object[] keys = m1.keySet().stream().filter(it -> !m2.containsKey(it)).toArray();
                        for (Object key : keys)
                            m1.remove(key);
                    }
                } else {
                    if (!Objects.equals(v1, v2))
                        setFieldValue(o1, field, v2);
                }
            }
        }
    }

    public static void copy(Object o1, Map<String, Object> values)
            throws ReflectiveOperationException {
        copy(o1, values, CopyOptions.create());
    }

    public static class CopyOptions {
        public static CopyOptions create() {
            return new CopyOptions();
        }

        private boolean nullsOnly;
        private boolean mergeCollections;
        private boolean updateCollections;
        private boolean exclusion;
        private Allow ignore;

        protected CopyOptions(){
            ignore = Allow.none();
        }

        public CopyOptions nullsOnly() {
            nullsOnly = true;
            return this;
        }

        public CopyOptions mergeCollections() {
            mergeCollections = true;
            return this;
        }

        public CopyOptions updateCollections() {
            updateCollections = true;
            return mergeCollections();
        }

        public CopyOptions exclusion() {
            exclusion = true;
            return this;
        }

        public CopyOptions ignoring(Allow ignore) {
            this.ignore = ignore;
            return this;
        }
    }
}
