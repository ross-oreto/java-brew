package io.oreto.brew.data.jpa.repo;

import io.oreto.brew.data.Model;

import javax.persistence.*;

@Entity
public class Entity5 implements Model<Long> {

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(nullable = false)
    private String name;

    public Entity5(String name) {
        this.name = name;
    }

    public Entity5() { }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Entity5 withId(Long id) {
        this.id = id;
        return this;
    }

    public Entity5 withName(String name) {
        this.name = name;
        return this;
    }
}
