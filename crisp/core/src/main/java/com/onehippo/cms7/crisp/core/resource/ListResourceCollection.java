package com.onehippo.cms7.crisp.core.resource;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.onehippo.cms7.crisp.api.resource.Resource;
import com.onehippo.cms7.crisp.api.resource.ResourceCollection;

/**
 * {@link ResourceCollection} implementation based on an underlying List object.
 */
public class ListResourceCollection implements ResourceCollection {

    private final List<Resource> unmodifiableList;

    public ListResourceCollection(final List<Resource> list) {
        super();
        unmodifiableList = Collections.unmodifiableList(list);
    }

    @Override
    public Iterator<Resource> iterator() {
        return unmodifiableList.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public Collection<Resource> getCollection() {
        return unmodifiableList;
    }

    @Override
    public int size() {
        return unmodifiableList.size();
    }

    @Override
    public Resource get(int index) {
        return unmodifiableList.get(index);
    }

}