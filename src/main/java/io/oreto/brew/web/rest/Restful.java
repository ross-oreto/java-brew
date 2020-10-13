package io.oreto.brew.web.rest;

import io.oreto.brew.data.Crud;
import io.oreto.brew.data.Model;
import io.oreto.brew.web.http.StatusCode;

import java.util.Optional;

public interface Restful<ID, T extends Model<ID>> extends Crud<ID, T> {
    default RestResponse list(String q, Integer page, Integer max, String sort) {
        return RestResponse.of(StatusCode.OK_CODE, List(q, page, max, sort));
    }
    default RestResponse save(T entity) {
        return RestResponse.of(StatusCode.CREATED_CODE, Create(entity));
    }
    default RestResponse get(ID id) {
        Optional<T> t = Retrieve(id);
        if (t.isPresent()) return RestResponse.of(StatusCode.OK_CODE, t);
        else throw new StatusCodeException(StatusCode.NOT_FOUND);
    }
    default RestResponse update(T entity) {
        Optional<T> t = Retrieve(entity.getId());
        if (t.isPresent()) return RestResponse.of(StatusCode.OK_CODE, Update(entity));
        else throw new StatusCodeException(StatusCode.NOT_FOUND);
    }
    default RestResponse delete(ID id) {
        Optional<T> t = Retrieve(id);
        if (t.isPresent()) return RestResponse.of(StatusCode.NO_CONTENT_CODE, Delete(id));
        else throw new StatusCodeException(StatusCode.NOT_FOUND);
    }
}
