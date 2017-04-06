package org.onehippo.cms7.crisp.api.resource;

import java.util.Iterator;

public interface ResourceContainer {

    boolean isAnyChildContained();

    Iterator<Resource> getChildIterator();

    Iterable<Resource> getChildren();

}
