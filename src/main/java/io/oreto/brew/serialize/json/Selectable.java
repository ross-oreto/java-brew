package io.oreto.brew.serialize.json;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Selectable {
    @JsonIgnore default String view() { return null; }
    @JsonIgnore default String select() { return null; }
    @JsonIgnore default String drop() { return null; }
}
