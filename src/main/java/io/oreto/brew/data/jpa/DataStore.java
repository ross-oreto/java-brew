package io.oreto.brew.data.jpa;

import io.oreto.brew.data.Paged;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DataStore {
    public static final Map<String, Object> EMPTY_MAP = new HashMap<String, Object>();

    public static <T> Map<String, Object> fetch(EntityManager entityManager, Class<T> entityClass,  String... fetch) {
        if (fetch.length == 0)
            return EMPTY_MAP;
        Map<String, Object> hints = new HashMap<String, Object>();
        EntityGraph<T> entityGraph = entityManager.createEntityGraph(entityClass);
        for (String graph : fetch)
            entityGraph.addSubgraph(graph);
        hints.put("javax.persistence.loadgraph", entityGraph);
        return hints;
    }

    public static <T> Paged<T> find(EntityManager entityManager
            , Class<T> entityClass
            , String q
            , Integer page
            , Integer max
            , String... sort) {
        return QueryParser.query(q
                , Paged.Page.of(page, max, Arrays.stream(sort).collect(Collectors.toList()))
                , entityManager
                , entityClass);
    }

    public static <T> Paged<T> find(EntityManager entityManager
            , Class<T> entityClass
            , String q
            , Integer page
            , Integer max) {
        return QueryParser.query(q
                , Paged.Page.of(page, max)
                , entityManager
                , entityClass);
    }

    public static <T> Paged<T> find(EntityManager entityManager
            , Class<T> entityClass
            , String q) {
        return QueryParser.query(q
                , Paged.Page.of()
                , entityManager
                , entityClass);
    }

    public static <T> Paged<T> find(EntityManager entityManager
            , Class<T> entityClass
            , String q
            , String... sort) {
        return QueryParser.query(q
                , Paged.Page.of().withSorting(sort)
                , entityManager
                , entityClass);
    }

    public static <T> Optional<T> findOne(EntityManager entityManager
            , Class<T> entityClass
            , String q
            , String... sort) {
        Paged<T> list = QueryParser.query(q
                , Paged.Page.of(1, 1, Arrays.stream(sort).collect(Collectors.toList()))
                , entityManager
                , entityClass);
        return list.getPage().getCount() > 0 ? Optional.of(list.getList().get(0)) : Optional.empty();
    }

    public static <T> T save(EntityManager entityManager, T t) {
        EntityTransaction trx = entityManager.getTransaction();
        try {
            if (trx.isActive())
                entityManager.persist(t);
            else {
                trx.begin();
                entityManager.persist(t);
                trx.commit();
            }
            return t;
        } catch(Exception x) {
            trx.rollback();
            throw x;
        }
    }

    public static <ID, T> Optional<T> get(EntityManager entityManager, Class<T> entityClass, ID id, String... fetch) {
        EntityTransaction trx = entityManager.getTransaction();
        try {
            T t;

            if (trx.isActive())
                t = entityManager.find(entityClass, id, fetch(entityManager, entityClass, fetch));
            else {
                trx.begin();
                t = entityManager.find(entityClass, id, fetch(entityManager, entityClass, fetch));
                trx.commit();
            }
            return t == null ? Optional.empty() : Optional.of(t);
        } catch (Exception x) {
            trx.rollback();
            throw x;
        }
    }

    public static <T> T update(EntityManager entityManager, T t) {
        EntityTransaction trx = entityManager.getTransaction();
        try {
            T entity;
            if (trx.isActive())
                entity = entityManager.merge(t);
            else {
                trx.begin();
                entity = entityManager.merge(t);
                trx.commit();
            }
            return entity;
        } catch(Exception x) {
            trx.rollback();
            throw x;
        }
    }

    public static <ID, T> T delete(EntityManager entityManager, Class<T> entityClass, ID id) {
        EntityTransaction trx = entityManager.getTransaction();
        try {
            Optional<T> t;
            if (trx.isActive()) {
                t = get(entityManager, entityClass, id);
                entityManager.remove(t.orElseThrow(EntityNotFoundException::new));
            } else {
                trx.begin();
                t = get(entityManager, entityClass, id);
                entityManager.remove(t.orElseThrow(EntityNotFoundException::new));
                trx.commit();
            }
            return t.get();
        } catch(Exception x) {
            trx.rollback();
            throw x;
        }
    }
}
