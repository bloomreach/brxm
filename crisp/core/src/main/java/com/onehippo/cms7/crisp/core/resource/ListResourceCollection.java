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

    /**
     * Unmodifiable view of the internal resource list.
     */
    private final List<Resource> unmodifiableList;

    /**
     * Constructs with a internal resource list.
     * @param list internal resource list
     */
    public ListResourceCollection(final List<Resource> list) {
        super();
        unmodifiableList = Collections.unmodifiableList(list);
    }

    /**
     * {@inheritDoc}
     */
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