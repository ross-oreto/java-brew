package io.oreto.brew.web.rest;

import io.oreto.brew.data.Model;
import io.oreto.brew.data.Paged;
import io.oreto.brew.data.jpa.Store;

import java.util.Optional;

public interface Restful<ID, T extends Model<ID>> extends Store<ID, T> {
    default RestResponse<Paged<T>> list(String q, Integer page, Integer max, String sort) {
        return RestResponse.ok(List(q, page, max, sort));
    }
    default RestResponse<T> save(T entity) {
        return RestResponse.created(Create(entity));
    }
    default RestResponse<T> get(ID id) {
        Optional<T> t = Retrieve(id);
        return t.map(RestResponse::ok).orElseGet(RestResponse::notFound);
    }
    default RestResponse<T> update(T entity) {
        Optional<T> t = Retrieve(entity.getId());
        return t.map(RestResponse::ok).orElseGet(RestResponse::notFound);
    }
    default RestResponse<T> delete(ID id) {
        Optional<T> t = Retrieve(id);
        Delete(id);
        return t.isPresent() ? RestResponse.noContent() : RestResponse.notFound();
    }
}
