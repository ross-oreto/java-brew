package io.oreto.brew.data;


import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Sort {
    public enum Direction {
        asc, desc
    }

    public static Sort of(String s) {
        Sort sort = new Sort();
        String[] sorts = s.split(",");
        sort.name = sorts[0];
        sort.direction = sorts.length > 1 && sorts[1].equals(Direction.desc.name()) ? Direction.desc : Direction.asc;

        return sort;
    }

    public static List<Sort> of(List<String> s) {
        return s.stream().filter(Objects::nonNull).map(Sort::of).collect(Collectors.toList());
    }

    public static Sort of(String name, String direction) {
        Sort sort = new Sort();
        sort.name = name;
        sort.direction = Direction.valueOf(direction);

        return sort;
    }

    private String name;
    private Direction direction = Direction.asc;

    public String getName() {
        return name;
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean isAscending() {
        return direction == Direction.asc;
    }

    public boolean isDescending() {
        return direction == Direction.desc;
    }
}
