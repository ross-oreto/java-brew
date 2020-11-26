package io.oreto.brew.data.jpa;

import io.oreto.brew.data.Paged;
import io.oreto.brew.str.Str;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class DataStore {
    public static <T> Paged<T> findAll(EntityManager entityManager
            , Class<T> entityClass
            , String q
            , Paged.Page page
            , String... fetch) {
        return QueryParser.query(q
                , page
                , entityManager
                , entityClass
                , fetch);
    }

    public static <T> Paged<T> findAll(EntityManager entityManager
            , Class<T> entityClass
            , String q
            , String... fetch) {
        return QueryParser.query(q
                , Paged.Page.of()
                , entityManager
                , entityClass
                , fetch);
    }

    public static <T> Paged<T> list(EntityManager entityManager
            , Class<T> entityClass
            , Paged.Page page
            , String... fetch) {
        return findAll(entityManager, entityClass, "", page, fetch);
    }

    public static <T> Paged<T> list(EntityManager entityManager
            , Class<T> entityClass
            , String... fetch) {
        return findAll(entityManager, entityClass, "", fetch);
    }

    public static <T> Paged<T> list(EntityManager entityManager
            , Class<T> entityClass) {
        return findAll(entityManager, entityClass, "");
    }

    public static <T> Optional<T> findOne(EntityManager entityManager
            , Class<T> entityClass
            , String q
            , Paged.Page page
            , String... fetch) {
        Paged<T> list = QueryParser.query(q
                , page
                , entityManager
                , entityClass
                , fetch);
        return list.getPage().getCount() > 0 ? Optional.of(list.getList().get(0)) : Optional.empty();
    }

    public static <T> Optional<T> findOne(EntityManager entityManager
            , Class<T> entityClass
            , String q
            , String... fetch) {
        return findOne(entityManager, entityClass, q, Paged.Page.of(), fetch);
    }

    public static EntityTransaction tryTransaction(EntityManager entityManager) {
        try {
            return entityManager.isJoinedToTransaction() ? null : entityManager.getTransaction();
        } catch (IllegalStateException ignored){ }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T save(EntityManager entityManager, T t, String... fetch) {
        EntityTransaction trx = tryTransaction(entityManager);

        try {
            if (trx == null || trx.isActive())
                entityManager.persist(t);
            else {
                trx.begin();
                entityManager.persist(t);
                trx.commit();
            }
            return fetch.length == 0 ? t
                    : (T) get(entityManager
                    , t.getClass()
                    , entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(t)
                    , fetch).orElse(null);
        } catch(Exception x) {
            if (Objects.nonNull(trx)) trx.rollback();
            throw x;
        }
    }

    public static <ID, T> Optional<T> get(EntityManager entityManager, Class<T> entityClass, ID id, String... fetch) {
        EntityTransaction trx = tryTransaction(entityManager);
        try {
            Optional<T> t;
            if (trx == null || trx.isActive())
                t = DataStore.findOne(entityManager, entityClass, String.format(":%s", id), fetch);
            else {
                trx.begin();
                t = DataStore.findOne(entityManager, entityClass, String.format(":%s", id), fetch);
                trx.commit();
            }
            return t;
        } catch (Exception x) {
            if (Objects.nonNull(trx)) trx.rollback();
            throw x;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T update(EntityManager entityManager, T t, String... fetch) {
        EntityTransaction trx = tryTransaction(entityManager);
        try {
            T entity;
            if (trx == null || trx.isActive())
                entity = entityManager.merge(t);
            else {
                trx.begin();
                entity = entityManager.merge(t);
                trx.commit();
            }
            return fetch.length == 0 ? entity
                    : (T) get(entityManager
                    , entity.getClass()
                    , entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity)
                    , fetch).orElse(null);
        } catch(Exception x) {
            if (Objects.nonNull(trx)) trx.rollback();
            throw x;
        }
    }

    public static <ID, T> T delete(EntityManager entityManager, Class<T> entityClass, ID id) {
        EntityTransaction trx = tryTransaction(entityManager);
        try {
            Optional<T> t;
            if (trx == null || trx.isActive())  {
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
            if (Objects.nonNull(trx)) trx.rollback();
            throw x;
        }
    }

    public static class Q<T> {
        public static <T> Q<T> of (Class<T> entityClass) {
            return new Q<>(entityClass);
        }

        public static class Func {
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

        private Q<T> op(String name, QueryParser.Expression.Operator op, String value, Opt... opts) {
            logic().add(Arrays.stream(opts).anyMatch(it -> it == Opt.not) ? QueryParser.Expression.NOT : Str.EMPTY)
                    .add(name)
                    .add("::")
                    .add(op.name())
                    .add(Arrays.stream(opts).anyMatch(it -> it == Opt.prop) ? QueryParser.Expression.PROP : Str.EMPTY)
                    .add(':')
                    .add(value);
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

        public Q<T> eq(String name, Object value, Opt... opts) {
            boolean negate = Arrays.stream(opts).anyMatch(it -> it == Opt.not);
            if (value == null)
                return negate ? isNotNull(name) : isNull(name);

            return op(name, QueryParser.Expression.Operator.eq, value.toString(), opts);
        }

        public Q<T> gt(String name, String value, Opt... opts) {
            return op(name, QueryParser.Expression.Operator.gt, value, opts);
        }

        public Q<T> gte(String name, String value, Opt... opts) {
            return op(name, QueryParser.Expression.Operator.gte, value, opts);
        }

        public Q<T> lt(String name, String value, Opt... opts) {
            return op(name, QueryParser.Expression.Operator.lt, value, opts);
        }

        public Q<T> lte(String name, String value, Opt... opts) {
            return op(name, QueryParser.Expression.Operator.lte, value, opts);
        }

        public Q<T> isNull(String name) {
            logic().add(name).add("::").add(QueryParser.Expression.Operator.gte.name());
            return this;
        }

        public Q<T> isNotNull(String name) {
            logic().add(QueryParser.Expression.NOT).add(name).add("::").add(QueryParser.Expression.Operator.gte.name());
            return this;
        }

        public Q<T> contains(String name, String value, Opt... opts) {
            return op(name, QueryParser.Expression.Operator.contains, value, opts);
        }

        public Q<T> iContains(String name, String value, Opt... opts) {
            return op(name, QueryParser.Expression.Operator.icontains, value, opts);
        }

        public Q<T> startsWith(String name, String value, Opt... opts) {
            return op(name, QueryParser.Expression.Operator.startswith, value, opts);
        }

        public Q<T> iStartsWith(String name, String value, Opt... opts) {
            return op(name, QueryParser.Expression.Operator.istartswith, value, opts);
        }

        public Q<T> endsWith(String name, String value, Opt... opts) {
            return op(name, QueryParser.Expression.Operator.endswith, value, opts);
        }

        public Q<T> iEndsWith(String name, String value, Opt... opts) {
            return op(name, QueryParser.Expression.Operator.iendswith, value, opts);
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

        public Paged<T> find(EntityManager em, String...fetch) {
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

        public Optional<T> findOne(EntityManager em, String...fetch) {
            return DataStore.findOne(em, entityClass, str.toString(), Paged.Page.of().withSorting(order), fetch);
        }

        @Override
        public String toString() {
            return str.toString();
        }
    }
}
