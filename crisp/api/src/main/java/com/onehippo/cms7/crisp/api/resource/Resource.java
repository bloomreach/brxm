/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.api.resource;

import java.io.Serializable;
import java.util.Iterator;

public interface Resource extends Serializable {

    String gerResourceType();

    boolean isResourceType(String resourceType);

    String getName();

    String getPath();

    ValueMap getMetadata();

    ValueMap getValueMap();

    Resource getParent();

    boolean isAnyChildContained();

    long getChildCount();

    Iterator<Resource> getChildIterator();

    Iterator<Resource> getChildIterator(long offset, long limit);

    Iterable<Resource> getChildren();

    Iterable<Resource> getChildren(long offset, long limit);

}
