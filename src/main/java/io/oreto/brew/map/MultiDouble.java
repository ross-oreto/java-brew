package io.oreto.brew.map;

import java.util.Map;
import java.util.stream.Collectors;

public class MultiDouble<K> extends MultiMap<K, Double> {

    public Map<K, Double> sum() {
        return asMap().keySet().stream()
                .collect(Collectors.toMap(it -> it, it -> get(it).stream()
                        .mapToDouble(Double::doubleValue).sum()));
    }
}
