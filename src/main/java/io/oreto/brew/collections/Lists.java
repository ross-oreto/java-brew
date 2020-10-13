package io.oreto.brew.collections;

import io.oreto.brew.obj.Reflect;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Lists {

    public static final long DEFAULT_SKIP = 0;
    public static final long DEFAULT_LIMIT = 20;

    public static <T> List<Indexed<T>> withIndex(Iterable<T> collection) {
        List<Indexed<T>> l = new ArrayList<>();
        int i = 0;
        for (T t : collection) { l.add(Indexed.index(i, t)); i++; }
        return l;
    }

    @SafeVarargs
    public static <T> List<T> of(T... t) {
        return Arrays.stream(t).collect(Collectors.toList());
    }

    @SafeVarargs
    public static <T> Set<T> set(T... t) {
        return Arrays.stream(t).collect(Collectors.toSet());
    }

    @SafeVarargs
    public static <T> T[] array(T... t) {
        return t;
    }

    public static <T> List<T> page(Stream<T> stream, long skip, long limit) {
        return stream
                .skip(skip)
                .limit(limit)
                .collect(Collectors.toList());
    }

    public static <T> List<T> page(Stream<T> stream) {
        return page(stream, DEFAULT_SKIP, DEFAULT_LIMIT);
    }

    public static <T> List<T> page(Stream<T> stream, long skip) {
        return page(stream, skip, DEFAULT_LIMIT);
    }

    public static <T> List<T> page(Iterable<T> iterable, long skip, long limit) {
        return page(StreamSupport.stream(iterable.spliterator(), false), skip, limit);
    }

    public static <T> List<T> page(Iterable<T> iterable) {
        return page(iterable, DEFAULT_SKIP, DEFAULT_LIMIT);
    }

    public static <T> List<T> page(Iterable<T> iterable, long skip) {
        return page(iterable, skip, DEFAULT_LIMIT);
    }

    public static <T> List<T> page(Collection<T> collection, Collection<String> order, long skip, long limit) {
        return page(orderStream(collection, order), skip, limit);
    }

    public static <T> List<T> page(Collection<T> collection, Collection<String> order, long skip) {
        return page(orderStream(collection, order), skip);
    }

    public static <T> List<T> page(Collection<T> collection, Collection<String> order) {
        return page(orderStream(collection, order));
    }

    public static <T> List<T> orderBy(Collection<T> collection, Collection<String> order) {
        return orderStream(collection, order).collect(Collectors.toList());
    }

    public static <T> Stream<T> orderStream(Collection<T> collection, Collection<String> order) {
        if (collection == null)
            return Stream.empty();
        if (order == null || order.isEmpty() || collection.isEmpty())
            return new ArrayList<>(collection).stream();

        return collection.stream()
                .sorted(orderToComparator(collection.iterator().next(), order));
    }

    private static <T> Comparator<T> orderToComparator(T o, Collection<String> order) {
        return order.stream()
                .map(it -> orderToComparator(o, it))
                .reduce(Comparator::thenComparing)
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private static <T> Comparator<T> orderToComparator(T o, String order) {
        String[] sort = order.split(",");
        String field = sort[0];
        boolean descending = sort.length > 1 && sort[1].trim().equalsIgnoreCase("desc");
        return new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                try {
                    Comparable<Object> v1 =
                            (Comparable<Object>) Reflect.getFieldValue(o1, Reflect.getField(o1.getClass(), field));
                    Comparable<Object> v2 =
                            (Comparable<Object>) Reflect.getFieldValue(o2, Reflect.getField(o2.getClass(), field));
                    return descending ? v2.compareTo(v1) : v1.compareTo(v2);
                } catch (Exception ignored) {
                    return 0;
                }
            }
            @Override
            public boolean equals(Object obj) {
                return o.getClass() == obj.getClass() && o.equals(obj);
            }
        };
    }
}
