package io.oreto.brew.web.page;

import io.oreto.brew.data.validation.Validator;

import java.util.List;

public interface Validatable {
    List<Validator<?>> validators();
}
