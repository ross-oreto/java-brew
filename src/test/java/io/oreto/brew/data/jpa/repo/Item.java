package io.oreto.brew.data.jpa.repo;

import io.oreto.brew.data.Model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

@Entity
@Table
public class Item implements Model<Long>, Serializable, Comparable<Item>, Comparator<Item> {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;

    @ManyToMany(cascade = {CascadeType.PERSIST}, fetch = FetchType.LAZY, mappedBy = "items")
    private List<Order> orders;

    @ElementCollection(fetch = FetchType.LAZY)
    private Map<String, String> attributes;

    public Item() {
        orders = new ArrayList<>();
        attributes = new HashMap<>();
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public Item withId(Long id) {
        this.id = id;
        return this;
    }

    public Item withName(String name) {
        this.name = name;
        return this;
    }

    public Item addAttribute(String...entries) {
        int len = entries.length;
        for (int i = 0; i < len; i = i + 2)
            this.attributes.put(entries[i], i + 1 < len ? entries[i + 1] : null);
        return this;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), name);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Item)) {
            return false;
        }
        Item person = (Item) o;
        return Objects.equals(person.getId(), getId())
                && Objects.equals(person.getName(), getName());
    }

    @Override
    public int compare(Item o1, Item o2) {
        return Comparator.comparing(Item::getName)
                .thenComparing(Item::getId).compare(o1, o2);
    }

    @Override
    public int compareTo(Item o) {
        return this.compare(this, o);
    }
}
