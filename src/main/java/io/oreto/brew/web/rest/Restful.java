package io.oreto.brew.web.rest;

import io.oreto.brew.collections.Lists;
import io.oreto.brew.data.Model;
import io.oreto.brew.data.Paged;
import io.oreto.brew.data.jpa.Store;
import io.oreto.brew.obj.Reflect;
import io.oreto.brew.obj.Safe;
import io.oreto.brew.web.page.Form;
import io.oreto.brew.web.page.Notification;

import javax.persistence.PersistenceException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public interface Restful<ID, T extends Model<ID>> extends Store<ID, T> {
    default Form<T> form() {
        return Form.of(getEntityClass());
    }

    default RestResponse<T> persistenceExceptionResponse(PersistenceException persistenceException, T entity) {
        Throwable t = persistenceException;
        Throwable cause = null;

        while (Objects.nonNull(t) )  {
            cause = t;
            t = t.getCause();
        }
        List<Notification> notifications = Lists.of(new Notification(PersistenceException.class.getSimpleName()
                   , Safe.of(cause).q(it -> {
                       String message = it.getLocalizedMessage();
                       return message.contains(":") ? message.substring(0, message.indexOf(':')) : message;
                   }).orElse(persistenceException.getLocalizedMessage())
                   , Notification.Type.error));
        return RestResponse.unprocessable(entity).withNotifications(notifications);
    }

    default RestResponse<Paged<T>> find(String q, Paged.Page page, String... fetch) {
        return RestResponse.ok(FindAll(q, page, fetch));
    }
    default RestResponse<Paged<T>> find(String q, String... fetch) {
        return find(q, Paged.Page.of(), fetch);
    }
    default RestResponse<T> findOne(String q, Paged.Page page, String... fetch) {
        Optional<T> t = FindOne(q, page, fetch);
        return t.map(RestResponse::ok).orElseGet(RestResponse::notFound);
    }
    default RestResponse<T> findOne(String q, String... fetch) {
        return findOne(q, Paged.Page.of(), fetch);
    }

    default RestResponse<T> save(T entity, String... fetch) {
        Form<T> form = form().withData(entity);
        if (form.submit()) {
            try {
                return RestResponse.created(Create(entity, fetch));
            } catch (PersistenceException e) {
                return persistenceExceptionResponse(e, entity);
            }
        } else {
            return RestResponse.unprocessable(entity).withNotifications(form.getNotifications());
        }
    }
    default RestResponse<T> get(ID id, String... fetch) {
        Optional<T> t = Retrieve(id, fetch);
        return t.map(RestResponse::ok).orElseGet(RestResponse::notFound);
    }
    default RestResponse<T> update(T entity, String... fetch) {
        Form<T> form = form().withData(entity);
        if (form.submit()) {
            try {
                entity = Update(entity);
                getEntityManager().detach(entity);
                entity = Retrieve(entity.getId(), fetch).orElse(null);
                return entity == null ? RestResponse.notFound() : RestResponse.ok(entity);
            } catch (PersistenceException e) {
                return persistenceExceptionResponse(e, entity);
            }
        } else {
            return RestResponse.unprocessable(entity).withNotifications(form.getNotifications());
        }
    }

    default RestResponse<T> update(ID id, Map<String, Object> fields, String... fetch)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Optional<T> t = Retrieve(id, fetch);
        if (t.isPresent()) {
            Reflect.copy(t.get(), fields, Reflect.CopyOptions.create().mergeCollections());
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
        Optional<T> t = Retrieve(id);
        Delete(id);
        return t.isPresent() ? RestResponse.noContent() : RestResponse.notFound();
    }
}
