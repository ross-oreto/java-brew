package io.oreto.brew;

import io.oreto.brew.time.DateRange;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TimeTest {

    @Test
    public void dateRangeTest() {
        LocalDateTime d = LocalDateTime.now();
        assertArrayEquals(
                new Integer[]{
                        d.getSecond()
                        , d.plusSeconds(1).getSecond()
                        , d.plusSeconds(2).getSecond()
                        , d.plusSeconds(3).getSecond()
                        , d.plusSeconds(4).getSecond()
                }
                , DateRange.From(d).until(Duration.ofSeconds(5))
                        .by(Duration.ofSeconds(1))
                        .map(LocalDateTime::getSecond).toArray()
        );

        assertArrayEquals(
                new String[]{
                        d.getDayOfWeek().name()
                        , d.plusDays(1).getDayOfWeek().name()
                        , d.plusDays(2).getDayOfWeek().name()
                        , d.plusDays(3).getDayOfWeek().name()
                        , d.plusDays(4).getDayOfWeek().name()
                        , d.plusDays(5).getDayOfWeek().name()
                        , d.plusDays(6).getDayOfWeek().name()
                        , d.plusDays(7).getDayOfWeek().name()
                }
                , DateRange.From(d).to(Duration.ofDays(7))
                        .by(Duration.ofDays(1))
                        .map(it -> it.getDayOfWeek().name()).toArray()
        );

        //assertFalse(DateRange.To(Duration.ofSeconds(10)).in(d));
        assertTrue(DateRange.To(Duration.ofSeconds(10)).in(LocalDateTime.now()));
    }
}
