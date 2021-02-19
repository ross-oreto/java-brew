package io.oreto.brew.data;

import java.util.List;

public interface Paginate {
    int getPage();
    long getOffset();
    int getSize();
    Long getCount();
    void setCount(Long count);
    int getPages();
    List<Sortable> getSorting();
    default boolean isZeroBased() {
        return false;
    }
    boolean isCountEnabled();
    default Paginate withCount(long count) {
        setCount(count);
        return this;
    }
    Paginate disableCount();

    default Paginate nextPage() {
       return hasNext() ? Pager.of(this).withPage(getPage() + 1) : this;
    }

    default Paginate previousPage() {
        return hasPrevious() ? Pager.of(this).withPage(getPage() - 1) : this;
    }

    default boolean hasNext() {
        return getPage() < getPages();
    }

    default boolean hasPrevious() {
        return isZeroBased() ? getPage() > 0 : getPage() > 1;
    }

    default Paginate firstPage() {
        return Pager.of(this).withPage(isZeroBased() ? 0 : 1);
    }

    default Paginate lastPage() {
        return Pager.of(this).withPage(getPages());
    }
}
