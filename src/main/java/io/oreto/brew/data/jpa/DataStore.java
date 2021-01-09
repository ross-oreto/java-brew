package io.oreto.brew.data.jpa;

import io.oreto.brew.data.Paged;
import io.oreto.brew.data.Pager;
import io.oreto.brew.data.Paginate;
import io.oreto.brew.obj.Reflect;
import io.oreto.brew.obj.Safe;
import io.oreto.brew.str.Str;

import javax.persistence.*;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class DataStore {

    public static <T> Long count(EntityManager entityManager, Class<T> entityClass, String q) {
        return QueryParser.count(q, entityManager, entityClass);
    }

    public static <T> Long count(EntityManager entityManager, Class<T> entityClass) {
        return count(entityManager, entityClass, "");
    }

    public static <T> boolean exists(EntityManager entityManager, Class<T> entityClass, String q) {
        return count(entityManager, entityClass, q) > 0;
    }

    public static <T> boolean exists(EntityManager entityManager, Class<T> entityClass) {
        return exists(entityManager, entityClass, "");
    }

    public static <T> Paged<T> findAll(EntityManager entityManager
            , Class<T> entityClass
            , String q
            , Paginate pager
            , String... fetch) {
        return QueryParser.query(q
                , pager
                , entityManager
                , entityClass
                , fetch);
    }

    public static <T> Paged<T> findAll(EntityManager entityManager
            , Class<T> entityClass
            , String q
            , String... fetch) {
        return QueryParser.query(q
                , Pager.of()
                , entityManager
                , entityClass
                , fetch);
    }

    public static <T> Paged<T> list(EntityManager entityManager
            , Class<T> entityClass
            , Paginate pager
            , String... fetch) {
        return findAll(entityManager, entityClass, "", pager, fetch);
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
            , Paginate pager
            , String... fetch) {
        Paged<T> list = QueryParser.query(q
                , pager.disableCount()
                , entityManager
                , entityClass
                , fetch);
        return list.getPage().size() > 0 ? Optional.of(list.getPage().get(0)) : Optional.empty();
    }

    public static <T> Optional<T> findOne(EntityManager entityManager
            , Class<T> entityClass
            , String q
            , String... fetch) {
        return findOne(entityManager, entityClass, q, Pager.of(), fetch);
    }

    public static EntityTransaction tryTransaction(EntityManager entityManager) {
        try {
            return entityManager.getTransaction();
        } catch (IllegalStateException ignored){ }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T save(EntityManager entityManager, T t, String... fetch) {
        EntityTransaction trx = tryTransaction(entityManager);

        try {
            if (trx == null || trx.isActive()) {
                entityManager.persist(t);
            }
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
            String query = String.format(":%s", id);

            if (id.getClass().isAnnotationPresent(Embeddable.class) ||
                    id.getClass().isAnnotationPresent(IdClass.class)) {
                Q<?> q = Q.of(id.getClass());

                String idRef = "";
                if (id.getClass().isAnnotationPresent(Embeddable.class)) {
                    Optional<Field> idField =
                            Reflect.getAllFields(entityClass).stream()
                                    .filter(it -> it.isAnnotationPresent(EmbeddedId.class))
                                    .findFirst();
                    idRef = idField.isPresent() ? String.format("%s.", idField.get().getName()) : "";
                }
                String finalIdRef = idRef;
                Reflect.getAllFields(id.getClass()).forEach(it -> {
                    try {
                        q.eq(String.format("%s%s", finalIdRef, it.getName()), Reflect.getFieldValue(id, it));
                    } catch (ReflectiveOperationException e) {
                        e.printStackTrace();
                    }
                });
                query = q.toString();
            }

            Optional<T> t;
            if (trx == null || trx.isActive())
                t = DataStore.findOne(entityManager, entityClass, query, fetch);
            else {
                trx.begin();
                t = DataStore.findOne(entityManager, entityClass, query, fetch);
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
            if (trx == null || trx.isActive()) {
                entity = entityManager.merge(t);
                entityManager.flush();
            } else {
                trx.begin();
                entity = entityManager.merge(t);
                entityManager.flush();
                trx.commit();
            }
            entity = fetch.length == 0 ? entity
                    : (T) get(entityManager
                    , entity.getClass()
                    , entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity)
                    , fetch).orElse(null);
            return entity;
        } catch(Exception x) {
            entityManager.detach(t);
            if (Objects.nonNull(trx)) trx.rollback();
            throw x;
        }
    }

    public static <T> T delete(EntityManager entityManager, T t) {
        EntityTransaction trx = tryTransaction(entityManager);
        try {
            if (trx == null || trx.isActive())  {
                entityManager.remove(t);
            } else {
                trx.begin();
                entityManager.remove(t);
                trx.commit();
            }
            return t;
        } catch(Exception x) {
            if (Objects.nonNull(trx)) trx.rollback();
            throw x;
        }
    }

    public static <T> int deleteAll(EntityManager entityManager, Class<T> entityClass, Paginate pager) {
        int count = 0;
        EntityTransaction trx = tryTransaction(entityManager);
        try {
            if (trx == null || trx.isActive())  {
                for(T t : list(entityManager, entityClass, pager).getPage()) {
                    entityManager.remove(t);
                    count++;
                }
                entityManager.flush();
            } else {
                trx.begin();
                for(T t : list(entityManager, entityClass, pager).getPage()) {
                    entityManager.remove(t);
                    count++;
                }
                entityManager.flush();
                trx.commit();
            }
        } catch(Exception x) {
            if (Objects.nonNull(trx)) trx.rollback();
            throw x;
        }
        return count;
    }

    public static <T> int deleteAll(EntityManager entityManager, Class<T> entityClass) {
        return deleteAll(entityManager, entityClass, Pager.of(1, 100).disableCount());
    }

    public static <T> int deleteWhere(EntityManager entityManager, Class<T> entityClass, String q, Paginate pager) {
        int count = 0;
        EntityTransaction trx = tryTransaction(entityManager);
        try {
            if (trx == null || trx.isActive())  {
                for(T t : findAll(entityManager, entityClass, q, pager).getPage()) {
                    entityManager.remove(t);
                    count++;
                }
                entityManager.flush();
            } else {
                trx.begin();
                for(T t : findAll(entityManager, entityClass, q, pager).getPage()) {
                    entityManager.remove(t);
                    count++;
                }
                entityManager.flush();
                trx.commit();
            }
        } catch(Exception x) {
            if (Objects.nonNull(trx)) trx.rollback();
            throw x;
        }
        return count;
    }

    public static <T> int deleteWhere(EntityManager entityManager, Class<T> entityClass, String q) {
        return deleteWhere(entityManager, entityClass, q, Pager.of(1, 100).disableCount());
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
        private boolean started;

        protected Q(Class<T> entityClass) {
            this.entityClass = entityClass;
            this.order = new String[] {};
        }

        private Str logic() {
            if (started) {
                str.space()
                        .add(or ? QueryParser.Logical.Operator.or.name() : QueryParser.Logical.Operator.and.name())
                        .space();
            } else
                started = true;
            return str;
        }

        private Q<T> op(String name, QueryParser.Expression.Operator op, Object value, Opt... opts) {
            String val = Objects.nonNull(value) ? value.toString() : null;
            val = Objects.nonNull(val) && val.contains(" ") && !val.trim().startsWith("\"")
                    ? String.format("\"%s\n", val)
                    : val;
            logic()
                    .add(name)
                    .add("::")
                    .add(Arrays.stream(opts).anyMatch(it -> it == Opt.not) ? QueryParser.Expression.NOT : Str.EMPTY)
                    .add(op.name())
                    .add(Arrays.stream(opts).anyMatch(it -> it == Opt.prop) ? QueryParser.Expression.PROP : Str.EMPTY)
                    .add(':')
                    .add(val);
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

            return op(name, QueryParser.Expression.Operator.eq, Safe.of(value).q(Object::toString).val(), opts);
        }

        public Q<T> gt(String name, Object value, Opt... opts) {
            return op(name, QueryParser.Expression.Operator.gt, value, opts);
        }

        public Q<T> gte(String name, Object value, Opt... opts) {
            return op(name, QueryParser.Expression.Operator.gte, value, opts);
        }

        public Q<T> lt(String name, Object value, Opt... opts) {
            return op(name, QueryParser.Expression.Operator.lt, value, opts);
        }

        public Q<T> lte(String name, Object value, Opt... opts) {
            return op(name, QueryParser.Expression.Operator.lte, value, opts);
        }

        public Q<T> isNull(String name) {
            logic().add(name).add("::").add(QueryParser.Expression.Operator.isnull.name());
            return this;
        }

        public Q<T> isNotNull(String name) {
            logic().add(name).add("::").add(QueryParser.Expression.NOT).add(QueryParser.Expression.Operator.isnull.name());
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
            logic().add('(');
            started = false;
            return this;
        }

        public Q<T> endGroup() {
            str.add(')');
            return this;
        }

        public Paged<T> find(EntityManager em, String...fetch) {
            Pager pager = Pager.of();
            if (Objects.nonNull(page))
                pager.setPage(page);

            if (Objects.nonNull(max))
                pager.setSize(max);

            if (Objects.nonNull(order) && order.length > 0)
                pager.withSorting(order);

            return QueryParser.query(str.toString()
                    , pager
                    , em
                    , entityClass
                    , fetch);
        }

        public Optional<T> findOne(EntityManager em, String...fetch) {
            return DataStore.findOne(em, entityClass, str.toString(), Pager.of().withSorting(order), fetch);
        }

        public long count(EntityManager em) {
            return DataStore.count(em, entityClass, str.toString());
        }

        public boolean exists(EntityManager em) {
            return DataStore.exists(em, entityClass, str.toString());
        }

        @Override
        public String toString() {
            return str.toString();
        }
    }
}
