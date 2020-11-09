package io.oreto.brew.data.jpa.repo;

import javax.persistence.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Entity
public class Entity1 {

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;

    @ElementCollection(fetch = FetchType.LAZY)
    private List<String> strings;

    @ElementCollection(fetch = FetchType.LAZY)
    private Map<String, String> entries;

    @OneToMany(cascade = CascadeType.PERSIST)
    private List<Entity2> entity2s;

    @OneToOne(cascade = CascadeType.PERSIST)
    private Entity2 entity2;

    public Entity1() { }

    public Entity1(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Entity2> getEntity2s() {
        return entity2s;
    }

    public Entity2 getEntity2() {
        return entity2;
    }

    public List<String> getStrings() {
        return strings;
    }

    public Map<String, String> getEntries() {
        return entries;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEntity2s(List<Entity2> entity2s) {
        this.entity2s = entity2s;
    }


    public Entity1 withId(Long id) {
        this.id = id;
        return this;
    }

    public void setStrings(List<String> strings) {
        this.strings = strings;
    }

    public void setEntries(Map<String, String> entries) {
        this.entries = entries;
    }

    public void setEntity2(Entity2 entity2) {
        this.entity2 = entity2;
    }

    public Entity1 withName(String name) {
        this.name = name;
        return this;
    }

    public Entity1 withStrings(String... strings) {
        this.strings = Arrays.stream(strings).collect(Collectors.toList());
        return this;
    }

    public Entity1 withEntries(Map<String, String> entries) {
        this.entries = entries;
        return this;
    }

    public Entity1 withEntity2s(List<Entity2> entity2s) {
        this.entity2s = entity2s;
        return this;
    }

    public Entity1 withEntity2(Entity2 entity2) {
        this.entity2 = entity2;
        return this;
    }
}
