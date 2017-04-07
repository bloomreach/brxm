package org.onehippo.cms7.crisp.api.resource;

import java.io.Serializable;
import java.util.Iterator;

public interface ResourceContainer extends Serializable {

    boolean isAnyChildContained();

    Iterator<Resource> getChildIterator();

    Iterable<Resource> getChildren();

}
