package io.oreto.brew.data;

public interface Sortable {
    String getName();
    Sort.Direction getDirection();

    default boolean isAscending() {
        return getDirection() == Sort.Direction.asc;
    }
    default boolean isDescending() {
        return getDirection() == Sort.Direction.desc;
    }
}
