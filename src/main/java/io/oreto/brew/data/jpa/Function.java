package io.oreto.brew.data.jpa;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum Function {
    count(true)
    , avg(true)
    , sum(true)
    , max(true)
    , min(true)
    , greatest(true)
    , least(true)
    , count_distinct(true);

    private boolean aggregate;

    Function(boolean aggregate) {
        this.aggregate = aggregate;
    }

    public static boolean isValid(String s) {
        return Arrays.stream(values()).map(Enum::name).collect(Collectors.toList()).contains(s);
    }

    public boolean isAggregate() {
        return aggregate;
    }
}
