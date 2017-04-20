package com.onehippo.cms7.crisp.api.resource;

import java.io.Serializable;
import java.util.Iterator;

public interface ResourceContainer extends Serializable {

    boolean isAnyChildContained();

    long getChildCount();

    Iterator<Resource> getChildIterator();

    Iterator<Resource> getChildIterator(long offset, long limit);

    Iterable<Resource> getChildren();

    Iterable<Resource> getChildren(long offset, long limit);

}
