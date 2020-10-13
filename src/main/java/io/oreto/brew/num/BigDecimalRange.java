package io.oreto.brew.num;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class BigDecimalRange extends Range<BigDecimal> {
    public static BigDecimalRange of(BigDecimal from, BigDecimal to) {
        return new BigDecimalRange().from(from).to(to);
    }
    public static BigDecimalRange From(BigDecimal from) {
        return new BigDecimalRange().from(from);
    }
    public static BigDecimalRange To(BigDecimal to) {
        return new BigDecimalRange().from(BigDecimal.ZERO).to(to);
    }
    public static BigDecimalRange Until(BigDecimal to) {
        return new BigDecimalRange().from(BigDecimal.ZERO).until(to);
    }

    @Override
    protected BigDecimal defaultStep() { return BigDecimal.valueOf(1L); }

    @Override
    public <T> List<T> map(Function<BigDecimal, T> mapper) {
        List<T> l = new ArrayList<>();
        BigDecimal step = by();
        BigDecimal len = inclusive ? to().add(step) : to();
        for (BigDecimal i = from(); i.compareTo(len) < 0; i = i.add(step)) {
            l.add(mapper.apply(i));
        }
        return l;
    }

    @Override
    public void each(Consumer<BigDecimal> consumer) {
        BigDecimal step = by();
        BigDecimal len = inclusive ? to().add(step) : to();
        for (BigDecimal i = from(); i.compareTo(len) < 0; i = i.add(step)) {
            consumer.accept(i);
        }
    }

    @Override
    public BigDecimalRange from(BigDecimal to){ super.from(to); return this; }

    @Override
    public BigDecimalRange to(BigDecimal to){ super.to(to); return this; }

    @Override
    public BigDecimalRange until(BigDecimal to){ super.until(to); return this; }

    @Override
    public boolean in(BigDecimal element) {
        return super.in(element) && element.subtract(from()).remainder(by()).equals(BigDecimal.ZERO);
    }

    public boolean allIn(BigDecimal... element) {
        return Arrays.stream(element).allMatch(this::in);
    }
    public boolean anyIn(BigDecimal... element) {
        return Arrays.stream(element).anyMatch(this::in);
    }
    public boolean noneIn(BigDecimal... element) {
        return !anyIn(element);
    }
}
