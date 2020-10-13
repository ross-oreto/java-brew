package io.oreto.brew.num;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class DoubleRange extends Range<Double> {
    public static DoubleRange of(Double from, Double to) {
        return new DoubleRange().from(from).to(to);
    }
    public static DoubleRange From(Double from) {
        return new DoubleRange().from(from);
    }
    public static DoubleRange To(Double to) {
        return new DoubleRange().from(0D).to(to);
    }
    public static DoubleRange Until(Double to) {
        return new DoubleRange().from(0D).until(to);
    }

    @Override
    protected Double defaultStep() { return 1D; }

    @Override
    public <T> List<T> map(Function<Double, T> mapper) {
        List<T> l = new ArrayList<>();
        double step = by();
        double len = inclusive ? to() + step : to();
        for (double i = from(); i < len; i+=step) {
            l.add(mapper.apply(i));
        }
        return l;
    }

    @Override
    public void each(Consumer<Double> consumer) {
        double step = by();
        double len = inclusive ? to() + step : to();
        for (double i = from(); i < len; i+=step) {
            consumer.accept(i);
        }
    }

    @Override
    public DoubleRange from(Double to){ super.from(to); return this; }

    @Override
    public DoubleRange to(Double to){ super.to(to); return this; }

    @Override
    public DoubleRange until(Double to){ super.until(to); return this; }

    @Override
    public boolean in(Double element) {
        return super.in(element) && (element - from()) % by() == 0;
    }

    public boolean allIn(Double... element) {
        return Arrays.stream(element).allMatch(this::in);
    }
    public boolean anyIn(Double... element) {
        return Arrays.stream(element).anyMatch(this::in);
    }
    public boolean noneIn(Double... element) {
        return !anyIn(element);
    }
}
