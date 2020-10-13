package io.oreto.brew.time;

import io.oreto.brew.Range;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class DateRange extends Range<LocalDateTime> {
    public static DateRange From(LocalDateTime from) {
        return new DateRange().from(from);
    }
    public static DateRange To(LocalDateTime to) {
        return new DateRange().from(LocalDateTime.now()).to(to);
    }
    public static DateRange To(Duration to) {
        LocalDateTime date = LocalDateTime.now();
        return new DateRange().from(date).to(date.plus(to)).by(to);
    }
    public static DateRange Until(LocalDateTime to) {
        return new DateRange().from(LocalDateTime.now()).until(to);
    }
    public static DateRange Until(Duration to) {
        LocalDateTime date = LocalDateTime.now();
        return new DateRange().from(date).until(date.plus(to)).by(to);
    }

    protected Duration step;
    public DateRange by(Duration step) { this.step = step; return this; }
    public Duration by() { return step == null ? (step = defaultStep()) : step; }

    protected Duration defaultStep() { return Duration.ofDays(1); }

    public <T> List<T> map(Function<LocalDateTime, T> mapper) {
        List<T> l = new ArrayList<>();
        Duration step = by();

        LocalDateTime len = inclusive ? to().plus(step) : to();
        for (LocalDateTime i = from(); i.compareTo(len) < 0; i = i.plus(step)) {
            l.add(mapper.apply(i));
        }
        return l;
    }

    public void each(Consumer<LocalDateTime> consumer) {
        Duration step = by();
        LocalDateTime len = inclusive ? to().plus(step) : to();
        for (LocalDateTime i = from(); i.compareTo(len) < 0; i = i.plus(step)) {
            consumer.accept(i);
        }
    }

    @Override
    public DateRange from(LocalDateTime to){ super.from(to); return this; }

    @Override
    public DateRange to(LocalDateTime to){ super.to(to); return this; }

    public DateRange to(Duration to) {
        super.to(from().plus(to));
        return this;
    }

    @Override
    public DateRange until(LocalDateTime to){ super.until(to); return this; }

    public DateRange until(Duration to) {
        super.until(from().plus(to));
        return this;
    }

    public boolean allIn(LocalDateTime... element) {
        return Arrays.stream(element).allMatch(this::in);
    }
    public boolean anyIn(LocalDateTime... element) {
        return Arrays.stream(element).anyMatch(this::in);
    }
    public boolean noneIn(LocalDateTime... element) {
        return !anyIn(element);
    }
}
