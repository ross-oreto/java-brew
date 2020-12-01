package io.oreto.brew.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Paged<T> {
    public static <T> Paged<T> of(List<T> list, Page page) {
        return new Paged<>(list, page);
    }

    Page page;
    List<T> list;

    public Paged(List<T> list, Page page) {
        this.page = page;
        this.list = list;
    }

    public Page getPage() {
        return page;
    }

    public List<T> getList() {
        return list;
    }

    public static class Page {
        public static Page of(int number, int size, int count, int pages, List<String> sorting) {
            return new Page(number, size, count, pages, sorting);
        }

        public static Page of(int number, int size, List<String> sorting) {
            return new Page(number, size, sorting);
        }

        public static Page of(int number, int size) {
            return new Page(number, size);
        }

        public static Page of() {
            return of(1, 20);
        }

        private int number;
        private int offset;
        private int size;
        private Long count;
        private Integer pages;
        private List<Sort> sorting;
        private boolean zeroBased;

        @JsonIgnore
        boolean countEnabled = true;

        protected Page(int number, int size, long count, int pages, List<String> sorting) {
            this.size = size;
            this.count = count;
            this.pages = pages;
            setNumber(number);
            this.sorting = Sort.of(sorting);
        }

        public Page() {}

        public Page(int number, int size) {
            this.size = size;
            setNumber(number);
            this.sorting = new ArrayList<>();
        }

        public Page(int number, int size, List<String> sorting) {
            this.size = size;
            setNumber(number);
            this.sorting = Sort.of(sorting);
        }

        public int getSize() {
            return size;
        }
        public Long getCount() {
            return count;
        }
        public int getPages() {
            return pages;
        }
        public int getNumber() {
            return number;
        }
        public int getOffset() { return offset; }
        public List<Sort> getSorting() {
            return sorting;
        }

        public Page withCount(long count) {
            this.count = count;
            this.pages = (count / size) + count % size > 0 ? 1 : 0;
            return this;
        }

        public Page disableCount() {
            this.countEnabled = false;
            return this;
        }

        public boolean isCountEnabled() {
            return countEnabled;
        }

        public Page withOffset(int offset) {
            this.offset = offset;
            offsetToPage();
            return this;
        }

        public Page withSorting(String... sorting) {
            this.sorting = Sort.of(Arrays.stream(sorting).collect(Collectors.toList()));
            return this;
        }

        public Page zeroBased() {
            if (!this.zeroBased) {
                this.zeroBased = true;
                if (number > 0)
                    number--;
                pageToOffset();
            }
            return this;
        }

        public void setNumber(int number) {
            this.number = number;
            pageToOffset();
        }

        public void setSize(int size) {
            this.size = size;
        }

        public void setSorting(List<Sort> sorting) {
            this.sorting = sorting;
        }

        protected void pageToOffset() {
            offset = Math.max(0, ((number + (zeroBased ? 1 : 0)) * size) - size);
        }

        protected void offsetToPage() {
            number = (offset / size) + (zeroBased ? 0 : 1);
        }
    }
}
