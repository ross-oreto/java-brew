package io.oreto.brew.data.jpa.repo;

import io.oreto.brew.data.jpa.DataStore;

public class Entity1Store extends DataStore<Long, Entity1> {
    public Entity1Store() {
        super(Entity1.class);
    }
}
