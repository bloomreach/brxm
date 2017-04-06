package org.onehippo.cms7.crisp.api.resource;

import java.util.Iterator;

public interface ResourceContainable {

    boolean isAnyChildContained();

    Iterator<Resource> getChildIterator();

    Iterable<Resource> getChildren();

}
