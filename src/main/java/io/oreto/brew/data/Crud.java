package io.oreto.brew.data;

import java.util.Optional;

public interface Crud<ID, T> {
    T Create(T t);
    Optional<T> Retrieve(ID id);
    T Update(T t);
    T Delete(ID id);
}
