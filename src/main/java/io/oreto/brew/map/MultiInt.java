package io.oreto.brew.map;

import java.util.Map;
import java.util.stream.Collectors;

public class MultiInt<K> extends MultiMap<K, Integer> {

    public Map<K, Integer> sum() {
        return keySet().stream()
                .collect(Collectors.toMap(it -> it, it -> get(it).stream()
                        .mapToInt(Integer::intValue).sum()));
    }
}
