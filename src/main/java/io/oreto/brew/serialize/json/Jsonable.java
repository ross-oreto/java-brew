package io.oreto.brew.serialize.json;

public interface Jsonable {
    default JsonRenderer json(String name, boolean pretty) {
        return new JsonRenderer(name, pretty);
    }
    default JsonRenderer json(boolean pretty) {
        return new JsonRenderer(pretty);
    }
    default JsonRenderer json(String name) {
        return json(name, false);
    }
    default JsonRenderer json() {
        return new JsonRenderer();
    }
}
