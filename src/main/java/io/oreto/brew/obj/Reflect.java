package io.oreto.brew.obj;

import io.oreto.brew.str.Str;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    public static Field getField(Class<?> aClass, String field) {
        return getAllFields(aClass).stream().filter(it -> it.getName().equals(field)).findFirst().orElse(null);
    }

    public static Object getFieldValue(Object o, Field field)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Object value;
        if (Modifier.isPublic(field.getModifiers())) {
            value = field.get(o);
        } else if (Arrays.stream(o.getClass().getMethods())
                .anyMatch(method -> method.getName().equals(field.getName())
                        && method.getParameterCount() == 0)) {
            value = o.getClass().getMethod(field.getName()).invoke(o);
        } else {
            value = o.getClass().getMethod(String.format("get%s"
                    , Str.capitalize(field.getName())))
                    .invoke(o);
        }
        return value;
    }
}
