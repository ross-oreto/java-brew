package io.oreto.brew.data.jpa;

import io.oreto.brew.data.*;

import javax.persistence.EntityManager;
import java.util.Optional;
import java.util.function.Function;

public interface Store<ID, T extends Model<ID>> extends Crud<ID, T> {
    EntityManager getEntityManager();
    Class<T> getEntityClass();

    default <U> U unit(Function<EntityManager, U> work) {
       EntityManager em = getEntityManager();
        U result = work.apply(em);
        em.close();
        return result;
    }

    default Paged<T> List(EntityManager em, Paginate pager, Fetch.Plan fetchPlan) {
        return DataStore.list(em, getEntityClass(), pager, fetchPlan);
    }

    default Paged<T> List(Paginate pager, Fetch.Plan fetchPlan) {
        return unit(em -> List(em, pager, fetchPlan));
    }

    default Paged<T> List(EntityManager em, Fetch.Plan fetchPlan) {
        return DataStore.list(em, getEntityClass(), fetchPlan);
    }

    default Paged<T> List(Fetch.Plan fetchPlan) {
        return unit(em -> List(em, fetchPlan));
    }

    default Paged<T> List(EntityManager em, Paginate pager) {
        return List(em, pager, Fetch.Plan.none());
    }

    default Paged<T> List(Paginate pager) {
        return unit(em -> List(em, pager, Fetch.Plan.none()));
    }

    default Paged<T> List(EntityManager em) {
        return List(em, Fetch.Plan.none());
    }

    default Paged<T> List() {
        return unit(em -> List(em, Fetch.Plan.none()));
    }

    default Paged<T> FindAll(EntityManager em, String q, Paginate pager, Fetch.Plan fetchPlan) {
        return DataStore.findAll(em, getEntityClass(), q, pager, fetchPlan);
    }

    default Paged<T> FindAll(String q, Paginate pager, Fetch.Plan fetchPlan) {
        return unit(em -> FindAll(em, q, pager, fetchPlan));
    }

    default Paged<T> FindAll(String q, Paginate pager) {
       return FindAll(q, pager, Fetch.Plan.none());
    }
    default Paged<T> FindAll(String q) {
        return unit(em -> FindAll(em, q, Pager.of(), Fetch.Plan.none()));
    }

    default Optional<T> FindOne(EntityManager em, String q, Paginate pager, Fetch.Plan fetchPlan) {
        return DataStore.findOne(em, getEntityClass(), q, pager, fetchPlan);
    }
    default Optional<T> FindOne(String q, Paginate pager, Fetch.Plan fetchPlan) {
        return unit(em -> FindOne(em, q, pager, fetchPlan));
    }
    default Optional<T> FindOne(String q, Paginate pager) {
        return unit(em -> FindOne(em, q, pager, Fetch.Plan.none()));
    }
    default Optional<T> FindOne(String q) {
        return unit(em -> FindOne(em, q, Pager.of(), Fetch.Plan.none()));
    }

    default T Create(EntityManager em, T t, Fetch.Plan fetchPlan) {
        return DataStore.save(em, t, fetchPlan);
    }

    @Override
    default T Create(T t, Fetch.Plan fetchPlan) {
        return unit(em -> Create(em, t, fetchPlan));
    }
    @Override
    default T Create(T t) {
        return unit(em -> Create(em, t, Fetch.Plan.none()));
    }

    default Optional<T> Retrieve(EntityManager em, ID id, Fetch.Plan fetchPlan) {
        return DataStore.get(em, getEntityClass(), id, fetchPlan);
    }

    @Override
    default Optional<T> Retrieve(ID id, Fetch.Plan fetchPlan) {
       return unit(em -> Retrieve(em, id, fetchPlan));
    }
    @Override
    default Optional<T> Retrieve(ID id) {
        return unit(em -> Retrieve(em, id, Fetch.Plan.none()));
    }

    default T Update(EntityManager em, T t, Fetch.Plan fetchPlan) {
        return DataStore.update(em, t, fetchPlan);
    }

    @Override
    default T Update(T t, Fetch.Plan fetchPlan) {
        return unit(em -> Update(em, t, fetchPlan));
    }
    @Override
    default T Update(T t) {
        return unit(em -> Update(em, t, Fetch.Plan.none()));
    }

    default T Delete(EntityManager em, T t) {
        return DataStore.delete(em, t);
    }

    @Override
    default T Delete(T t) {
        return unit(em -> Delete(em, t));
    }
}
