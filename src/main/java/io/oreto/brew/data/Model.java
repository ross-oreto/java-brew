package io.oreto.brew.data;

import io.oreto.brew.obj.Reflect;

import java.lang.reflect.InvocationTargetException;

public interface Model<ID> {
    default String idName() {
        return "id";
    }

    @SuppressWarnings("unchecked")
    default ID getId() {
        try {
            return (ID) Reflect.getFieldValue(this, idName());
        } catch (Exception e) {
            return null;
        }
    }

    default void setId(ID id) {
        try {
            Reflect.setFieldValue(this, idName(), id);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}
