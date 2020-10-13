package io.oreto.brew;

import java.util.Comparator;

public class Range<T> implements Comparable<T> {

    public static <T extends Comparable<T>> boolean in(T element, T from, T to, boolean inclusive) {
        int l = element.compareTo(from);
        int r = element.compareTo(to);
        return inclusive
                ? l >= 0 && r <= 0
                : l >= 0 && r < 0;
    }

    public static <T extends Comparable<T>> boolean in(T element, T from, T to) {
        return in(element, from, to, false);
    }

    public static <T> boolean in(T element, T from, T to, Comparator<T> comp, boolean inclusive) {
        int l = comp.compare(element, from);
        int r = comp.compare(element, to);
        return inclusive
                ? l >= 0 && r <= 0
                : l >= 0 && r < 0;
    }

    public static <T> boolean in(T element, T from, T to, Comparator<T> comp) {
        return in(element, from, to, comp, false);
    }

    public static <T> Range<T> of(T from, T to) {
        return new Range<>(from, to);
    }

    public static <T> Range<T> of(T from, T to, Comparator<T> comparator) {
        return new Range<>(from, to, comparator);
    }

    public static <T> Range<T> of() {
        return new Range<>();
    }

    @SuppressWarnings("unchecked")
    private Comparator<T> comparator = (o1, o2) -> ((Comparable<T>)o1).compareTo(o2);

    private T from;
    private T to;
    protected boolean inclusive;

    protected Range() { }

    protected Range(T from, T to) {
        this.from = from;
        this.to = to;
    }

    protected Range(T from, T to, Comparator<T> comparator) {
        this(from, to);
        this.comparator = comparator;
    }

    public T from(){ return from; }
    public Range<T> from(T from){ this.from = from; return this; }

    public T to(){ return to; }
    public Range<T> to(T to){ this.to = to; this.inclusive = true; return this; }
    public Range<T> until(T to){ this.to = to; this.inclusive = false; return this; }

    public boolean in(T element, Comparator<T> comparator) {
        return in(element, from, to, comparator, inclusive);
    }

    public boolean in(T element) {
        return in(element, from, to, comparator, inclusive);
    }

    public boolean isEmpty() {
        return from == null && to == null;
    }

    public boolean isPresent() {
        return !isEmpty();
    }

    @Override
    public int compareTo(T o) {
        if (comparator.compare(o, from) < 0)
            return -1;
        else if (inclusive ? comparator.compare(o, to) >= 0 : comparator.compare(o, to) > 0)
            return 1;
        else
            return 0;
    }
}
