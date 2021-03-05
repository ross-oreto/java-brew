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
        return QueryReader.count(q, entityManager, entityClass);
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
            , Fetch.Plan fetchPlan) {
        return QueryReader.query(q
                , pager
                , entityManager
                , entityClass
                , fetchPlan);
    }

    public static <T> Paged<T> findAll(EntityManager entityManager
            , Class<T> entityClass
            , String q
            , Fetch.Plan fetchPlan) {
        return QueryReader.query(q
                , Pager.of()
                , entityManager
                , entityClass
                , fetchPlan);
    }

    public static <T> Paged<T> findAll(EntityManager entityManager
            , Class<T> entityClass
            , String q) {
       return findAll(entityManager, entityClass, q, Fetch.Plan.none());
    }

    public static <T> Paged<T> list(EntityManager entityManager
            , Class<T> entityClass
            , Paginate pager
            , Fetch.Plan fetchPlan) {
        return findAll(entityManager, entityClass, "", pager, fetchPlan);
    }
    public static <T> Paged<T> list(EntityManager entityManager
            , Class<T> entityClass
            , Paginate pager) {
        return findAll(entityManager, entityClass, "", pager, Fetch.Plan.none());
    }

    public static <T> Paged<T> list(EntityManager entityManager
            , Class<T> entityClass
            , Fetch.Plan fetchPlan) {
        return findAll(entityManager, entityClass, "", fetchPlan);
    }

    public static <T> Paged<T> list(EntityManager entityManager
            , Class<T> entityClass) {
        return list(entityManager, entityClass, Fetch.Plan.none());
    }

    public static <T> Optional<T> findOne(EntityManager entityManager
            , Class<T> entityClass
            , String q
            , Paginate pager
            , Fetch.Plan fetchPlan) {
        Paged<T> list = QueryReader.query(q
                , pager.disableCount()
                , entityManager
                , entityClass
                , fetchPlan);
        return list.getPage().size() > 0 ? Optional.of(list.getPage().get(0)) : Optional.empty();
    }

    public static <T> Optional<T> findOne(EntityManager entityManager
            , Class<T> entityClass
            , String q
            , Paginate pager) {
       return findOne(entityManager, entityClass, q, pager, Fetch.Plan.none());
    }

    public static <T> Optional<T> findOne(EntityManager entityManager
            , Class<T> entityClass
            , String q
            , Fetch.Plan fetchPlan) {
        return findOne(entityManager, entityClass, q, Pager.of(), fetchPlan);
    }

    public static <T> Optional<T> findOne(EntityManager entityManager
            , Class<T> entityClass
            , String q) {
        return findOne(entityManager, entityClass, q, Pager.of(), Fetch.Plan.none());
    }

    public static EntityTransaction tryTransaction(EntityManager entityManager) {
        try {
            return entityManager.getTransaction();
        } catch (IllegalStateException ignored){ }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T save(EntityManager entityManager, T t, Fetch.Plan fetchPlan) {
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

            return fetchPlan.isEmpty() ? t
                    : (T) get(entityManager
                    , t.getClass()
                    , entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(t)
                    , fetchPlan).orElse(null);
        } catch(Exception x) {
            if (Objects.nonNull(trx)) trx.rollback();
            throw x;
        }
    }

    public static <T> T save(EntityManager entityManager, T t) {
        return save(entityManager, t, Fetch.Plan.none());
    }

    public static <ID, T> Optional<T> get(EntityManager entityManager, Class<T> entityClass, ID id, Fetch.Plan fetchPlan) {
//        entityManager.find(entityClass, id);
        EntityTransaction trx = tryTransaction(entityManager);
        try {
            String query = String.format(":%s", id);

            if (id.getClass().isAnnotationPresent(Embeddable.class) ||
                    entityClass.isAnnotationPresent(IdClass.class)) {
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
                t = DataStore.findOne(entityManager, entityClass, query, fetchPlan);
            else {
                trx.begin();
                t = DataStore.findOne(entityManager, entityClass, query, fetchPlan);
                trx.commit();
            }
            return t;
        } catch (Exception x) {
            if (Objects.nonNull(trx)) trx.rollback();
            throw x;
        }
    }

    public static <ID, T> Optional<T> get(EntityManager entityManager, Class<T> entityClass, ID id) {
        return get(entityManager, entityClass, id, Fetch.Plan.none());
    }

    @SuppressWarnings("unchecked")
    public static <T> T update(EntityManager entityManager, T t, Fetch.Plan fetchPlan) {
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
            entity = fetchPlan.isEmpty() ? entity
                    : (T) get(entityManager
                    , entity.getClass()
                    , entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity)
                    , fetchPlan).orElse(null);
            return entity;
        } catch(Exception x) {
            entityManager.detach(t);
            if (Objects.nonNull(trx)) trx.rollback();
            throw x;
        }
    }

    public static <T> T update(EntityManager entityManager, T t) {
       return update(entityManager, t, Fetch.Plan.none());
    }

    public static <T> T delete(EntityManager entityManager, T t) {
        EntityTransaction trx = tryTransaction(entityManager);
        try {
            if (trx == null || trx.isActive())  {
                entityManager.remove(entityManager.contains(t) ? t: entityManager.merge(t));
            } else {
                trx.begin();
                entityManager.remove(entityManager.contains(t) ? t: entityManager.merge(t));
                entityManager.flush();
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
                for(T t : list(entityManager, entityClass, pager, Fetch.Plan.none()).getPage()) {
                    entityManager.remove(t);
                    count++;
                }
                entityManager.flush();
            } else {
                trx.begin();
                for(T t : list(entityManager, entityClass, pager, Fetch.Plan.none()).getPage()) {
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
                for(T t : findAll(entityManager, entityClass, q, pager, Fetch.Plan.none()).getPage()) {
                    entityManager.remove(t);
                    count++;
                }
                entityManager.flush();
            } else {
                trx.begin();
                for(T t : findAll(entityManager, entityClass, q, pager, Fetch.Plan.none()).getPage()) {
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
                        .add(or ? QueryReader.Logical.Operator.or.name() : QueryReader.Logical.Operator.and.name())
                        .space();
            } else
                started = true;
            return str;
        }

        private Q<T> op(String name, QueryReader.Expression.Operator op, Object value, Opt... opts) {
            String val = Objects.nonNull(value) ? value.toString() : null;
            val = Objects.nonNull(val) && val.contains(" ") && !val.trim().startsWith("\"")
                    ? String.format("\"%s\"", val)
                    : val;
            logic()
                    .add(name)
                    .add("::")
                    .add(Arrays.stream(opts).anyMatch(it -> it == Opt.not) ? QueryReader.Expression.NOT : Str.EMPTY)
                    .add(op.name())
                    .add(Arrays.stream(opts).anyMatch(it -> it == Opt.prop) ? QueryReader.Expression.PROP : Str.EMPTY)
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

            return op(name, QueryReader.Expression.Operator.eq, Safe.of(value).q(Object::toString).val(), opts);
        }

        public Q<T> gt(String name, Object value, Opt... opts) {
            return op(name, QueryReader.Expression.Operator.gt, value, opts);
        }

        public Q<T> gte(String name, Object value, Opt... opts) {
            return op(name, QueryReader.Expression.Operator.gte, value, opts);
        }

        public Q<T> lt(String name, Object value, Opt... opts) {
            return op(name, QueryReader.Expression.Operator.lt, value, opts);
        }

        public Q<T> lte(String name, Object value, Opt... opts) {
            return op(name, QueryReader.Expression.Operator.lte, value, opts);
        }

        public Q<T> isNull(String name) {
            logic().add(name).add("::").add(QueryReader.Expression.Operator.isnull.name());
            return this;
        }

        public Q<T> isNotNull(String name) {
            logic().add(name).add("::").add(QueryReader.Expression.NOT).add(QueryReader.Expression.Operator.isnull.name());
            return this;
        }

        public Q<T> contains(String name, String value, Opt... opts) {
            return op(name, QueryReader.Expression.Operator.contains, value, opts);
        }

        public Q<T> iContains(String name, String value, Opt... opts) {
            return op(name, QueryReader.Expression.Operator.icontains, value, opts);
        }

        public Q<T> startsWith(String name, String value, Opt... opts) {
            return op(name, QueryReader.Expression.Operator.startswith, value, opts);
        }

        public Q<T> iStartsWith(String name, String value, Opt... opts) {
            return op(name, QueryReader.Expression.Operator.istartswith, value, opts);
        }

        public Q<T> endsWith(String name, String value, Opt... opts) {
            return op(name, QueryReader.Expression.Operator.endswith, value, opts);
        }

        public Q<T> iEndsWith(String name, String value, Opt... opts) {
            return op(name, QueryReader.Expression.Operator.iendswith, value, opts);
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

        public Paged<T> find(EntityManager em, Fetch.Plan fetchPlan) {
            Pager pager = Pager.of();
            if (Objects.nonNull(page))
                pager.setPage(page);

            if (Objects.nonNull(max))
                pager.setSize(max);

            if (Objects.nonNull(order) && order.length > 0)
                pager.withSorting(order);

            return QueryReader.query(str.toString()
                    , pager
                    , em
                    , entityClass
                    , fetchPlan);
        }
        public Paged<T> find(EntityManager em) {
           return find(em, Fetch.Plan.none());
        }

        public Optional<T> findOne(EntityManager em, Fetch.Plan fetchPlan) {
            return DataStore.findOne(em, entityClass, str.toString(), Pager.of().withSorting(order), fetchPlan);
        }
        public Optional<T> findOne(EntityManager em) {
            return findOne(em, Fetch.Plan.none());
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
