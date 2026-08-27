package com.github.gcolin.platform;

import java.util.List;

public class PagedList<E> {

    private long total;
    private long start;
    private List<E> elements;

    public PagedList() {}

    public PagedList(List<E> elements, int start, long total) {
        this.elements = elements;
        this.start = start;
        this.total = total;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public List<E> getElements() {
        return elements;
    }

    public void setElements(List<E> elements) {
        this.elements = elements;
    }
}
