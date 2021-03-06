package io.oreto.brew.web.rest;

import io.oreto.brew.data.Model;
import io.oreto.brew.data.Paged;
import io.oreto.brew.data.Pager;
import io.oreto.brew.data.Paginate;
import io.oreto.brew.data.jpa.Fetch;
import io.oreto.brew.data.jpa.Store;
import io.oreto.brew.obj.Reflect;
import io.oreto.brew.obj.Safe;
import io.oreto.brew.web.page.Form;
import io.oreto.brew.web.page.Notification;
import io.oreto.brew.web.page.Validatable;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public interface Restful<ID, T extends Model<ID>> extends Store<ID, T> {

    default Validatable validation(T data) {
        return Form.of(getEntityClass()).withModel(data);
    }

    default Validatable saveValidation(T data) {
        return validation(data);
    }

    default Validatable updateValidation(T data) {
        return validation(data);
    }

    default Validatable deleteValidation(T data) {
        return validation(data);
    }

    static <T> RestResponse<T> persistenceExceptionResponse(PersistenceException persistenceException, T entity) {
        Throwable t = persistenceException;
        Throwable cause = null;

        while (Objects.nonNull(t) && !Objects.equals(t, cause))  {
            cause = t;
            t = t.getCause();
        }
        Notification notification = Notification.of(
                   Safe.of(cause).q(it -> {
                       String message = it.getLocalizedMessage();
                       message = message.contains(";") ? message.substring(0, message.indexOf(';')) : message;
                       return message.contains(":") ? message.substring(0, message.indexOf(':')) : message;
                   }).orElse(persistenceException.getLocalizedMessage())
                   , Notification.Type.error, "persistence error");
        return RestResponse.unprocessable(entity).notify(notification);
    }

    default RestResponse<Paged<T>> list(Paginate pager, Fetch.Plan fetchPlan) {
        return RestResponse.ok(List(pager, fetchPlan));
    }
    default RestResponse<Paged<T>> list(Paginate pager) {
        return RestResponse.ok(List(pager));
    }
    default RestResponse<Paged<T>> list(Fetch.Plan fetchPlan) {
        return RestResponse.ok(List(fetchPlan));
    }
    default RestResponse<Paged<T>> list() {
        return RestResponse.ok(List());
    }

    default RestResponse<Paged<T>> find(String q, Paginate pager, Fetch.Plan fetchPlan) {
        return RestResponse.ok(FindAll(q, pager, fetchPlan));
    }
    default RestResponse<Paged<T>> find(String q, Paginate pager) {
        return find(q, pager, Fetch.Plan.none());
    }
    default RestResponse<Paged<T>> find(String q, Fetch.Plan fetchPlan) {
        return find(q, Pager.of(), fetchPlan);
    }
    default RestResponse<Paged<T>> find(String q) {
       return find(q, Fetch.Plan.none());
    }
    default RestResponse<T> findOne(String q, Paginate pager, Fetch.Plan fetchPlan) {
        Optional<T> t = FindOne(q, pager, fetchPlan);
        return t.map(RestResponse::ok).orElseGet(RestResponse::notFound);
    }
    default RestResponse<T> findOne(String q, Paginate pager) {
        return findOne(q, pager, Fetch.Plan.none());
    }
    default RestResponse<T> findOne(String q, Fetch.Plan fetchPlan) {
        return findOne(q, Pager.of(), fetchPlan);
    }
    default RestResponse<T> findOne(String q) {
        return findOne(q, Pager.of(), Fetch.Plan.none());
    }

    default RestResponse<T> save(T entity, Fetch.Plan fetchPlan) {
        Validatable validation = saveValidation(entity);
        if (validation.validate()) {
            try {
                return RestResponse.created(Create(entity, fetchPlan));
            } catch (PersistenceException e) {
                return persistenceExceptionResponse(e, entity);
            }
        } else {
            return RestResponse.unprocessable(entity).notify(validation.validationErrors());
        }
    }
    default RestResponse<T> save(T entity) {
       return save(entity, Fetch.Plan.none());
    }

    default RestResponse<T> get(ID id, Fetch.Plan fetchPlan) {
        Optional<T> t = Retrieve(id, fetchPlan);
        return t.map(RestResponse::ok).orElseGet(RestResponse::notFound);
    }
    default RestResponse<T> get(ID id) {
       return get(id, Fetch.Plan.none());
    }
    default RestResponse<T> update(T entity, Fetch.Plan fetchPlan) {
        return unit(em -> updateAndRetrieve(em, entity, fetchPlan));
    }
    default RestResponse<T> update(T entity) {
       return update(entity, Fetch.Plan.none());
    }

    default RestResponse<T> update(ID id, Map<String, Object> fields, Fetch.Plan fetchPlan) {
        T t = unit(em -> em.find(getEntityClass(), id));
        if (Objects.nonNull(t)) {
            try {
                Reflect.copy(t, fields, Reflect.CopyOptions.create().updateCollections());
            } catch (ReflectiveOperationException e) {
                RestResponse<T> restResponse = RestResponse.unprocessable();
                restResponse.withError(e);
                return restResponse;
            }
            return update(t, fetchPlan);
        } else {
            return RestResponse.notFound();
        }
    }
    default RestResponse<T> update(ID id, Map<String, Object> fields) {
        return update(id, fields, Fetch.Plan.none());
    }

    default RestResponse<T> updateAndRetrieve(EntityManager em, T entity, Fetch.Plan fetchPlan) {
        Validatable validation = updateValidation(entity);
        if (validation.validate()) {
            try {
                entity = Update(em, entity, Fetch.Plan.none());
                entity = Retrieve(em, entity.getId(), fetchPlan).orElse(null);
                return entity == null ? RestResponse.notFound() : RestResponse.ok(entity);
            } catch (PersistenceException e) {
                em.detach(entity);
                return persistenceExceptionResponse(e, entity);
            }
        } else {
            em.detach(entity);
            return RestResponse.unprocessable(entity).notify(validation.validationErrors());
        }
    }

    default RestResponse<T> update(ID id, T entity, Iterable<String> fields, Fetch.Plan fetchPlan) {
        return unit(em -> {
            T t = em.find(getEntityClass(), id);
            if (Objects.nonNull(t)) {
                try {
                    Reflect.copy(t, entity, fields, Reflect.CopyOptions.create().updateCollections());
                } catch (ReflectiveOperationException e) {
                    RestResponse<T> restResponse = RestResponse.unprocessable();
                    restResponse.withError(e);
                    return restResponse;
                }
                return updateAndRetrieve(em, t, fetchPlan);
            } else {
                return RestResponse.notFound();
            }
        });
    }
    default RestResponse<T> update(ID id, T entity, Iterable<String> fields) {
       return update(id, entity, fields, Fetch.Plan.none());
    }
    default RestResponse<T> update(ID id, T entity) {
        return update(id, entity, null, Fetch.Plan.none());
    }

    default RestResponse<T> replace(ID id, T entity, Fetch.Plan fetchPlan) {
        if (entity.getId() == null)
            entity.setId(id);
        return update(entity, fetchPlan);
    }
    default RestResponse<T> replace(ID id, T entity) {
       return replace(id, entity, Fetch.Plan.none());
    }

    default RestResponse<T> delete(T entity) {
        Validatable validation = deleteValidation(entity);
        if (validation.validate()) {
            Delete(entity);
            return RestResponse.noContent();
        } else {
            return RestResponse.unprocessable(entity).notify(validation.validationErrors());
        }
    }

    default RestResponse<T> delete(ID id) {
        return unit(em ->  {
            T t = em.find(getEntityClass(), id);
            if (t == null)
                return RestResponse.notFound();
            Validatable validation = deleteValidation(t);
            if (validation.validate()) {
                Delete(em, t);
                return RestResponse.noContent();
            } else {
                return RestResponse.unprocessable(t).notify(validation.validationErrors());
            }
        });
    }
}
