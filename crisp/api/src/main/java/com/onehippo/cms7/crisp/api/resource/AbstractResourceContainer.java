package com.onehippo.cms7.crisp.api.resource;

import java.util.Iterator;

public abstract class AbstractResourceContainer implements ResourceContainer {

    private static final long serialVersionUID = 1L;

    public AbstractResourceContainer() {
    }

    @Override
    public long getChildCount() {
        return -1;
    }

    @Override
    public Iterator<Resource> getChildIterator() {
        return getChildIterator(0L, -1L);
    }

    @Override
    public Iterable<Resource> getChildren() {
        return getChildren(0L, -1L);
    }

}
