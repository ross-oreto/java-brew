package io.oreto.brew.data;

import io.oreto.brew.data.jpa.Fetch;

import java.util.Optional;

public interface Crud<ID, T extends Model<ID>> {
    T Create(T t, Fetch.Plan fetchPlan);
    default T Create(T t) {
        return Create(t, Fetch.Plan.none());
    }
    Optional<T> Retrieve(ID id, Fetch.Plan fetchPlan);
    default Optional<T> Retrieve(ID id) {
       return Retrieve(id, Fetch.Plan.none());
    }
    T Update(T t, Fetch.Plan fetchPlan);
    default T Update(T t) {
        return Update(t, Fetch.Plan.none());
    };
    T Delete(T t);
}
