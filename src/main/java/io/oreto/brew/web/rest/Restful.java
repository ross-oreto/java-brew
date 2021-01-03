package io.oreto.brew.web.rest;

import io.oreto.brew.data.Model;
import io.oreto.brew.data.Paged;
import io.oreto.brew.data.Pager;
import io.oreto.brew.data.Paginate;
import io.oreto.brew.data.jpa.Store;
import io.oreto.brew.obj.Reflect;
import io.oreto.brew.obj.Safe;
import io.oreto.brew.web.page.Form;
import io.oreto.brew.web.page.Notification;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public interface Restful<ID, T extends Model<ID>> extends Store<ID, T> {

    default Form<T> form(T data) {
        return Form.of(getEntityClass()).withData(data);
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

    default RestResponse<Paged<T>> list(Paginate pager, String... fetch) {
        return RestResponse.ok(List(pager, fetch));
    }
    default RestResponse<Paged<T>> list(String... fetch) {
        return RestResponse.ok(List(fetch));
    }
    default RestResponse<Paged<T>> list() {
        return RestResponse.ok(List());
    }

    default RestResponse<Paged<T>> find(String q, Paginate pager, String... fetch) {
        return RestResponse.ok(FindAll(q, pager, fetch));
    }
    default RestResponse<Paged<T>> find(String q, String... fetch) {
        return find(q, Pager.of(), fetch);
    }
    default RestResponse<T> findOne(String q, Paginate pager, String... fetch) {
        Optional<T> t = FindOne(q, pager, fetch);
        return t.map(RestResponse::ok).orElseGet(RestResponse::notFound);
    }
    default RestResponse<T> findOne(String q, String... fetch) {
        return findOne(q, Pager.of(), fetch);
    }

    default RestResponse<T> save(T entity, String... fetch) {
        Form<T> form = form(entity);
        EntityManager em = getEntityManager();
        if (form.submit()) {
            try {
                return RestResponse.created(Create(em, entity, fetch));
            } catch (PersistenceException e) {
                return persistenceExceptionResponse(e, entity);
            }
        } else {
            return RestResponse.unprocessable(entity).notify(form.getNotifications());
        }
    }
    default RestResponse<T> get(ID id, String... fetch) {
        Optional<T> t = Retrieve(id, fetch);
        return t.map(RestResponse::ok).orElseGet(RestResponse::notFound);
    }
    default RestResponse<T> update(T entity, String... fetch) {
        Form<T> form = form(entity);
        EntityManager em = getEntityManager();
        if (form.submit()) {
            try {
                entity = Update(em, entity);
                em.detach(entity);
                entity = Retrieve(em, entity.getId(), fetch).orElse(null);
                return entity == null ? RestResponse.notFound() : RestResponse.ok(entity);
            } catch (PersistenceException e) {
                em.detach(entity);
                return persistenceExceptionResponse(e, entity);
            }
        } else {
            em.detach(entity);
            return RestResponse.unprocessable(entity).notify(form.getNotifications());
        }
    }

    default RestResponse<T> update(ID id, Map<String, Object> fields, String... fetch) {
        Optional<T> t = Retrieve(id);
        if (t.isPresent()) {
            try {
                Reflect.copy(t.get(), fields, Reflect.CopyOptions.create().mergeCollections());
            } catch (ReflectiveOperationException e) {
                RestResponse<T> restResponse = RestResponse.unprocessable();
                restResponse.withError(e);
                return restResponse;
            }
            return update(t.get(), fetch);
        } else {
            return RestResponse.notFound();
        }
    }

    default RestResponse<T> update(ID id, T entity, Iterable<String> fields, String... fetch) {
        Optional<T> t = Retrieve(id);
        if (t.isPresent()) {
            try {
                Reflect.copy(t.get(), entity, fields, Reflect.CopyOptions.create().mergeCollections());
            } catch (ReflectiveOperationException e) {
                RestResponse<T> restResponse = RestResponse.unprocessable();
                restResponse.withError(e);
                return restResponse;
            }
            return update(t.get(), fetch);
        } else {
            return RestResponse.notFound();
        }
    }

    default RestResponse<T> replace(ID id, T entity, String... fetch) {
        if (entity.getId() == null)
            entity.setId(id);
        return update(entity, fetch);
    }

    default RestResponse<T> delete(ID id) {
        return Delete(id).isPresent() ? RestResponse.noContent() : RestResponse.notFound();
    }
}
