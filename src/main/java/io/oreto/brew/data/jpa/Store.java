package io.oreto.brew.data.jpa;

import io.oreto.brew.data.Crud;
import io.oreto.brew.data.Model;
import io.oreto.brew.data.Paged;
import io.oreto.brew.data.Paginate;

import javax.persistence.EntityManager;
import java.util.Optional;

public interface Store<ID, T extends Model<ID>> extends Crud<ID, T> {
    EntityManager getEntityManager();
    Class<T> getEntityClass();

    default Paged<T> List(EntityManager em, Paginate pager, String... fetch) {
        return DataStore.list(em, getEntityClass(), pager, fetch);
    }

    default Paged<T> List(Paginate pager, String... fetch) {
        return List(getEntityManager(), pager, fetch);
    }

    default Paged<T> List(EntityManager em, String... fetch) {
        return DataStore.list(em, getEntityClass(), fetch);
    }

    default Paged<T> List(String... fetch) {
        return List(getEntityManager(), fetch);
    }

    default Paged<T> List(EntityManager em) {
        return DataStore.list(em, getEntityClass());
    }

    default Paged<T> List() {
        return List(getEntityManager());
    }

    default Paged<T> FindAll(EntityManager em, String q, Paginate pager, String... fetch) {
        return DataStore.findAll(em, getEntityClass(), q, pager, fetch);
    }

    default Paged<T> FindAll(String q, Paginate pager, String... fetch) {
        return FindAll(getEntityManager(), q, pager, fetch);
    }

    default Optional<T> FindOne(EntityManager em, String q, Paginate pager, String... fetch) {
        return DataStore.findOne(em, getEntityClass(), q, pager, fetch);
    }

    default Optional<T> FindOne(String q, Paginate pager, String... fetch) {
        return FindOne(getEntityManager(), q, pager, fetch);
    }

    default T Create(EntityManager em, T t, String... fetch) {
        return DataStore.save(em, t, fetch);
    }

    @Override
    default T Create(T t, String... fetch) {
        return Create(getEntityManager(), t, fetch);
    }

    default Optional<T> Retrieve(EntityManager em, ID id, String... fetch) {
        return DataStore.get(em, getEntityClass(), id, fetch);
    }

    @Override
    default Optional<T> Retrieve(ID id, String... fetch) {
       return Retrieve(getEntityManager(), id, fetch);
    }

    default T Update(EntityManager em, T t, String... fetch) {
        return DataStore.update(em, t, fetch);
    }

    @Override
    default T Update(T t, String... fetch) {
        return Update(getEntityManager(), t, fetch);
    }

    default Optional<T> Delete(EntityManager em, ID id) {
        return DataStore.delete(em, getEntityClass(), id);
    }

    @Override
    default Optional<T> Delete(ID id) {
        return Delete(getEntityManager(), id);
    }
}
