package io.oreto.brew.data.jpa;

import io.oreto.brew.data.*;

import javax.persistence.EntityManager;
import java.util.Optional;

public interface Store<ID, T extends Model<ID>> extends Crud<ID, T> {
    EntityManager getEntityManager();
    Class<T> getEntityClass();

    default Paged<T> List(EntityManager em, Paginate pager, Fetch.Plan fetchPlan) {
        return DataStore.list(em, getEntityClass(), pager, fetchPlan);
    }

    default Paged<T> List(Paginate pager, Fetch.Plan fetchPlan) {
        return List(getEntityManager(), pager, fetchPlan);
    }

    default Paged<T> List(EntityManager em, Fetch.Plan fetchPlan) {
        return DataStore.list(em, getEntityClass(), fetchPlan);
    }

    default Paged<T> List(Fetch.Plan fetchPlan) {
        return List(getEntityManager(), fetchPlan);
    }

    default Paged<T> List(EntityManager em, Paginate pager) {
        return List(em, pager, Fetch.Plan.none());
    }

    default Paged<T> List(Paginate pager) {
        return List(getEntityManager(), pager, Fetch.Plan.none());
    }

    default Paged<T> List(EntityManager em) {
        return List(em, Fetch.Plan.none());
    }

    default Paged<T> List() {
        return List(getEntityManager(), Fetch.Plan.none());
    }

    default Paged<T> FindAll(EntityManager em, String q, Paginate pager, Fetch.Plan fetchPlan) {
        return DataStore.findAll(em, getEntityClass(), q, pager, fetchPlan);
    }

    default Paged<T> FindAll(String q, Paginate pager, Fetch.Plan fetchPlan) {
        return FindAll(getEntityManager(), q, pager, fetchPlan);
    }

    default Paged<T> FindAll(String q, Paginate pager) {
       return FindAll(q, pager, Fetch.Plan.none());
    }
    default Paged<T> FindAll(String q) {
        return FindAll(getEntityManager(), q, Pager.of(), Fetch.Plan.none());
    }

    default Optional<T> FindOne(EntityManager em, String q, Paginate pager, Fetch.Plan fetchPlan) {
        return DataStore.findOne(em, getEntityClass(), q, pager, fetchPlan);
    }
    default Optional<T> FindOne(String q, Paginate pager, Fetch.Plan fetchPlan) {
        return FindOne(getEntityManager(), q, pager, fetchPlan);
    }
    default Optional<T> FindOne(String q, Paginate pager) {
        return FindOne(getEntityManager(), q, pager, Fetch.Plan.none());
    }
    default Optional<T> FindOne(String q) {
        return FindOne(getEntityManager(), q, Pager.of(), Fetch.Plan.none());
    }

    default T Create(EntityManager em, T t, Fetch.Plan fetchPlan) {
        return DataStore.save(em, t, fetchPlan);
    }

    @Override
    default T Create(T t, Fetch.Plan fetchPlan) {
        return Create(getEntityManager(), t, fetchPlan);
    }
    @Override
    default T Create(T t) {
        return Create(getEntityManager(), t, Fetch.Plan.none());
    }

    default Optional<T> Retrieve(EntityManager em, ID id, Fetch.Plan fetchPlan) {
        return DataStore.get(em, getEntityClass(), id, fetchPlan);
    }

    @Override
    default Optional<T> Retrieve(ID id, Fetch.Plan fetchPlan) {
       return Retrieve(getEntityManager(), id, fetchPlan);
    }
    @Override
    default Optional<T> Retrieve(ID id) {
        return Retrieve(getEntityManager(), id, Fetch.Plan.none());
    }

    default T Update(EntityManager em, T t, Fetch.Plan fetchPlan) {
        return DataStore.update(em, t, fetchPlan);
    }

    @Override
    default T Update(T t, Fetch.Plan fetchPlan) {
        return Update(getEntityManager(), t, fetchPlan);
    }
    @Override
    default T Update(T t) {
        return Update(getEntityManager(), t, Fetch.Plan.none());
    }

    default T Delete(EntityManager em, T t) {
        return DataStore.delete(em, t);
    }

    @Override
    default T Delete(T t) {
        return Delete(getEntityManager(), t);
    }
}
