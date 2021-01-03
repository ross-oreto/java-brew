package io.oreto.brew.data.jpa.repo;

import io.oreto.brew.web.rest.Restful;

import javax.persistence.EntityManager;

public class Entity5Store implements Restful<Long, Entity5> {
    private final EntityManager entityManager;

    public Entity5Store(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    public Class<Entity5> getEntityClass() {
        return Entity5.class;
    }
}
