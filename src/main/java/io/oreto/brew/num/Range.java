package io.oreto.brew.num;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

abstract public class Range<T extends Number> extends io.oreto.brew.Range<T> {

    public static boolean numberIn(Number element, Number from, Number to, boolean inclusive) {
        boolean in;
        if (element instanceof Integer) {
            in = Range.in((Integer) element, from.intValue(), to.intValue(), inclusive);
        } else if (element instanceof Byte) {
            in = Range.in((Byte) element, from.byteValue(), to.byteValue(), inclusive);
        } else if (element instanceof Short) {
            in = Range.in((Short) element, from.shortValue(), to.shortValue(), inclusive);
        } else if (element instanceof Long) {
            in = Range.in((Long) element, from.longValue(), to.longValue(), inclusive);
        } else if (element instanceof Float) {
            in = Range.in((Float) element, from.floatValue(), to.floatValue(), inclusive);
        } else if (element instanceof Double) {
            in = Range.in((Double) element, from.doubleValue(), to.doubleValue(), inclusive);
        } else if (element instanceof BigInteger) {
            in = Range.in((BigInteger) element, (BigInteger) from, (BigInteger) to, inclusive);
        } else if (element instanceof BigDecimal) {
            in = Range.in((BigDecimal) element, (BigDecimal) from, (BigDecimal) to, inclusive);
        } else {
            in = false;
        }
        return in;
    }

    protected T step;
    public Range<T> by(T step) { this.step = step; return this; }
    public T by() { return step == null ? (step = defaultStep()) : step; }
    abstract protected T defaultStep();

    abstract public <R> List<R> map(Function<T, R> mapper);
    abstract public void each(Consumer<T> consumer);

    @SuppressWarnings("unchecked")
    public <R> R[] toArray() {
        return (R[]) map(it->it).toArray();
    }

    public List<T> toList() {
        return map(it->it);
    }
}
