package io.oreto.brew.data;

import java.util.List;

public class Paged<T> {
    public static <T> Paged<T> of(List<T> page, Paginate pager) {
        return new Paged<>(page, pager);
    }

    Paginate pager;
    List<T> page;

    public Paged(List<T> page, Paginate pager) {
        this.pager = pager;
        this.page = page;
    }

    public Paginate getPager() {
        return pager;
    }

    public List<T> getPage() {
        return page;
    }
}
