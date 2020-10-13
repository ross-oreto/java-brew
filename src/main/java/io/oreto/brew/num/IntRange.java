package io.oreto.brew.num;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class IntRange extends Range<Integer> {

    public static IntRange of(Integer from, Integer to) {
        return new IntRange().from(from).to(to);
    }
    public static IntRange From(Integer from) {
        return new IntRange().from(from);
    }
    public static IntRange To(Integer to) {
        return new IntRange().from(0).to(to);
    }
    public static IntRange Until(Integer to) {
        return new IntRange().from(0).until(to);
    }

    @Override
    protected Integer defaultStep() { return 1; }

    @Override
    public <T> List<T> map(Function<Integer, T> mapper) {
        List<T> l = new ArrayList<>();
        int step = by();
        int len = inclusive ? to() + step : to();
        for (int i = from(); i < len; i+=step) {
            l.add(mapper.apply(i));
        }
        return l;
    }

    @Override
    public void each(Consumer<Integer> consumer) {
        int step = by();
        int len = inclusive ? to() + step : to();
        for (int i = from(); i < len; i+=step) {
            consumer.accept(i);
        }
    }

    @Override
    public IntRange from(Integer to){ super.from(to); return this; }

    @Override
    public IntRange to(Integer to){ super.to(to); return this; }

    @Override
    public IntRange until(Integer to){ super.until(to); return this; }

    @Override
    public boolean in(Integer element) {
        return super.in(element) && (element - from()) % by() == 0;
    }

    public boolean allIn(Integer... element) {
        return Arrays.stream(element).allMatch(this::in);
    }
    public boolean anyIn(Integer... element) {
        return Arrays.stream(element).anyMatch(this::in);
    }
    public boolean noneIn(Integer... element) {
        return !anyIn(element);
    }
}
