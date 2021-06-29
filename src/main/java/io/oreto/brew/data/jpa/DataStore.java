package io.oreto.brew.data.jpa;

import io.oreto.brew.data.Paged;
import io.oreto.brew.data.Pager;
import io.oreto.brew.data.Paginate;
import io.oreto.brew.data.Sortable;
import io.oreto.brew.obj.Reflect;

import javax.persistence.*;
import java.lang.reflect.Field;
import java.util.List;
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
                entityManager.clear();
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
                entityManager.clear();
            } else {
                trx.begin();
                for(T t : list(entityManager, entityClass, pager, Fetch.Plan.none()).getPage()) {
                    entityManager.remove(t);
                    count++;
                }
                entityManager.flush();
                entityManager.clear();
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
                entityManager.clear();
            } else {
                trx.begin();
                for(T t : findAll(entityManager, entityClass, q, pager, Fetch.Plan.none()).getPage()) {
                    entityManager.remove(t);
                    count++;
                }
                entityManager.flush();
                entityManager.clear();
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
                return QStr.Func.of(function, name);
            }
        }

        private final QStr q = QStr.of();
        private final Class<T> entityClass;

        protected Q(Class<T> entityClass) {
            this.entityClass = entityClass;
        }

        public Q<T> page(int page) {
            q.page(page);
            return this;
        }

        public Q<T> order(String... order) {
            q.order(order);
            return this;
        }

        public Q<T> order(List<Sortable> order) {
            q.order(order.stream().map(Sortable::toString).toArray(String[]::new));
            return this;
        }

        public Q<T> limit(int max) {
            q.limit(max);
            return this;
        }

        public Q<T> eq(String name, Object value, Opt... opts) {
            q.eq(name, value, opts);
            return this;
        }

        public Q<T> gt(String name, Object value, Opt... opts) {
            q.gt(name, value, opts);
            return this;
        }

        public Q<T> gte(String name, Object value, Opt... opts) {
            q.gte(name, value, opts);
            return this;
        }

        public Q<T> lt(String name, Object value, Opt... opts) {
            q.lt(name, value, opts);
            return this;
        }

        public Q<T> lte(String name, Object value, Opt... opts) {
            q.lte(name, value, opts);
            return this;
        }

        public Q<T> isNull(String name) {
            q.isNull(name);
            return this;
        }

        public Q<T> isNotNull(String name) {
            q.isNotNull(name);
            return this;
        }

        public Q<T> contains(String name, String value, Opt... opts) {
            q.contains(name, value, opts);
            return this;
        }

        public Q<T> iContains(String name, String value, Opt... opts) {
            q.iContains(name, value, opts);
            return this;
        }

        public Q<T> startsWith(String name, String value, Opt... opts) {
            q.startsWith(name, value, opts);
            return this;
        }

        public Q<T> iStartsWith(String name, String value, Opt... opts) {
            q.iStartsWith(name, value, opts);
            return this;
        }

        public Q<T> endsWith(String name, String value, Opt... opts) {
            q.endsWith(name, value, opts);
            return this;
        }

        public Q<T> iEndsWith(String name, String value, Opt... opts) {
            q.iEndsWith(name, value, opts);
            return this;
        }

        public Q<T> or() {
            q.or();
            return this;
        }

        public Q<T> endOr() {
            q.endOr();
            return this;
        }

        public Q<T> group() {
            q.group();
            return this;
        }

        public Q<T> endGroup() {
            q.endGroup();
            return this;
        }

        public Paged<T> find(EntityManager em, Fetch.Plan fetchPlan) {
            Pager pager = Pager.of();
            if (Objects.nonNull(q.page))
                pager.setPage(q.page);

            if (Objects.nonNull(q.max))
                pager.setSize(q.max);

            if (Objects.nonNull(q.order) && q.order.length > 0)
                pager.withSorting(q.order);

            return QueryReader.query(q.toString()
                    , pager
                    , em
                    , entityClass
                    , fetchPlan);
        }
        public Paged<T> find(EntityManager em) {
            return find(em, Fetch.Plan.none());
        }

        public Optional<T> findOne(EntityManager em, Fetch.Plan fetchPlan) {
            return DataStore.findOne(em, entityClass, q.toString(), Pager.of().withSorting(q.order), fetchPlan);
        }
        public Optional<T> findOne(EntityManager em) {
            return findOne(em, Fetch.Plan.none());
        }

        public long count(EntityManager em) {
            return DataStore.count(em, entityClass, q.toString());
        }

        public boolean exists(EntityManager em) {
            return DataStore.exists(em, entityClass, q.toString());
        }

        @Override
        public String toString() {
            return q.toString();
        }
    }
}
