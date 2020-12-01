package io.oreto.brew.data.jpa.repo;

import io.oreto.brew.data.Model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Entity3 implements Model<Long> {

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;

    public Entity3(String name) {
        this.name = name;
    }

    public Entity3() { }

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

    public Entity3 withId(Long id) {
        this.id = id;
        return this;
    }

    public Entity3 withName(String name) {
        this.name = name;
        return this;
    }
}
