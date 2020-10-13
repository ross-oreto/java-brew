package io.oreto.brew.map;

import java.util.*;
import java.util.stream.Collectors;

public class MultiMap<K, V> {
    static <K, V> MultiMap<K, V> create() {
        return new MultiMap<K, V>();
    }

    private final Map<K, List<V>> map = new HashMap<>();
    private boolean allowEmptyValues = true;

    public MultiMap<K, V> disallowEmptyValues(){ this.allowEmptyValues = false; return this; }
    public MultiMap<K, V> allowEmptyValues(){ this.allowEmptyValues = true; return this; }

    public final MultiMap<K, V> put(K k, V v) {
        if (allowEmptyValues || (v instanceof String && !"".equals(v)) || (!(v instanceof String) && v != null)) {
            if (map.containsKey(k)) map.get(k).add(v);
            else map.put(k, new ArrayList<V>() {{
                add(v);
            }});
        }
        return this;
    }

    public final MultiMap<K, V> put(K k){
        if (!map.containsKey(k)) map.put(k, new ArrayList<>());
        return this;
    }

    public final MultiMap<K, V> putAll(Map<? extends K, ? extends V> m) {
        m.forEach(this::put);
        return this;
    }

    public final MultiMap<K, V> putAll(Collection<Map.Entry<K, V>> entries) {
        entries.forEach(it -> put(it.getKey(), it.getValue()));
        return this;
    }

    public final List<V> get(K k) {
        return map.get(k);
    }

    public final Optional<List<V>> listAt(K k) {
        List<V> l = get(k);
        return l == null ? Optional.empty() : Optional.of(l);
    }

    public final int size() {
        return map.size();
    }

    public final Optional<Integer> sizeAt(K k) {
        return listAt(k).map(List::size);
    }

    public Collection<Map.Entry<K, List<V>>> entriesWith(V v) {
        return map.keySet().stream()
                .filter(k -> get(k).contains(v))
                .map(it -> (Map.Entry<K, List<V>>) new AbstractMap.SimpleEntry<>(it, get(it)))
                .collect(Collectors.toList());
    }

    public final boolean containsKey(K k) {
        return map.containsKey(k);
    }

    public final boolean isEmpty() {
        return map.isEmpty();
    }

    public final boolean containsValue(V v) {
        return map.keySet().stream().anyMatch(k -> get(k).contains(v));
    }

    public final MultiMap<K, V> remove(K key) {
        map.remove(key);
        return this;
    }

    public final MultiMap<K, V> clear(K key) {
        map.get(key).clear();
        return this;
    }

    public final MultiMap<K, V> clear() {
        map.clear();
        return this;
    }

    public final Set<K> keySet() {
        return map.keySet();
    }

    public final Collection<List<V>> values() {
        return map.values();
    }

    @SuppressWarnings("unchecked")
    private final Comparator<V> comparator = (o1, o2) -> ((Comparable<V>)o1).compareTo(o2);
    @SuppressWarnings("unchecked")
    private final Comparator<V> descending = (o1, o2) -> ((Comparable<V>)o2).compareTo(o1);

    /**
     * Sort each list in the map
     * @param c The comparator used by sort.
     * @return A self referencing MultiMap to support a fluent api.
     */
    public MultiMap<K, V> sort(Comparator<? super V> c) {
        map.forEach(((k, vs) -> vs.sort(c)));
        return this;
    }

    /**
     * Sort using the default ascending comparator which expects the value V to implement Comparable[V]
     * @return  A self referencing MultiMap to support a fluent api.
     */
    public MultiMap<K, V> sort() {
        return sort(comparator);
    }

    /**
     * Sort using the default descending order comparator which expects the value V to implement Comparable[V]
     * @return  A self referencing MultiMap to support a fluent api.
     */
    public MultiMap<K, V> sortDescending() {
        return sort(descending);
    }

    public Map<K, List<V>> asMap() { return map; }
}
