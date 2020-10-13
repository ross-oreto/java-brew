package io.oreto.brew.map;

import java.util.Map;
import java.util.stream.Collectors;

public class MultiLong<K> extends MultiMap<K, Long> {

    public Map<K, Long> sum() {
        return keySet().stream()
                .collect(Collectors.toMap(it -> it, it -> get(it).stream()
                        .mapToLong(Long::longValue).sum()));
    }
}
