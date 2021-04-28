package io.oreto.brew.map;

import java.util.LinkedHashMap;
import java.util.Map;

public class Maps {
    public static class E<K, V> {
        public static <K, V> E<K, V> of(K k, V v) {
            return new E<K, V>(k, v);
        }

        private final K key;
        private final V val;

        public E(K key, V val) {
            this.key = key;
            this.val = val;
        }

        public K key() {
            return key;
        }

        public V val() {
            return val;
        }
    }

    @SafeVarargs
    public static <K, V> Map<K, V> of(E<K, V>... entries) {
        Map<K, V> kvMap = new LinkedHashMap<>();
        for (E<K, V> entry : entries)
            kvMap.put(entry.key, entry.val);
        return kvMap;
    }
}
