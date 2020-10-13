package io.oreto.brew.data.jpa.repo;

import javax.persistence.*;
import java.util.List;

@Entity
public class Entity2 {

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;

    @OneToMany(cascade = CascadeType.PERSIST)
    private List<Entity3> entity3s;

    public Entity2() { }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Entity3> getEntity3s() {
        return entity3s;
    }

    public void setEntity3s(List<Entity3> entity3s) {
        this.entity3s = entity3s;
    }

    public Entity2(String name) {
        this.name = name;
    }

    public Entity2 withId(Long id) {
        this.id = id;
        return this;
    }

    public Entity2 withName(String name) {
        this.name = name;
        return this;
    }

    public Entity2 withEntity3s(List<Entity3> entity3s) {
        this.entity3s = entity3s;
        return this;
    }
}
