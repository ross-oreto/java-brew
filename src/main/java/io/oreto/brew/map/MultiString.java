package io.oreto.brew.map;

import java.util.Map;
import java.util.stream.Collectors;

public class MultiString<K> extends MultiMap<K, String> {

    public Map<K, String> join(CharSequence s) {
        return keySet().stream()
                .collect(Collectors.toMap(it -> it, it -> String.join(s, get(it))));
    }

    public Map<K, String> join() {
        return join(",");
    }
}
