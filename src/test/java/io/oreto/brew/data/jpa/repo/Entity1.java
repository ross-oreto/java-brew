package io.oreto.brew.data.jpa.repo;

import javax.persistence.*;
import java.util.List;

@Entity
public class Entity1 {

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;

    @OneToMany(cascade = CascadeType.PERSIST)
    private List<Entity2> entity2s;

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

    public Entity1 withName(String name) {
        this.name = name;
        return this;
    }

    public Entity1 withEntity2s(List<Entity2> entity2s) {
        this.entity2s = entity2s;
        return this;
    }
}
