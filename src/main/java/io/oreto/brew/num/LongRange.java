package io.oreto.brew.num;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class LongRange extends Range<Long> {
    public static LongRange of(Long from, Long to) {
        return new LongRange().from(from).to(to);
    }
    public static LongRange From(Long from) {
        return new LongRange().from(from);
    }
    public static LongRange To(Long to) {
        return new LongRange().from(0L).to(to);
    }
    public static LongRange Until(Long to) {
        return new LongRange().from(0L).until(to);
    }

    @Override
    protected Long defaultStep() { return 1L; }

    @Override
    public <T> List<T> map(Function<Long, T> mapper) {
        List<T> l = new ArrayList<>();
        long step = by();
        long len = inclusive ? to() + step : to();
        for (long i = from(); i < len; i+=step) {
            l.add(mapper.apply(i));
        }
        return l;
    }

    @Override
    public void each(Consumer<Long> consumer) {
        long step = by();
        long len = inclusive ? to() + step : to();
        for (long i = from(); i < len; i+=step) {
            consumer.accept(i);
        }
    }

    @Override
    public LongRange from(Long to){ super.from(to); return this; }

    @Override
    public LongRange to(Long to){ super.to(to); return this; }

    @Override
    public LongRange until(Long to){ super.until(to); return this; }

    @Override
    public boolean in(Long element) {
        return super.in(element) && (element - from()) % by() == 0;
    }

    public boolean allIn(Long... element) {
        return Arrays.stream(element).allMatch(this::in);
    }
    public boolean anyIn(Long... element) {
        return Arrays.stream(element).anyMatch(this::in);
    }
    public boolean noneIn(Long... element) {
        return !anyIn(element);
    }
}
