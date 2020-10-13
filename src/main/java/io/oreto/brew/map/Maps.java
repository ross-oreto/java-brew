package io.oreto.brew.map;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

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
        return Arrays.stream(entries).collect(Collectors.toMap(E::key, E::val));
    }
}
