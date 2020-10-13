package io.oreto.brew.num;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Num {
    public enum Type {
        natural     // 1, 2, 3, 4...
        , whole     // 0, 1, 2, 3...
        , integer   // -3, -2, -1, 0, 1, 2, 3
        , rational  // -1.1, -.5, 0, 1, 1.5, 2...
    }

    static final int defaultPlaces = 2;
    static final RoundingMode defaultRoundingMode = RoundingMode.HALF_UP;

    public static double round(double d, int places, RoundingMode roundingMode) {
        return new BigDecimal(d).setScale(places, roundingMode).doubleValue();
    }

    public static double round(double d, int places) {
        return new BigDecimal(d).setScale(places, defaultRoundingMode).doubleValue();
    }

    public static double round(double d) {
        return new BigDecimal(d).setScale(defaultPlaces, defaultRoundingMode).doubleValue();
    }

    public static float round(float d, int places, RoundingMode roundingMode) {
        return new BigDecimal(d).setScale(places, roundingMode).floatValue();
    }

    public static float round(float d, int places) {
        return new BigDecimal(d).setScale(places, defaultRoundingMode).floatValue();
    }

    public static float round(float d) {
        return new BigDecimal(d).setScale(defaultPlaces, defaultRoundingMode).floatValue();
    }

    public static BigDecimal round(BigDecimal d, int places, RoundingMode roundingMode) {
        return d.setScale(places, roundingMode);
    }

    public static BigDecimal round(BigDecimal d, int places) {
        return d.setScale(places, defaultRoundingMode);
    }

    public static BigDecimal round(BigDecimal d) {
        return d.setScale(defaultPlaces, defaultRoundingMode);
    }

    // --------------------------------- INTEGER ROUNDING ---------------------------------

    public static int roundToInt(double d, RoundingMode roundingMode) {
        return new BigDecimal(d).setScale(0, roundingMode).intValue();
    }

    public static int roundToInt(double d) {
        return new BigDecimal(d).setScale(0, defaultRoundingMode).intValue();
    }

    public static int roundUp(double d) {
        return new BigDecimal(d).setScale(0, RoundingMode.UP).intValue();
    }

    public static int roundDown(double d) {
        return new BigDecimal(d).setScale(0, RoundingMode.DOWN).intValue();
    }

    public static int roundToInt(float d, RoundingMode roundingMode) {
        return new BigDecimal(d).setScale(0, roundingMode).intValue();
    }

    public static int roundUp(float d) {
        return new BigDecimal(d).setScale(0, RoundingMode.UP).intValue();
    }

    public static int roundDown(float d) {
        return new BigDecimal(d).setScale(0, RoundingMode.DOWN).intValue();
    }

    public static int roundToInt(BigDecimal d, RoundingMode roundingMode) {
        return d.setScale(0, roundingMode).intValue();
    }

    public static int roundUp(BigDecimal d) {
        return d.setScale(0, RoundingMode.UP).intValue();
    }

    public static int roundDown(BigDecimal d) {
        return d.setScale(0, RoundingMode.DOWN).intValue();
    }
}
