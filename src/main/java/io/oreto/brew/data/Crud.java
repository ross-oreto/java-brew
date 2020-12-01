package io.oreto.brew.data;

import java.util.Optional;

public interface Crud<ID, T extends Model<ID>> {
    T Create(T t, String... fetch);
    Optional<T> Retrieve(ID id, String... fetch);
    T Update(T t, String... fetch);
    T Delete(ID id);
}
