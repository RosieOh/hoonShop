package com.hoonshop.catalog.domain;

import java.util.List;

public record ProductSearchResult(List<Product> items, int page, int size, long total) {

    public boolean hasNext() {
        return (long) page * size < total;
    }
}
