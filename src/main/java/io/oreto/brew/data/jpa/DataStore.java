package io.oreto.brew.data.jpa;

import io.oreto.brew.data.Paged;
import io.oreto.brew.str.Str;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import java.util.*;
import java.util.stream.Collectors;

public class DataStore {
    public static final Map<String, Object> EMPTY_MAP = new HashMap<String, Object>();

    public static <T> Map<String, Object> fetch(EntityManager entityManager, Class<T> entityClass, String... fetch) {
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
            , String[] sort
            , String... fetch) {
        return QueryParser.query(q
                , Paged.Page.of(page, max, Arrays.stream(sort).collect(Collectors.toList()))
                , entityManager
                , entityClass
                , fetch);
    }

    public static <T> Paged<T> find(EntityManager entityManager
            , Class<T> entityClass
            , String q
            , Integer page
            , Integer max
            , String... fetch) {
        return QueryParser.query(q
                , Paged.Page.of(page, max)
                , entityManager
                , entityClass
                , fetch);
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
            , String[] sort
            , String... fetch) {
        return QueryParser.query(q
                , Paged.Page.of().withSorting(sort)
                , entityManager
                , entityClass
                , fetch);
    }

    public static <T> Optional<T> findOne(EntityManager entityManager
            , Class<T> entityClass
            , String q
            , String[] sort
            , String[] fetch) {
        Paged<T> list = QueryParser.query(q
                , Paged.Page.of(1, 1, Arrays.stream(sort).collect(Collectors.toList()))
                , entityManager
                , entityClass
                , fetch);
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

    public static class Q<T> {
        static <T> Q<T> of (Class<T> entityClass) {
            return new Q<>(entityClass);
        }

        static class Func {
            public static String of(Function function, String name) {
                return String.format("%s{%s}", function.name(), name);
            }
        }

        private final Str str = Str.of();
        private final Class<T> entityClass;

        private boolean or;
        private Integer page;
        private Integer max;
        private String[] order;

        protected Q(Class<T> entityClass) {
            this.entityClass = entityClass;
            this.order = new String[] {};
        }

        private Str logic() {
            if (str.isNotEmpty()) {
                str.space()
                        .add(or ? QueryParser.Logical.Operator.or.name() : QueryParser.Logical.Operator.and.name())
                        .space();
            }
            return str;
        }

        private Q<T> op(String name, QueryParser.Expression.Operator op, String value) {
            logic().add(name).add("::").add(op.name()).add(':').add(value);
            return this;
        }

        public Q<T> page(int page) {
            this.page = page;
            return this;
        }

        public Q<T> order(String... order) {
            this.order = order;
            return this;
        }

        public Q<T> limit(int max) {
            this.max = max;
            return this;
        }

        public Q<T> eq(String name, Object value) {
            logic().add(name).add(':').add(value.toString());
            return this;
        }

        public Q<T> gt(String name, String value) {
            return op(name, QueryParser.Expression.Operator.gt, value);
        }

        public Q<T> gte(String name, String value) {
            return op(name, QueryParser.Expression.Operator.gte, value);
        }

        public Q<T> lt(String name, String value) {
            return op(name, QueryParser.Expression.Operator.lt, value);
        }

        public Q<T> lte(String name, String value) {
            return op(name, QueryParser.Expression.Operator.lte, value);
        }

        public Q<T> isnull(String name) {
            logic().add(name).add("::").add(QueryParser.Expression.Operator.gte.name());
            return this;
        }

        public Q<T> contains(String name, String value) {
            return op(name, QueryParser.Expression.Operator.contains, value);
        }

        public Q<T> iContains(String name, String value) {
            return op(name, QueryParser.Expression.Operator.icontains, value);
        }

        public Q<T> startsWith(String name, String value) {
            return op(name, QueryParser.Expression.Operator.startswith, value);
        }

        public Q<T> iStartsWith(String name, String value) {
            return op(name, QueryParser.Expression.Operator.istartswith, value);
        }

        public Q<T> endsWith(String name, String value) {
            return op(name, QueryParser.Expression.Operator.endswith, value);
        }

        public Q<T> iEndsWith(String name, String value) {
            return op(name, QueryParser.Expression.Operator.iendswith, value);
        }

        public Q<T> or() {
            or = true;
            return this;
        }

        public Q<T> endOr() {
            or = false;
            return this;
        }

        public Q<T> group() {
            str.add('(');
            return this;
        }

        public Q<T> endGroup() {
            str.add(')');
            return this;
        }

        Paged<T> find(EntityManager em, String...fetch) {
            Paged.Page paging = Paged.Page.of();
            if (Objects.nonNull(page))
                paging.setNumber(page);

            if (Objects.nonNull(max))
                paging.setSize(max);

            if (Objects.nonNull(order) && order.length > 0)
                paging.withSorting(order);

            return QueryParser.query(str.toString()
                    , paging
                    , em
                    , entityClass
                    , fetch);
        }

        Optional<T> findOne(EntityManager em, String...fetch) {
            return DataStore.findOne(em, entityClass, str.toString(), order, fetch);
        }
    }
}
