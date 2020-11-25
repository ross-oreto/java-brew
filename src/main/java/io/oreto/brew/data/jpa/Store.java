package io.oreto.brew.data.jpa;

import io.oreto.brew.data.Crud;
import io.oreto.brew.data.Paged;

import javax.persistence.EntityManager;
import java.util.Optional;

public interface Store<ID, T> extends Crud<ID, T> {
    EntityManager getEntityManager();
    Class<T> getEntityClass();

    default Paged<T> FindAll(String q, Paged.Page page, String... fetch) {
        return DataStore.findAll(getEntityManager(), getEntityClass(), q, page, fetch);
    }

    default Optional<T> FindOne(String q, Paged.Page page, String... fetch) {
        return DataStore.findOne(getEntityManager(), getEntityClass(), q, page, fetch);
    }

    @Override
    default T Create(T t, String... fetch) {
        return DataStore.save(getEntityManager(), t, fetch);
    }

    @Override
    default Optional<T> Retrieve(ID id, String... fetch) {
       return DataStore.get(getEntityManager(), getEntityClass(), id, fetch);
    }

    @Override
    default T Update(T t, String... fetch) {
        return DataStore.update(getEntityManager(), t, fetch);
    }

    @Override
    default T Delete(ID id) {
        return DataStore.delete(getEntityManager(), getEntityClass(), id);
    }
}
