package io.oreto.brew.data.jpa;

import io.oreto.brew.data.Crud;
import io.oreto.brew.data.Paged;

import java.util.Optional;

public interface Store<ID, T> extends Crud<ID, T> {
    Paged<T> FindAll(String q, Paged.Page page, String... fetch);
    Optional<T> FindOne(String q, Paged.Page page, String... fetch);
}
