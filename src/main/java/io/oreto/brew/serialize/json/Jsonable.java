package io.oreto.brew.serialize.json;

public interface Jsonable {
    JsonRenderer json();

    default String renderJson(Object t, String view, String select, String drop) {
        return json().render(t, view, select, drop);
    }
}