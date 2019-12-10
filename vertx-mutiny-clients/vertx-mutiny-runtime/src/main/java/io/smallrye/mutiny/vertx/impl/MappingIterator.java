package io.smallrye.mutiny.vertx.impl;

import java.util.Iterator;
import java.util.function.Function;

/**
 * @author Thomas Segismont
 */
public class MappingIterator<U, V> implements Iterator<V> {

    private final Iterator<U> iterator;
    private final Function<U, V> mapping;

    public MappingIterator(Iterator<U> iterator, Function<U, V> mapping) {
        this.iterator = iterator;
        this.mapping = mapping;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public V next() {
        return mapping.apply(iterator.next());
    }
}
