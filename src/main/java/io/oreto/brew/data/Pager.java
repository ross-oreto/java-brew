package io.oreto.brew.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Pager implements Paginate {
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 10;

    public static Pager of(int page, int size, long count, int pages, List<String> sorting) {
        return new Pager(page, size, count, pages, sorting);
    }

    public static Pager of(int page, int size, long count, int pages) {
        return new Pager(page, size, count, pages);
    }

    public static Pager of(int page, int size, List<String> sorting) {
        return new Pager(page, size, sorting);
    }

    public static Pager of(int page, int size) {
        return new Pager(page, size);
    }

    public static Pager of() {
        return of(DEFAULT_PAGE, DEFAULT_SIZE);
    }

    public static Pager of(Paginate paginate) {
        return Pager.of(paginate.getPage(), paginate.getSize(), paginate.getCount(), paginate.getPages())
                .withSorting(paginate.getSorting());
    }

    public static Pager page(int page) {
        return of(page, DEFAULT_SIZE);
    }

    public static Pager size(int size) {
        return of(DEFAULT_PAGE, size);
    }

    private int page;
    private long offset;
    private int size;
    private Long count;
    private Integer pages;
    private List<Sortable> sorting;
    private boolean zeroBased;

    @JsonIgnore private boolean countEnabled = true;

    public Pager() {}

    protected Pager(int page, int size, long count, int pages) {
        this.size = size;
        this.count = count;
        this.pages = pages;
        setPage(page);
    }

    protected Pager(int page, int size, long count, int pages, List<String> sorting) {
        this(page, size, count, pages);
        this.sorting = Sort.of(sorting);
    }

    public Pager(int page, int size) {
        this.size = size;
        setPage(page);
        this.sorting = new ArrayList<>();
    }

    public Pager(int page, int size, List<String> sorting) {
        this(page, size);
        this.sorting = Sort.of(sorting);
    }

    public int getSize() {
        return size;
    }
    public Long getCount() {
        return count;
    }

    @Override
    public void setCount(Long count) {
        this.count = count;
    }

    public int getPages() {
        return pages;
    }
    public int getPage() {
        return page;
    }
    public long getOffset() { return offset; }
    public List<Sortable> getSorting() {
        return sorting;
    }

    public Pager withCount(long count) {
        this.count = count;
        this.pages = size == 0
                ? 0
                : Math.toIntExact((count / size) + (count % size > 0 ? 1 : 0));
        return this;
    }

    public Pager disableCount() {
        this.countEnabled = false;
        return this;
    }

    public boolean isCountEnabled() {
        return countEnabled;
    }

    public Pager withOffset(int offset) {
        this.offset = offset;
        offsetToPage();
        return this;
    }

    public Pager withSorting(String... sorting) {
        this.sorting = Sort.of(Arrays.stream(sorting).collect(Collectors.toList()));
        return this;
    }

    public Pager withSorting(Sortable... sorting) {
        this.sorting = Arrays.asList(sorting);
        return this;
    }

    public Pager withSorting(List<Sortable> sorting) {
        this.sorting = sorting;
        return this;
    }

    public Pager zeroBased() {
        if (!this.zeroBased) {
            this.zeroBased = true;
            if (page > 0)
                page--;
            pageToOffset();
        }
        return this;
    }

    public boolean isZeroBased() {
        return zeroBased;
    }

    public void setPage(int page) {
        this.page = page;
        pageToOffset();
    }

    public Pager withPage(int page) {
        setPage(page);
        return this;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setSorting(List<Sortable> sorting) {
        this.sorting = sorting;
    }

    protected void pageToOffset() {
        offset = Math.max(0, ((page + (zeroBased ? 1 : 0)) * size) - size);
    }

    protected void offsetToPage() {
        int p = size == 0 ? 0 : (int) (offset / size);
        page = p + (zeroBased ? 0 : 1);
    }
}
