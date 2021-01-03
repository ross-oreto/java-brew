package io.oreto.brew.data;


import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Sort implements Sortable {
    public enum Direction {
        asc, desc
    }

    public static Sortable of(String s) {
        Sort sort = new Sort();
        String[] sorts = s.split(",");
        sort.name = sorts[0];
        sort.direction = sorts.length > 1 && sorts[1].toLowerCase().equals(Direction.desc.name())
                ? Direction.desc
                : Direction.asc;

        return sort;
    }

    public static List<Sortable> of(List<String> s) {
        return s.stream().filter(Objects::nonNull).map(Sort::of).collect(Collectors.toList());
    }

    public static Sortable of(String name, String direction) {
        Sort sort = new Sort();
        sort.name = name;
        sort.direction = Direction.valueOf(direction.toLowerCase());

        return sort;
    }

    private String name;
    private Direction direction = Direction.asc;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Direction getDirection() {
        return direction;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }
}
