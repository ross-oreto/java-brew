package io.oreto.brew.num;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class BigIntRange extends Range<BigInteger> {
    public static BigIntRange of(BigInteger from, BigInteger to) {
        return new BigIntRange().from(from).to(to);
    }
    public static BigIntRange From(BigInteger from) {
        return new BigIntRange().from(from);
    }
    public static BigIntRange To(BigInteger to) {
        return new BigIntRange().from(BigInteger.ZERO).to(to);
    }
    public static BigIntRange Until(BigInteger to) {
        return new BigIntRange().from(BigInteger.ZERO).until(to);
    }

    @Override
    protected BigInteger defaultStep() { return BigInteger.valueOf(1L); }

    @Override
    public <T> List<T> map(Function<BigInteger, T> mapper) {
        List<T> l = new ArrayList<>();
        BigInteger step = by();
        BigInteger len = inclusive ? to().add(step) : to();
        for (BigInteger i = from(); i.compareTo(len) < 0; i = i.add(step)) {
            l.add(mapper.apply(i));
        }
        return l;
    }

    @Override
    public void each(Consumer<BigInteger> consumer) {
        BigInteger step = by();
        BigInteger len = inclusive ? to().add(step) : to();
        for (BigInteger i = from(); i.compareTo(len) < 0; i = i.add(step)) {
            consumer.accept(i);
        }
    }

    @Override
    public BigIntRange from(BigInteger to){ super.from(to); return this; }

    @Override
    public BigIntRange to(BigInteger to){ super.to(to); return this; }

    @Override
    public BigIntRange until(BigInteger to){ super.until(to); return this; }

    @Override
    public boolean in(BigInteger element) {
        return super.in(element) && element.subtract(from()).mod(by()).equals(BigInteger.ZERO);
    }

    public boolean allIn(BigInteger... element) {
        return Arrays.stream(element).allMatch(this::in);
    }
    public boolean anyIn(BigInteger... element) {
        return Arrays.stream(element).anyMatch(this::in);
    }
    public boolean noneIn(BigInteger... element) {
        return !anyIn(element);
    }
}
