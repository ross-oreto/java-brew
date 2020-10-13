package io.oreto.brew.web.page;

import java.util.List;
import java.util.function.Function;

public interface Validatable<T> {
    List<Function<Form<T>, Notification>> validators();
}
