package io.oreto.brew.data.jpa.repo;

import io.oreto.brew.web.rest.Restful;

import javax.persistence.EntityManager;

public class Entity1Store implements Restful<Long, Entity1> {
    private final EntityManager entityManager;

    public Entity1Store(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    public Class<Entity1> getEntityClass() {
        return Entity1.class;
    }
}
