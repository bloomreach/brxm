package org.onehippo.cms7.crisp.api.resource;

import java.util.Iterator;

public interface Resource {

    String gerResourceType();

    boolean isResourceType(String resourceType);

    String getName();

    String getPath();

    ValueMap getMetadata();

    ValueMap getValueMap();

    Resource getParent();

    boolean hasChildren();

    Iterator<Resource> listChildren();

    Iterable<Resource> getChildren();

}
