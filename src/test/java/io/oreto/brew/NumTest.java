package io.oreto.brew;

import io.oreto.brew.num.*;
import io.oreto.brew.num.Range;
import org.junit.Test;

import java.math.BigInteger;
import java.math.RoundingMode;

import static org.junit.Assert.*;

public class NumTest {

    @Test
    public void roundingTest() {
        assertEquals(.23, Num.round(.23), 0);
        assertEquals(0.0, Num.round(.23, 0), 0);
        assertEquals(.2, Num.round(.24, 1), 0);
        assertEquals(.3, Num.round(.25, 1), 0);
        assertEquals(.3, Num.round(.26, 1), 0);
        assertEquals(.27, Num.round(.263, 2, RoundingMode.UP), 0);

        assertEquals(1, Num.roundToInt(.5));
        assertEquals(0, Num.roundToInt(.4));
        assertEquals(1, Num.roundUp(.4));
        assertEquals(0, Num.roundDown(.9));
    }

    @Test
    public void rangeTest() {
        assertArrayEquals(new String[]{"0", "1", "2"}, IntRange.Until(3).map(Object::toString).toArray());
        assertArrayEquals(new String[]{"0", "1", "2", "3"}, IntRange.To(3).map(Object::toString).toArray());
        assertArrayEquals(new Integer[]{0, 1, 2, 3}, IntRange.To(3).toArray());
        assertArrayEquals(
                new Double[]{0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0}
                , DoubleRange.From(0.1).until(1.0).by(.1).map(it -> Num.round(it, 1)).toArray()
        );
        assertArrayEquals(
                new BigInteger[] {
                        BigInteger.valueOf(1000000000)
                        , BigInteger.valueOf(1000000001), BigInteger.valueOf(1000000002)
                }
                , BigIntRange.From(BigInteger.valueOf(1000000000)).to(BigInteger.valueOf(1000000002)).toArray()
        );
    }

    @Test
    public void inRangeTest() {

        assertTrue(Range.numberIn(1.5, 0, 10, true));

        assertTrue(IntRange.To(10).by(2).in(4));
        assertFalse(IntRange.To(10).by(2).in(5));

        assertFalse(IntRange.Until(10).by(2).in(10));
        assertTrue(IntRange.Until(10).by(2).in(0));

        assertTrue(DoubleRange.in(.5, .1, 1.0));
        assertTrue(DoubleRange.in(.1, .1, 1.0));
        assertTrue(DoubleRange.in(.9, .1, 1.0));
        assertTrue(DoubleRange.in(1.0, .1, 1.0, true));
    }
}
