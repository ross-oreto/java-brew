package io.oreto.brew.data.jpa;

import io.oreto.brew.data.Crud;
import io.oreto.brew.data.Paged;

import java.util.Optional;

public interface Store<ID, T> extends Crud<ID, T> {
    Paged<T> List(String q, Integer page, Integer max, String... sort);
    Optional<T> ListSingle(String q, String... sort);
}
