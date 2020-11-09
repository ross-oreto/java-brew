package io.oreto.brew.web.rest;

import io.oreto.brew.data.Model;
import io.oreto.brew.data.Paged;
import io.oreto.brew.data.jpa.Store;

import java.util.Optional;

public interface Restful<ID, T extends Model<ID>> extends Store<ID, T> {
    default RestResponse<Paged<T>> find(String q, Paged.Page page, String... fetch) {
        return RestResponse.ok(FindAll(q, page, fetch));
    }
    default RestResponse<T> findOne(String q, Paged.Page page, String... fetch) {
        Optional<T> t = FindOne(q, page, fetch);
        return t.map(RestResponse::ok).orElseGet(RestResponse::notFound);
    }
    default RestResponse<T> save(T entity, String... fetch) {
        return RestResponse.created(Create(entity, fetch));
    }
    default RestResponse<T> get(ID id, String... fetch) {
        Optional<T> t = Retrieve(id, fetch);
        return t.map(RestResponse::ok).orElseGet(RestResponse::notFound);
    }
    default RestResponse<T> update(T entity, String... fetch) {
        return RestResponse.ok(Update(entity, fetch));
    }
    default RestResponse<T> delete(ID id) {
        Optional<T> t = Retrieve(id);
        Delete(id);
        return t.isPresent() ? RestResponse.noContent() : RestResponse.notFound();
    }
}
